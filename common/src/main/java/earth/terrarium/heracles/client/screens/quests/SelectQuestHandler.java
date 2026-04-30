package earth.terrarium.heracles.client.screens.quests;

import com.mojang.blaze3d.platform.InputConstants;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.mousemode.MouseMode;
import earth.terrarium.heracles.client.utils.TexturePlacements;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.quests.OpenQuestPacket;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;
import org.joml.Vector2i;

import java.util.HashSet;
import java.util.Set;

import java.util.function.Consumer;

public class SelectQuestHandler {

    private final String group;
    private final Consumer<ClientQuests.QuestEntry> onSelection;

    private long lastClickTime = 0;
    private QuestWidget selectedQuest;
    private boolean pendingLink = false;

    private Vector2i start = null;
    private Vector2i startOffset = null;

    public SelectQuestHandler(String group, Consumer<ClientQuests.QuestEntry> onSelection) {
        this.group = group;
        this.onSelection = onSelection;
    }

    public void clickQuest(MouseMode mode, int mouseX, int mouseY, QuestWidget quest) {
        if (selectedQuest == quest) {
            if (Screen.hasShiftDown()) {
                release();
                return;
            } else if (System.currentTimeMillis() - lastClickTime < 500) {
                selectedQuest = null;
                NetworkHandler.CHANNEL.sendToServer(new OpenQuestPacket(
                    this.group, quest.id(), Minecraft.getInstance().screen instanceof QuestsEditScreen
                ));
            }
        } else if ((mode == MouseMode.SELECT_LINK || pendingLink) && selectedQuest != null) {
            ClientQuests.updateQuest(quest.entry(), q -> {
                
                Set<String> newDeps = new HashSet<>(q.dependencies());
                if (Screen.hasShiftDown()) {
                    boolean changed = newDeps.remove(selectedQuest.id());
                    if (changed) {
                        selectedQuest.entry().dependents().remove(quest.entry());
                        quest.entry().dependencies().remove(selectedQuest.entry());
                    }
                } else {
                    if (!quest.entry().dependents().contains(selectedQuest.entry())) {
                        boolean added = newDeps.add(selectedQuest.id());
                        if (added) {
                            selectedQuest.entry().dependents().add(quest.entry());
                            quest.entry().dependencies().add(selectedQuest.entry());
                        }
                    }
                }
                pendingLink = false;
                return NetworkQuestData.builder().dependencies(newDeps);
            });
            return;
        }
        onSelection.accept(quest.entry());
        selectedQuest = quest;
        lastClickTime = System.currentTimeMillis();
        start = new Vector2i(mouseX, mouseY);
        startOffset = new Vector2i(quest.x(), quest.y());
    }

    public void release() {
        selectedQuest = null;
        start = null;
        startOffset = null;
        pendingLink = false;
        onSelection.accept(null);
    }

    public void startPendingLink(QuestWidget source) {
        this.selectedQuest = source;
        this.pendingLink = true;
        if (source != null) {
            this.onSelection.accept(source.entry());
            this.lastClickTime = System.currentTimeMillis();
        }
    }

    public boolean isPendingLink() {
        return this.pendingLink;
    }

    public void onDrag(int mouseX, int mouseY) {
        if (selectedQuest != null && start != null && startOffset != null) {
            int newX = mouseX - start.x() + startOffset.x();
            int newY = mouseY - start.y() + startOffset.y();
            if (DisplayConfig.isGridLocked(this.group)) {
                int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
                int insetX = visualInsetX(selectedQuest);
                int insetY = visualInsetY(selectedQuest);
                var info = TexturePlacements.getOrDefault(selectedQuest.quest().display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
                int offX = Math.round(info.xOffset() * selectedQuest.nodeScale());
                int offY = Math.round(info.yOffset() * selectedQuest.nodeScale());
                int snappedTopLeftX = Math.round((newX + offX + insetX) / (float) gs) * gs;
                int snappedTopLeftY = Math.round((newY + offY + insetY) / (float) gs) * gs;
                newX = snappedTopLeftX - offX - insetX;
                newY = snappedTopLeftY - offY - insetY;
            }
            Vector2i clamped = clampToCanvasLimit(selectedQuest, newX, newY);
            newX = clamped.x();
            newY = clamped.y();
            final int finalX = newX;
            final int finalY = newY;
            ClientQuests.updateQuest(selectedQuest.entry(), quest ->
                NetworkQuestData.builder().group(quest, selectedQuest.group(), pos -> {
                    pos.x = finalX;
                    pos.y = finalY;
                    return pos;
                }),
                false
            );
        }
    }

    public boolean onKeyPress(int key) {
        int x = 0;
        int y = 0;
        switch (key) {
            case InputConstants.KEY_UP -> y = -1;
            case InputConstants.KEY_DOWN -> y = 1;
            case InputConstants.KEY_LEFT -> x = -1;
            case InputConstants.KEY_RIGHT -> x = 1;
        }

        if (Screen.hasShiftDown()) {
            x *= 10;
            y *= 10;
        } else if (Screen.hasControlDown()) {
            x *= 5;
            y *= 5;
        }

        if (x == 0 && y == 0) return false;
        if (selectedQuest == null) return false;

        int newX = selectedQuest.x() + x;
        int newY = selectedQuest.y() + y;
        if (DisplayConfig.isGridLocked(this.group)) {
            int gs = Math.max(1, DisplayConfig.getGridSize(this.group));
            int insetX = visualInsetX(selectedQuest);
            int insetY = visualInsetY(selectedQuest);
            var info = TexturePlacements.getOrDefault(selectedQuest.quest().display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
            int offX = Math.round(info.xOffset() * selectedQuest.nodeScale());
            int offY = Math.round(info.yOffset() * selectedQuest.nodeScale());
            int snappedTopLeftX = Math.round((newX + offX + insetX) / (float) gs) * gs;
            int snappedTopLeftY = Math.round((newY + offY + insetY) / (float) gs) * gs;
            newX = snappedTopLeftX - offX - insetX;
            newY = snappedTopLeftY - offY - insetY;
        }
        Vector2i clamped = clampToCanvasLimit(selectedQuest, newX, newY);
        newX = clamped.x();
        newY = clamped.y();
        final int finalX = newX;
        final int finalY = newY;

        ClientQuests.updateQuest(selectedQuest.entry(), quest ->
                NetworkQuestData.builder().group(quest, selectedQuest.group(), pos -> {
                    pos.x = finalX;
                    pos.y = finalY;
                    return pos;
                }),
            false
        );
        return true;
    }

    public QuestWidget selectedQuest() {
        return selectedQuest;
    }

    private int visualInsetX(QuestWidget quest) {
        var info = TexturePlacements.getOrDefault(quest.quest().display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
        if (info.xOffset() == 0 && info.yOffset() == 0 && info.width() == 24 && info.height() == 24) {
            return Math.max(0, Math.round(quest.nodeScale()) - 1);
        }
        return 0;
    }

    private int visualInsetY(QuestWidget quest) {
        var info = TexturePlacements.getOrDefault(quest.quest().display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
        if (info.xOffset() == 0 && info.yOffset() == 0 && info.width() == 24 && info.height() == 24) {
            return Math.max(0, Math.round(quest.nodeScale()) - 1);
        }
        return 0;
    }

    private Vector2i clampToCanvasLimit(QuestWidget quest, int x, int y) {
        if (quest == null || !DisplayConfig.hasCanvasLimit(this.group)) {
            return new Vector2i(x, y);
        }
        int canvasWidth = Math.max(1, DisplayConfig.getCanvasLimitWidth(this.group));
        int canvasHeight = Math.max(1, DisplayConfig.getCanvasLimitHeight(this.group));
        int canvasMinX = -(canvasWidth / 2);
        int canvasMinY = -(canvasHeight / 2);
        int canvasMaxX = canvasMinX + canvasWidth;
        int canvasMaxY = canvasMinY + canvasHeight;

        var info = TexturePlacements.getOrDefault(quest.quest().display().iconBackground(), TexturePlacements.NO_OFFSET_24X);
        int offX = Math.round(info.xOffset() * quest.nodeScale());
        int offY = Math.round(info.yOffset() * quest.nodeScale());
        int width = Math.max(1, Math.round(info.width() * quest.nodeScale()));
        int height = Math.max(1, Math.round(info.height() * quest.nodeScale()));

        int minPosX = canvasMinX - offX;
        int minPosY = canvasMinY - offY;
        int maxPosX = canvasMaxX - offX - width;
        int maxPosY = canvasMaxY - offY - height;

        int clampedX = maxPosX < minPosX ? (minPosX + maxPosX) / 2 : Mth.clamp(x, minPosX, maxPosX);
        int clampedY = maxPosY < minPosY ? (minPosY + maxPosY) / 2 : Mth.clamp(y, minPosY, maxPosY);
        return new Vector2i(clampedX, clampedY);
    }
}
