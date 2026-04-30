package earth.terrarium.heracles.client;

import com.mojang.blaze3d.platform.InputConstants;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.data.ThemeHandler;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.handlers.QuestTutorial;
import earth.terrarium.heracles.client.screens.QuestTutorialScreen;
import earth.terrarium.heracles.client.toasts.QuestClaimedToast;
import earth.terrarium.heracles.client.toasts.QuestCompletedToast;
import earth.terrarium.heracles.client.toasts.QuestUnlockedToast;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.groups.OpenGroupPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.BiConsumer;

public class HeraclesClient {

    public static final KeyMapping OPEN_QUESTS = new KeyMapping(
        "key.heracles.open_quests",
        InputConstants.KEY_U,
        "key.categories.odyssey"
    );
    public static final KeyMapping EDITOR_SELECT_MOVE = new KeyMapping(
        "key.heracles.editor.select_move",
        InputConstants.KEY_V,
        "key.categories.odyssey"
    );
    public static final KeyMapping EDITOR_DRAG = new KeyMapping(
        "key.heracles.editor.drag",
        InputConstants.KEY_H,
        "key.categories.odyssey"
    );
    public static final KeyMapping EDITOR_ADD = new KeyMapping(
        "key.heracles.editor.add",
        InputConstants.KEY_U,
        "key.categories.odyssey"
    );
    public static final KeyMapping EDITOR_LINK = new KeyMapping(
        "key.heracles.editor.link",
        InputConstants.KEY_L,
        "key.categories.odyssey"
    );
    public static final KeyMapping EDITOR_GRID_TOGGLE = new KeyMapping(
        "key.heracles.editor.grid_toggle",
        InputConstants.KEY_G,
        "key.categories.odyssey"
    );
    public static final KeyMapping EDITOR_GRID_LOCK = new KeyMapping(
        "key.heracles.editor.grid_lock",
        InputConstants.KEY_K,
        "key.categories.odyssey"
    );

    public static String lastGroup = "";
    public static boolean keepEditMode = false;
    public static String lastQuestSearch = "";
    public static boolean focusQuestSearchOnOpen = false;

    public static void init() {
        Heracles.setRegistryAccess(() -> {
            var connection = Minecraft.getInstance().getConnection();
            if (connection == null) return RegistryAccess.EMPTY;
            return connection.registryAccess();
        });
    }

    public static void clientTick() {
        if (OPEN_QUESTS.consumeClick()) {
            openQuestScreen();
        }
        QuestTutorial.tick();
    }

    public static void openQuestScreen() {
        if (!ClientQuests.groups().contains(lastGroup)) {
            lastGroup = "";
        }
        if (DisplayConfig.showTutorial) {
            if (!QuestTutorial.tutorialText().isBlank()) {
                Minecraft.getInstance().setScreen(new QuestTutorialScreen());
            } else {
                DisplayConfig.showTutorial = false;
                DisplayConfig.save();
                NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(lastGroup, keepEditMode));
            }
        } else {
            NetworkHandler.CHANNEL.sendToServer(new OpenGroupPacket(lastGroup, keepEditMode));
        }
    }

    public static void displayItemsRewardedToast(String id, List<Item> items) {
        QuestClaimedToast.addOrUpdate(Minecraft.getInstance().getToasts(), id, items);
    }

    public static void displayQuestCompleteToast(String id) {
        QuestCompletedToast.add(Minecraft.getInstance().getToasts(), id);
    }

    public static void displayQuestUnlockedToast(String id) {
        QuestUnlockedToast.add(Minecraft.getInstance().getToasts(), id);
    }

    public static void initReloadListeners(BiConsumer<ResourceLocation, PreparableReloadListener> init) {
        init.accept(new ResourceLocation(Heracles.MOD_ID, "theme"), ThemeHandler.INSTANCE);
    }
}
