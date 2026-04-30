package earth.terrarium.heracles.client.handlers;

import earth.terrarium.heracles.client.screens.quests.QuestsWidget;
import earth.terrarium.heracles.client.utils.MouseClick;

import java.util.ArrayList;
import java.util.List;

public class GhostBlueprintManager {
    private static List<ClientQuestClipboard.CopiedQuest> active = null;

    public static synchronized void setActiveBlueprint(List<ClientQuestClipboard.CopiedQuest> blueprint) {
        if (blueprint == null) {
            active = null;
            return;
        }
        active = new ArrayList<>();
        for (ClientQuestClipboard.CopiedQuest cq : blueprint) {
            active.add(new ClientQuestClipboard.CopiedQuest(cq.originalId, ClientQuestClipboard.deepCopy(cq.quest), cq.sourceGroup, cq.sourcePos));
        }
    }

    public static synchronized boolean hasActive() {
        return active != null && !active.isEmpty();
    }

    public static synchronized List<ClientQuestClipboard.CopiedQuest> getActive() {
        return active == null ? List.of() : new ArrayList<>(active);
    }

    public static synchronized void clear() {
        active = null;
    }

    public static synchronized java.util.List<String> placeAt(QuestsWidget widget, MouseClick click) {
        if (!hasActive()) return List.of();
        java.util.List<String> ids = ClientQuestClipboard.pasteBlueprintAt(widget, click, active);
        clear();
        QuestsWidget.refreshOpenScreens();
        return ids;
    }
}
