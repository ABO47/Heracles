package earth.terrarium.heracles.client.screens.quests;

import com.mojang.datafixers.util.Pair;
import earth.terrarium.heracles.client.HeraclesClient;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.screens.AbstractQuestScreen;
import earth.terrarium.heracles.client.screens.mousemode.MouseMode;
import earth.terrarium.heracles.client.widgets.boxes.EnterableEditBox;
import earth.terrarium.heracles.client.widgets.modals.ConfirmModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.menus.quests.QuestsContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket;
import earth.terrarium.heracles.common.network.packets.rewards.ClaimRewardsPacket;
import earth.terrarium.heracles.common.utils.ModUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestsScreen extends AbstractQuestScreen<QuestsContent> {

    protected SelectQuestWidget selectQuestWidget;
    protected QuestsWidget questsWidget;
    protected GroupsList groupsList;

    protected ConfirmModal confirmModal;
    protected EnterableEditBox groupSearchBox;
    protected EnterableEditBox questSearchBox;
    protected String groupSearchFilter = "";

    public QuestsScreen(QuestsContent content) {
        super(content, CommonComponents.EMPTY);
        this.hasBackButton = false;
    }

    @Override
    protected void init() {
        super.init();
        if (this.content.canEdit()) {
            addRenderableWidget(new ImageButton(this.width - 24, 1, 11, 11, 33, 15, 11, HEADING, 256, 256, (button) ->
                NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(this.content.group(), this.getClass() == QuestsScreen.class))
            )).setTooltip(Tooltip.create(ConstantComponents.TOGGLE_EDIT));
        }

        if (!(this instanceof QuestsEditScreen)) {
            addRenderableWidget(new ImageButton(sideBarWidth + 3, 1, 11, 11, 44, 15, 11, HEADING, 256, 256, (button) -> {
                NetworkHandler.CHANNEL.sendToServer(new ClaimRewardsPacket());
            })).setTooltip(Tooltip.create(ConstantComponents.Rewards.CLAIM));
        }

        if (!(this instanceof QuestsEditScreen)) {
            this.groupSearchBox = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, 2, 1, Math.max(40, sideBarWidth - 4), 11, Component.nullToEmpty("")));
            this.groupSearchBox.setMaxLength(64);
            this.groupSearchBox.setResponder(this::updateGroupFilter);
            this.groupSearchBox.setEnter(this::updateGroupFilter);
        }

        List<Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus>> quests = new ArrayList<>();
        content.quests().forEach((id, status) ->
            ClientQuests.get(id)
                .filter(quest -> quest.value().display().groups().containsKey(content.group()))
                .ifPresent(quest -> quests.add(Pair.of(quest, status)))
        );

        questsWidget = addRenderableWidget(new QuestsWidget(
            (int) (this.width * 0.25f), // aka SIDE_BAR_PORTION
            15,
            (int) (this.width * 0.75f),
            (int) (this.width * 0.50f),
            this.height - 15,
            () -> actualChildren().contains(selectQuestWidget),
            this::getMouseMode,
            this::showSelectQuestWidget
        ));
        questsWidget.update(this.content, quests);
        HeraclesClient.lastGroup = content.group();

        this.groupsList = addRenderableWidget(new GroupsList(
            0,
            15,
            sideBarWidth,
            this.height - 15,
            entry -> {
                if (entry == null || this.content.group().equals(entry.name())) return;
                NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(entry.name(), this.getClass() != QuestsScreen.class));
            }
        ));
        this.groupsList.update(ClientQuests.groups(), this.content.group());

        if (!(this instanceof QuestsEditScreen)) {
            int searchX = sideBarWidth + 20;
            int rightReserve = this.content.canEdit() ? 54 : 20;
            int searchW = Math.max(60, this.width - searchX - rightReserve);
            this.questSearchBox = addRenderableWidget(new EnterableEditBox(Minecraft.getInstance().font, searchX, 1, searchW, 11, Component.nullToEmpty("")));
            this.questSearchBox.setMaxLength(64);
            this.questSearchBox.setResponder(this::applyQuestSearch);
            this.questSearchBox.setEnter(this::applyQuestSearchAndJump);
            if (!HeraclesClient.lastQuestSearch.isBlank()) {
                this.questSearchBox.setValue(HeraclesClient.lastQuestSearch);
                this.applyQuestSearch(HeraclesClient.lastQuestSearch);
            }
            if (HeraclesClient.focusQuestSearchOnOpen) {
                this.setFocused(this.questSearchBox);
                this.questSearchBox.setFocused(true);
                this.questSearchBox.moveCursorToEnd();
                HeraclesClient.focusQuestSearchOnOpen = false;
            }
        }

        this.confirmModal = addTemporary(new ConfirmModal(this.width, this.height));
    }

    protected void showSelectQuestWidget(ClientQuests.QuestEntry quest) {
        if (this.selectQuestWidget == null) return;
        if (quest == null) {
            removeWidget(this.selectQuestWidget);
            return;
        }
        if (!actualChildren().contains(this.selectQuestWidget)) {
            addRenderableWidget(this.selectQuestWidget);
        }
        this.selectQuestWidget.setEntry(quest);
    }

    protected void updateGroupFilter(String input) {
        this.groupSearchFilter = normalize(input);
        List<String> groups = new ArrayList<>();
        for (String g : ClientQuests.groups()) {
            if (matchesQuery(g, this.groupSearchFilter)) groups.add(g);
        }
        if (this.groupsList != null) {
            this.groupsList.update(groups, this.content.group());
        }
    }

    protected void applyQuestSearch(String input) {
        String query = normalize(input);
        HeraclesClient.lastQuestSearch = input == null ? "" : input;
        if (this.questsWidget != null) {
            this.questsWidget.setSearchFilter(input == null ? "" : input.trim());
        }
        if (query.isBlank()) return;

        List<ClientQuests.QuestEntry> matches = new ArrayList<>();
        for (ClientQuests.QuestEntry entry : ClientQuests.entries()) {
            if (matchesQuest(entry, query)) matches.add(entry);
        }
        if (matches.isEmpty()) return;
        matches.sort((a, b) -> scoreMatch(b, query) - scoreMatch(a, query));
        jumpToEntry(matches.get(0));
    }

    protected void applyQuestSearchAndJump(String input) {
        String query = normalize(input);
        applyQuestSearch(input);
        if (query.isBlank()) return;
        for (ClientQuests.QuestEntry entry : ClientQuests.entries()) {
            if (!matchesQuest(entry, query)) continue;
            jumpToEntry(entry);
            return;
        }
    }

    private void jumpToEntry(ClientQuests.QuestEntry entry) {
        if (entry.value().display().groups().containsKey(this.content.group())) {
            if (this.questsWidget != null) {
                this.questsWidget.focusQuest(entry.key());
            }
            return;
        }
        for (String g : entry.value().display().groups().keySet()) {
            if (!g.equals(this.content.group())) {
                HeraclesClient.focusQuestSearchOnOpen = true;
                NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(g, this instanceof QuestsEditScreen));
                return;
            }
        }
    }

    private static boolean isExactMatch(ClientQuests.QuestEntry entry, String query) {
        String normalizedId = normalize(entry.key());
        String normalizedTitle = normalize(entry.value().display().title().getString());
        return normalizedId.equals(query) || normalizedTitle.equals(query);
    }

    private int scoreMatch(ClientQuests.QuestEntry entry, String query) {
        int score = 0;
        String id = normalize(entry.key());
        String title = normalize(entry.value().display().title().getString());
        if (id.equals(query) || title.equals(query)) score += 500;
        if (id.startsWith(query) || title.startsWith(query)) score += 200;
        if (entry.value().display().groups().containsKey(this.content.group())) score += 100;
        return score;
    }

    private static boolean matchesQuest(ClientQuests.QuestEntry entry, String query) {
        StringBuilder all = new StringBuilder();
        all.append(entry.key()).append(' ');
        all.append(entry.value().display().title().getString()).append(' ');
        all.append(entry.value().display().subtitle().getString()).append(' ');
        if (entry.value().display().title().getContents() instanceof TranslatableContents t) {
            all.append(t.getKey()).append(' ');
        }
        for (String line : entry.value().display().description()) all.append(line).append(' ');
        return matchesQuery(all.toString(), query);
    }

    protected static String normalize(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    protected static boolean matchesQuery(String haystack, String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) return true;
        String normalizedHaystack = normalize(haystack);
        String[] terms = normalizedQuery.split(" ");
        for (String term : terms) {
            if (term.isBlank()) continue;
            if (!normalizedHaystack.contains(term)) return false;
        }
        return true;
    }

    protected MouseMode getMouseMode() {
        return MouseMode.DRAG_MOVE_OPEN;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!isTemporaryWidgetVisible() && questsWidget != null && questsWidget.isMouseOver(mouseX, mouseY)) {
            questsWidget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
    }

    public GroupsList getGroupsList() {
        return groupsList;
    }

    public ConfirmModal confirmModal() {
        return confirmModal;
    }

    public String getGroup() {
        return content.group();
    }

    public void updateProgress(Map<String, QuestProgress> quests) {
        List<Pair<ClientQuests.QuestEntry, ModUtils.QuestStatus>> statues = new ArrayList<>();
        for (var entry : this.content.quests().entrySet()) {
            ClientQuests.QuestEntry quest = ClientQuests.get(entry.getKey())
                .filter(q -> q.value().display().groups().containsKey(content.group()))
                .orElse(null);
            if (quest == null) continue;
            if (entry.getValue() == ModUtils.QuestStatus.COMPLETED) {
                QuestProgress progress = quests.get(entry.getKey());
                if (progress != null && progress.isClaimed(quest.value())) {
                    entry.setValue(ModUtils.QuestStatus.COMPLETED_CLAIMED);
                }
            }
            statues.add(Pair.of(quest, entry.getValue()));
        }
        if (questsWidget == null) return;
        questsWidget.update(this.content, statues);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
