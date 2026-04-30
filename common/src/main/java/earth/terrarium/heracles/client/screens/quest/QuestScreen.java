package earth.terrarium.heracles.client.screens.quest;

import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.quest.rewards.RewardListWidget;
import earth.terrarium.heracles.client.screens.quest.tasks.TaskListWidget;
import earth.terrarium.heracles.client.screens.quest.visual.codec.VisualModelCodec;
import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualCanvasMetrics;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualCanvasController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.render.VisualRenderer;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.menus.quest.QuestContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.quests.OpenQuestPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestScreen extends BaseQuestScreen {
    private static final int VISUAL_CANVAS_STEP = 32;

    private TaskListWidget taskList;
    private RewardListWidget rewardList;

    private final List<VisualElement> visualElements = new ArrayList<>();
    private String visualBackgroundSrc = "";
    private int visualBackgroundOpacity = 100;
    private final VisualEditorState visualState = new VisualEditorState();
    private final VisualInteractionState interactionState = new VisualInteractionState();
    private final Map<Integer, net.minecraft.world.entity.Entity> entityPreviewCache = new HashMap<>();
    private final Map<Integer, String> entityPreviewIds = new HashMap<>();
    private VisualCanvasController canvasController;
    private int visualGridSize = 16;

    private int contentX;
    private int contentWidth;
    private int contentHeight;
    private int contentY;

    public QuestScreen(QuestContent content) {
        super(content);
    }

    @Override
    public void updateProgress(@Nullable QuestProgress newProgress) {
        super.updateProgress(newProgress);
        this.taskList.update(this.quest().tasks().values());
        this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());

        calculateContentArea();
        rebuildVisualModel();
        if (this.canvasController != null) {
            this.canvasController.clampVisualScroll();
        }
    }

    @Override
    protected void init() {
        super.init();
        calculateContentArea();

        this.taskList = new TaskListWidget(contentX, contentY, contentWidth, contentHeight, 5.0D, 5.0D, this.content.id(), this.entry(), this.content.progress(), this.content.quests(), null, null);
        this.rewardList = new RewardListWidget(contentX, contentY, contentWidth, contentHeight, 5.0D, 5.0D, this.entry(), this.content.progress(), null, null);
        this.visualGridSize = normalizeVisualGridSize(DisplayConfig.questDescriptionGridSize);
        this.canvasController = new VisualCanvasController(
            this.visualElements,
            this.visualState,
            this.interactionState,
            () -> this.contentX,
            () -> this.contentY,
            this::getVisualCanvasWidth,
            this::getVisualCanvasHeight,
            () -> this.visualGridSize,
            () -> false,
            () -> false,
            () -> false,
            () -> 6,
            ignored -> {},
            () -> {}
        );
        rebuildVisualModel();
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2) && Minecraft.getInstance().player.isCreative()) {
            addRenderableWidget(new ImageButton(this.width - 24, 1, 11, 11, 33, 15, 11, HEADING, 256, 256, (button) ->
                NetworkHandler.CHANNEL.sendToServer(new OpenQuestPacket(this.content.fromGroup(), this.content.id(), true))
            )).setTooltip(Tooltip.create(ConstantComponents.TOGGLE_EDIT));
        }
        updateProgress(null);
    }

    private void calculateContentArea() {
        this.contentWidth = (int) (this.width * 0.63f);
        this.contentHeight = this.height - 17;
        this.contentX = (int) (this.width * 0.31f);
        this.contentY = 17;
    }

    @Override
    public GuiEventListener getTaskList() {
        return this.taskList;
    }

    @Override
    public GuiEventListener getRewardList() {
        return this.rewardList;
    }

    @Override
    public GuiEventListener getDescriptionWidget() {
        return null;
    }

    @Override
    public String getDescriptionError() {
        return null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (this.overview == null || this.overview.isSelected()) {
            renderVisualOverview(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (this.overview != null && this.overview.isSelected() && isInVisualArea(mouseX, mouseY)) {
            int viewH = getVisualCanvasHeight();
            int canvasH = VisualCanvasMetrics.getCanvasHeight(viewH, this.visualElements, this.visualGridSize);
            int maxScrollY = Math.max(0, canvasH - viewH);
            int next = this.visualState.visualScrollY - (int) Math.round(delta * 24.0);
            this.visualState.visualScrollY = Mth.clamp(next, 0, maxScrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void rebuildVisualModel() {
        this.visualElements.clear();
        this.entityPreviewCache.clear();
        this.entityPreviewIds.clear();
        String raw = String.join("\n", this.quest().display().description()).replace("§", "&&");
        var parsed = VisualModelCodec.parse(raw, getVisualCanvasWidth());
        this.visualElements.addAll(parsed.elements());
        this.visualBackgroundSrc = parsed.backgroundSrc();
        this.visualBackgroundOpacity = parsed.backgroundOpacity();
        if (this.canvasController != null) {
            this.canvasController.clampVisualScroll();
        }
    }

    private void renderVisualOverview(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.canvasController == null) return;
        VisualRenderer.renderVisualCanvas(
            graphics,
            mouseX,
            mouseY,
            this.canvasController::clampVisualScroll,
            () -> this.visualBackgroundSrc,
            this::resolveImageTexture,
            this.visualBackgroundOpacity,
            this.contentX,
            this.contentY,
            getVisualCanvasWidth(),
            getVisualCanvasHeight(),
            false,
            this.visualGridSize,
            this.visualState.visualScrollX,
            this.visualState.visualScrollY,
            this.font,
            this.visualElements,
            java.util.Set.of(),
            -1,
            -1,
            null,
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY,
            x -> this.canvasController.screenToCanvasX((double) x),
            y -> this.canvasController.screenToCanvasY((double) y),
            this::getOrCreatePreviewEntity,
            -1,
            -1,
            false,
            0,
            0,
            0,
            0,
            (g, pos) -> {},
            false
        );
    }

    private boolean isInVisualArea(double mouseX, double mouseY) {
        int visualWidth = getVisualCanvasWidth();
        int visualHeight = getVisualCanvasHeight();
        return mouseX >= this.contentX && mouseX <= this.contentX + visualWidth
            && mouseY >= this.contentY && mouseY <= this.contentY + visualHeight;
    }

    private ResourceLocation resolveImageTexture(String src) {
        if (src == null || src.isBlank()) return null;
        if (src.startsWith("assets/")) {
            return earth.terrarium.heracles.client.handlers.CustomImageManager.getTexture(src);
        }
        ResourceLocation location = ResourceLocation.tryParse(src);
        if (location != null) return location;
        return earth.terrarium.heracles.client.handlers.CustomImageManager.getTexture(src);
    }

    private @Nullable net.minecraft.world.entity.Entity getOrCreatePreviewEntity(int index, EntityType<?> entityType) {
        if (Minecraft.getInstance().level == null) return null;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(entityType).toString();
        String cachedId = this.entityPreviewIds.get(index);
        if (!id.equals(cachedId) || !this.entityPreviewCache.containsKey(index)) {
            var created = entityType.create(Minecraft.getInstance().level);
            if (created == null) return null;
            this.entityPreviewCache.put(index, created);
            this.entityPreviewIds.put(index, id);
        }
        return this.entityPreviewCache.get(index);
    }

    private static int normalizeVisualGridSize(int value) {
        if (value <= 4) return 4;
        if (value <= 8) return 8;
        if (value <= 16) return 16;
        return 32;
    }

    private int getVisualCanvasWidth() {
        return snapDownToStep(this.contentWidth, VISUAL_CANVAS_STEP);
    }

    private int getVisualCanvasHeight() {
        return this.contentHeight;
    }

    private static int snapDownToStep(int value, int step) {
        int s = Math.max(1, step);
        if (value <= s) return value;
        int snapped = (value / s) * s;
        return Math.max(s, snapped);
    }
}

