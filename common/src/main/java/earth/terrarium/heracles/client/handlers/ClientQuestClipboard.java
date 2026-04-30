package earth.terrarium.heracles.client.handlers;

import earth.terrarium.heracles.api.quests.GroupDisplay;
import earth.terrarium.heracles.api.quests.Quest;
import earth.terrarium.heracles.api.quests.QuestDisplay;
import earth.terrarium.heracles.api.quests.QuestSettings;
import earth.terrarium.heracles.api.rewards.QuestReward;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.screens.quests.QuestsWidget;
import earth.terrarium.heracles.client.utils.MouseClick;
import earth.terrarium.heracles.client.utils.QuestIdPolicy;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.RegistryOps;
import org.joml.Vector2i;

import java.util.*;

public class ClientQuestClipboard {
    private static final List<CopiedQuest> COPIED = new ArrayList<>();
    private static final List<List<CopiedQuest>> HISTORY = new ArrayList<>();
    private static final int MAX_HISTORY = 20;

    public static void copySingle(String id, ClientQuests.QuestEntry entry, String group) {
        COPIED.clear();
        Vector2i pos = entry.value().display().position(group);
        COPIED.add(new CopiedQuest(id, deepCopy(entry.value()), group, new Vector2i(pos.x(), pos.y())));
        pushHistory();
    }

    public static void copyMultiple(Collection<ClientQuests.QuestEntry> entries, String group) {
        COPIED.clear();
        for (ClientQuests.QuestEntry entry : entries) {
            Vector2i pos = entry.value().display().position(group);
            COPIED.add(new CopiedQuest(entry.key(), deepCopy(entry.value()), group, new Vector2i(pos.x(), pos.y())));
        }
        pushHistory();
    }

    public static boolean hasCopied() {
        return !COPIED.isEmpty();
    }

    public static void clear() {
        COPIED.clear();
    }

    public static Quest deepCopy(Quest original) {
        try {
            var encoded = Quest.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, Heracles.getRegistryAccess()), original).result().orElse(null);
            if (encoded != null) {
                var decoded = Quest.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, Heracles.getRegistryAccess()), encoded).result().orElse(null);
                if (decoded != null) return decoded;
            }
        } catch (Exception ignored) {
        }
        Map<String, GroupDisplay> groups = new HashMap<>();
        for (Map.Entry<String, GroupDisplay> e : original.display().groups().entrySet()) {
            GroupDisplay g = e.getValue();
            groups.put(e.getKey(), new GroupDisplay(e.getKey(), new Vector2i(g.position()), g.nodeScale(), "", ""));
        }
        QuestDisplay display = new QuestDisplay(
            original.display().icon(),
            original.display().iconBackground(),
            original.display().title().copy(),
            original.display().subtitle().copy(),
            new ArrayList<>(original.display().description()),
            groups
        );
        QuestSettings settings = new QuestSettings(
            original.settings().individualProgress(),
            original.settings().hiddenUntil(),
            original.settings().unlockNotification(),
            original.settings().showDependencyArrow(),
            original.settings().repeatable(),
            original.settings().autoClaimRewards()
        );
        Set<String> deps = new HashSet<>(original.dependencies());
        Map<String, QuestTask<?, ?, ?>> tasksCopy = new HashMap<>();
        Map<String, QuestReward<?>> rewardsCopy = new HashMap<>();
        tasksCopy.putAll(original.tasks());
        rewardsCopy.putAll(original.rewards());
        return new Quest(display, settings, deps, tasksCopy, rewardsCopy);
    }

    private static void pushHistory() {
        List<CopiedQuest> snapshot = new ArrayList<>();
        for (CopiedQuest cq : COPIED) {
            snapshot.add(new CopiedQuest(cq.originalId, deepCopy(cq.quest), cq.sourceGroup, new Vector2i(cq.sourcePos.x(), cq.sourcePos.y())));
        }
        HISTORY.add(0, snapshot);
        while (HISTORY.size() > MAX_HISTORY) HISTORY.remove(HISTORY.size() - 1);
    }

    public static List<String> pasteAt(QuestsWidget widget, MouseClick click) {
        return pasteCopiedAt(widget, click, COPIED, false);
    }

    private static List<String> pasteCopiedAt(QuestsWidget widget, MouseClick click, List<CopiedQuest> copied, boolean centerOnCursor) {
        if (copied == null || copied.isEmpty()) return List.of();
        List<String> returnIds = new ArrayList<>();
        MouseClick local = widget.getLocal(click);

        double anchorX;
        double anchorY;
        if (centerOnCursor) {
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            for (CopiedQuest cq : copied) {
                TexturePlacements.Info info = TexturePlacements.getOrDefault(cq.quest.display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
                GroupDisplay sourceDisplay = cq.quest.display().groups().get(cq.sourceGroup);
                if (sourceDisplay == null && !cq.quest.display().groups().isEmpty()) {
                    sourceDisplay = cq.quest.display().groups().values().iterator().next();
                }
                float nodeScale = sourceDisplay == null ? 1.0f : sourceDisplay.nodeScale();
                if (Float.isNaN(nodeScale) || Float.isInfinite(nodeScale)) nodeScale = 1.0f;
                nodeScale = Math.max(0.5f, nodeScale);
                int offX = Math.round(info.xOffset() * nodeScale);
                int offY = Math.round(info.yOffset() * nodeScale);
                int width = Math.max(1, Math.round(info.width() * nodeScale));
                int height = Math.max(1, Math.round(info.height() * nodeScale));
                int left = cq.sourcePos.x() + offX;
                int top = cq.sourcePos.y() + offY;
                minX = Math.min(minX, left);
                minY = Math.min(minY, top);
                maxX = Math.max(maxX, left + width);
                maxY = Math.max(maxY, top + height);
            }
            anchorX = (minX == Integer.MAX_VALUE) ? 0.0 : (minX + maxX) / 2.0;
            anchorY = (minY == Integer.MAX_VALUE) ? 0.0 : (minY + maxY) / 2.0;
        } else {
            anchorX = Double.POSITIVE_INFINITY;
            anchorY = Double.POSITIVE_INFINITY;
            for (CopiedQuest cq : copied) {
                anchorX = Math.min(anchorX, cq.sourcePos.x());
                anchorY = Math.min(anchorY, cq.sourcePos.y());
            }
            if (anchorX == Double.POSITIVE_INFINITY) anchorX = 0.0;
            if (anchorY == Double.POSITIVE_INFINITY) anchorY = 0.0;
        }
        int dx = (int) Math.round(local.x() - anchorX);
        int dy = (int) Math.round(local.y() - anchorY);

        Map<String, String> idMap = new HashMap<>();
        Set<String> originalIds = new HashSet<>();
        for (CopiedQuest cq : copied) originalIds.add(cq.originalId);

        Set<String> reservedIds = new HashSet<>();
        for (CopiedQuest cq : copied) {
            String id = generatePastedQuestId(reservedIds, widget.group(), cq.originalId);
            reservedIds.add(id);
            idMap.put(cq.originalId, id);
        }
        
        for (CopiedQuest cq : copied) {
            Quest newQuest = deepCopy(cq.quest);
            Map<String, GroupDisplay> groups = newQuest.display().groups();
            int targetX = Math.round(cq.sourcePos.x() + dx);
            int targetY = Math.round(cq.sourcePos.y() + dy);
            if (DisplayConfig.isGridLocked(widget.group())) {
                int gs = Math.max(1, DisplayConfig.getGridSize(widget.group()));
                targetX = Math.round((float) targetX / (float) gs) * gs;
                targetY = Math.round((float) targetY / (float) gs) * gs;
            }

            // Pasted quests belong to the target group only. This keeps IDs/folders deterministic
            // and prevents copies from inheriting stale source-group metadata.
            GroupDisplay template = groupTemplate(widget.group());
            float copiedScale = copiedNodeScale(cq, widget.group());
            groups.clear();
            groups.put(widget.group(), new GroupDisplay(widget.group(), new Vector2i(targetX, targetY), copiedScale, "", ""));
            Set<String> newDeps = new HashSet<>();
            for (String dep : cq.quest.dependencies()) {
                if (!originalIds.contains(dep)) continue;
                newDeps.add(idMap.get(dep));
            }
            newQuest.dependencies().clear();
            newQuest.dependencies().addAll(newDeps);
            String newId = idMap.get(cq.originalId);
            widget.addQuest(ClientQuestNetworking.add(newId, newQuest));
            returnIds.add(newId);
        }
        if (!returnIds.isEmpty()) {
            widget.setMultiSelectedIds(returnIds);
        }
        return returnIds;
    }

    public static List<String> pasteRecent(QuestsWidget widget, MouseClick click) {
        if (HISTORY.isEmpty()) return List.of();
        List<CopiedQuest> recent = HISTORY.get(0);
        COPIED.clear();
        for (CopiedQuest cq : recent) {
            COPIED.add(new CopiedQuest(cq.originalId, deepCopy(cq.quest), cq.sourceGroup, new Vector2i(cq.sourcePos.x(), cq.sourcePos.y())));
        }
        return pasteAt(widget, click);
    }

    public static List<String> pasteBlueprintAt(QuestsWidget widget, MouseClick click, List<CopiedQuest> blueprint) {
        if (blueprint == null || blueprint.isEmpty()) return List.of();
        List<CopiedQuest> copied = new ArrayList<>();
        for (CopiedQuest cq : blueprint) {
            copied.add(new CopiedQuest(cq.originalId, deepCopy(cq.quest), cq.sourceGroup, new Vector2i(cq.sourcePos.x(), cq.sourcePos.y())));
        }
        return pasteCopiedAt(widget, click, copied, true);
    }

    private static float copiedNodeScale(CopiedQuest copiedQuest, String targetGroup) {
        GroupDisplay sourceDisplay = copiedQuest.quest.display().groups().get(copiedQuest.sourceGroup);
        if (sourceDisplay == null) sourceDisplay = copiedQuest.quest.display().groups().get(targetGroup);
        if (sourceDisplay == null && !copiedQuest.quest.display().groups().isEmpty()) {
            sourceDisplay = copiedQuest.quest.display().groups().values().iterator().next();
        }
        float nodeScale = sourceDisplay == null ? 1.0f : sourceDisplay.nodeScale();
        if (Float.isNaN(nodeScale) || Float.isInfinite(nodeScale)) return 1.0f;
        return Math.max(0.5f, nodeScale);
    }

    public static class CopiedQuest {
        public final String originalId;
        public final Quest quest;
        public final String sourceGroup;
        public final Vector2i sourcePos;

        public CopiedQuest(String originalId, Quest quest, String sourceGroup, Vector2i sourcePos) {
            this.originalId = originalId;
            this.quest = quest;
            this.sourceGroup = sourceGroup;
            this.sourcePos = sourcePos;
        }
    }

    private static String generatePastedQuestId(Set<String> reservedIds, String group, String hint) {
        return QuestIdPolicy.nextAvailable(group, group, reservedIds);
    }

    private static GroupDisplay groupTemplate(String group) {
        for (ClientQuests.QuestEntry entry : ClientQuests.byGroup(group)) {
            GroupDisplay display = entry.value().display().groups().get(group);
            if (display != null) return display.withNodeScale(1.0f);
        }
        return GroupDisplay.create(group);
    }
}
