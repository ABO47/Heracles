package earth.terrarium.heracles.api.validation;

import earth.terrarium.heracles.api.quests.Quest;
import net.minecraft.server.level.ServerPlayer;

public interface QuestValidationHandler {
    enum Action {
        ADD, UPDATE, REMOVE, LINK, UNLINK
    }

    /**
     * Validate a quest change on the server. Return true to allow, false to deny.
     */
    boolean validate(ServerPlayer player, Action action, String questId, Quest before, Quest after);
}
