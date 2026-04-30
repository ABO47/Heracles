package earth.terrarium.heracles.client.handlers;

import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.client.screens.quests.QuestsEditScreen;
import earth.terrarium.heracles.client.screens.quests.QuestsWidget;
import earth.terrarium.heracles.client.screens.quests.SelectQuestWidget;
import earth.terrarium.heracles.client.utils.ClientUtils;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.client.widgets.modals.TextInputModal;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.joml.Vector2i;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.function.Consumer;

public class QuestClipboard {

    public static final QuestClipboard INSTANCE = new QuestClipboard();

    private Quest quest;
    private String key;
    private String sourceGroup;

    public boolean action(int keyCode, SelectQuestWidget widget) {
        if (Screen.isCut(keyCode) && widget.entry() != null) {
            action(widget.widget(), widget.entry(), QuestAction.CUT);
            return true;
        } else if (Screen.isCopy(keyCode) && widget.entry() != null) {
            action(widget.widget(), widget.entry(), QuestAction.COPY);
            return true;
        } else if (Screen.isPaste(keyCode)) {
            if (ClientUtils.screen() instanceof QuestsEditScreen screen) {
                screen.questModal().setData(ClientUtils.getMousePos());
                paste(widget.widget(), screen.questModal(), entry -> widget.widget().addQuest(entry));
                return true;
            }
        } else if (isSpecialPaste(keyCode)) {
            if (Minecraft.getInstance().screen instanceof QuestsEditScreen screen) {
                MouseClick pos = widget.widget().getLocal(ClientUtils.getMousePos());
                String group = screen.getGroup();
                ClientQuests.get(this.key).ifPresent(entry -> {
                    if (entry.value().display().groups().containsKey(group)) return;
                    ClientQuests.updateQuest(entry, quest -> {
                        GroupDisplay template = groupTemplate(group);
                        float copiedScale = copiedNodeScale(quest, this.sourceGroup, group);
                        quest.display().groups().put(group, new GroupDisplay(group, new Vector2i((int) pos.x() - 12, (int) pos.y() - 12), copiedScale, "", ""));
                        return NetworkQuestData.builder().groups(quest.display().groups());
                    });
                    widget.widget().addQuest(entry);
                });
                return true;
            }
        }
        return false;
    }

    private static boolean isSpecialPaste(int keycode) {
        return keycode == 86 && Screen.hasControlDown() && Screen.hasShiftDown() && !Screen.hasAltDown();
    }

    public void action(QuestsWidget widget, ClientQuests.QuestEntry entry, QuestAction action) {
        this.quest = null;
        this.key = null;
        this.sourceGroup = null;
        switch (action) {
            case CUT -> {
                this.quest = ClientQuestClipboard.deepCopy(entry.value());
                this.key = entry.key();
                this.sourceGroup = widget.group();
                if (this.quest.display().groups().size() == 1) {
                    ClientQuestNetworking.remove(entry.key());
                } else {
                    ClientQuests.updateQuest(entry, quest -> {
                        quest.display().groups().remove(widget.group());
                        return NetworkQuestData.builder().groups(quest.display().groups());
                    });
                }
                widget.removeQuest(entry);
            }
            case COPY -> {
                this.quest = ClientQuestClipboard.deepCopy(entry.value());
                this.key = entry.key();
                this.sourceGroup = widget.group();
            }
            default -> throw new IllegalStateException("Unexpected value: " + action);
        }
    }

    public void paste(QuestsWidget widget, TextInputModal<MouseClick> modal, Consumer<ClientQuests.QuestEntry> callback) {
        if (this.quest != null) {
            if (this.key != null) {
                if (ClientQuests.get(this.key).isPresent()) {
                    this.key = null;
                }
            }
            if (this.key != null) {
                create(this.key, widget, modal.getData(), callback);
                this.key = null;
                this.quest = null;
                this.sourceGroup = null;
            } else {
                modal.setVisible(true);
                modal.setCallback((click, id) -> {
                    create(id, widget, click, callback);
                    this.quest = null;
                    this.sourceGroup = null;
                });
            }
        }
    }

    private void create(String id, QuestsWidget widget, MouseClick mouse, Consumer<ClientQuests.QuestEntry> callback) {
        if (Minecraft.getInstance().screen instanceof QuestsEditScreen screen) {
            MouseClick local = widget.getLocal(mouse);
            String group = screen.getGroup();
            Quest newQuest = ClientQuestClipboard.deepCopy(quest);
            Map<String, GroupDisplay> copiedGroups = new HashMap<>();
            for (Map.Entry<String, GroupDisplay> e : newQuest.display().groups().entrySet()) {
                GroupDisplay groupDisplay = e.getValue();
                Vector2i pos = groupDisplay.position();
                copiedGroups.put(e.getKey(), new GroupDisplay(e.getKey(), new Vector2i(pos.x(), pos.y()), groupDisplay.nodeScale(), "", ""));
            }
            GroupDisplay template = groupTemplate(group);
            float copiedScale = copiedNodeScale(newQuest, this.sourceGroup, group);
            copiedGroups.put(group, new GroupDisplay(group, new Vector2i((int) local.x() - 12, (int) local.y() - 12), copiedScale, "", ""));
            QuestDisplay display = new QuestDisplay(
                
                newQuest.display().icon(),
                newQuest.display().iconBackground(),
                newQuest.display().title().copy(),
                newQuest.display().subtitle().copy(),
                new ArrayList<>(newQuest.display().description()),
                copiedGroups
            );
            Quest replacement = new Quest(
                display,
                newQuest.settings(),
                new HashSet<>(newQuest.dependencies()),
                new HashMap<>(newQuest.tasks()),
                new HashMap<>(newQuest.rewards())
            );
            callback.accept(ClientQuestNetworking.add(id, replacement));
        }
    }

    public enum QuestAction {
        COPY,
        CUT
    }

    private GroupDisplay groupTemplate(String group) {
        for (ClientQuests.QuestEntry entry : ClientQuests.byGroup(group)) {
            GroupDisplay display = entry.value().display().groups().get(group);
            if (display != null) return display.withNodeScale(1.0f);
        }
        return GroupDisplay.create(group);
    }

    private static float copiedNodeScale(Quest quest, String sourceGroup, String targetGroup) {
        GroupDisplay sourceDisplay = sourceGroup == null ? null : quest.display().groups().get(sourceGroup);
        if (sourceDisplay == null) sourceDisplay = quest.display().groups().get(targetGroup);
        if (sourceDisplay == null && !quest.display().groups().isEmpty()) {
            sourceDisplay = quest.display().groups().values().iterator().next();
        }
        float nodeScale = sourceDisplay == null ? 1.0f : sourceDisplay.nodeScale();
        if (Float.isNaN(nodeScale) || Float.isInfinite(nodeScale)) return 1.0f;
        return Math.max(0.5f, nodeScale);
    }

}
