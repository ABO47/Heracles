package earth.terrarium.heracles.client.screens.quest;

import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.rewards.QuestReward;
import earth.terrarium.heracles.api.rewards.QuestRewardType;
import earth.terrarium.heracles.api.tasks.QuestTask;
import earth.terrarium.heracles.api.tasks.QuestTaskType;
import earth.terrarium.heracles.client.handlers.ClientQuests;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.quest.flow.QuestInputRouter;
import earth.terrarium.heracles.client.screens.quest.flow.QuestPointerInputRouter;
import earth.terrarium.heracles.client.screens.quest.flow.QuestTaskRewardController;
import earth.terrarium.heracles.client.screens.quest.flow.QuestVisualMenuLauncher;
import earth.terrarium.heracles.client.screens.quest.flow.QuestVisualRenderCoordinator;
import earth.terrarium.heracles.client.screens.quest.flow.QuestMouseClickCoordinator;
import earth.terrarium.heracles.client.screens.quest.visual.codec.VisualModelCodec;
import earth.terrarium.heracles.client.screens.quest.editing.QuestMultiLineEditBox;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualCanvasController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualActionController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualSelectionOps;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualTextEditorLifecycle;
import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualCanvasMetrics;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.rewards.RewardListWidget;
import earth.terrarium.heracles.client.screens.quest.tasks.TaskListWidget;
import earth.terrarium.heracles.client.screens.quest.visual.toolbar.VisualInlineToolbarController;
import earth.terrarium.heracles.client.screens.quest.visual.toolbar.VisualToolbarBuilder;
import earth.terrarium.heracles.client.screens.quest.visual.toolbar.VisualToolbarCoordinator;
import earth.terrarium.heracles.client.screens.quest.visual.toolbar.VisualToolbarStateApplier;
import earth.terrarium.heracles.client.screens.quest.visual.toolbar.VisualTextEditingActions;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.modals.CreateObjectModal;
import earth.terrarium.heracles.client.widgets.modals.EditorTextContextMenu;
import earth.terrarium.heracles.client.widgets.modals.ItemModal;
import earth.terrarium.heracles.common.constants.ConstantComponents;
import earth.terrarium.heracles.common.handlers.progress.QuestProgress;
import earth.terrarium.heracles.common.handlers.quests.QuestHandler;
import earth.terrarium.heracles.common.menus.quest.QuestContent;
import earth.terrarium.heracles.common.network.NetworkHandler;
import earth.terrarium.heracles.common.network.packets.quests.OpenQuestPacket;
import earth.terrarium.heracles.common.network.packets.quests.data.NetworkQuestData;
import earth.terrarium.heracles.common.utils.ModUtils;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class QuestEditScreen extends BaseQuestScreen {
    private static final int VISUAL_CANVAS_STEP = 32;

    private enum DescriptionTab {
        VISUAL,
        RAW
    }

    private TaskListWidget taskList;
    private RewardListWidget rewardList;
    private MultiLineEditBox descriptionBox;
    private MultiLineEditBox visualTextEditor;

    private static final ResourceLocation GRID_TOGGLE_CUSTOM_TEXTURE = new ResourceLocation(Heracles.MOD_ID, "textures/gui/grid_toggle_button.png");
    private static final ResourceLocation GRID_LOCK_CUSTOM_TEXTURE = new ResourceLocation(Heracles.MOD_ID, "textures/gui/grid_lock_button.png");
    private static final ResourceLocation INLINE_TOOLBAR_TEXTURE = new ResourceLocation(Heracles.MOD_ID, "textures/gui/editor.png");

    private CreateObjectModal createModal;
    private ItemModal itemModal;
    private EditorTextContextMenu descriptionContextMenu;
    private VisualToolbarBuilder.ToolbarWidgets toolbar;

    private int contentX;
    private int contentY;
    private int contentWidth;
    private int contentHeight;
    private int descriptionX;
    private int descriptionY;
    private int descriptionWidth;
    private int descriptionHeight;

    private final List<VisualElement> visualElements = new ArrayList<>();
    private DescriptionTab descriptionTab = DescriptionTab.VISUAL;
    private boolean visualGridEnabled = true;
    private boolean visualGridSnap = false;
    private int visualGridSize = 16;
    private boolean guideSnapEnabled = true;
    private boolean centerSnapEnabled = true;
    private int guideSnapDistance = 6;
    private String lastRawDescription = "";
    private boolean syncingRawFromVisual = false;
    private final Set<Integer> multiSelectedVisualElements = new HashSet<>();
    private final Map<Integer, net.minecraft.world.entity.Entity> entityPreviewCache = new HashMap<>();
    private final Map<Integer, String> entityPreviewIds = new HashMap<>();
    private VisualElement clipboardVisualElement;
    private static final String[] INLINE_TEXT_TOOLS = new String[]{"B", "I", "U", "S", "Q", "Inv", "L", "C", "R", "Clr", "Case", "Sz"};
    private static final int INLINE_TOOL_COLUMNS = 6;
    private String visualBackgroundSrc = "";
    private int visualBackgroundOpacity = 100;
    private final VisualEditorState visualState = new VisualEditorState();
    private final VisualInteractionState interactionState = new VisualInteractionState();
    private VisualCanvasController canvasController;
    private VisualSelectionOps selectionOps;
    private VisualActionController visualActions;
    private QuestVisualRenderCoordinator visualRenderCoordinator;
    private QuestMouseClickCoordinator mouseClickCoordinator;

    public QuestEditScreen(QuestContent content) {
        super(content);
    }

    @Override
    public void updateProgress(@Nullable QuestProgress newProgress) {
        super.updateProgress(newProgress);
        this.taskList.update(this.quest().tasks().values());
        this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());
    }

    @Override
    protected void rebuildWidgets() {
        var oldBox = this.descriptionBox;
        if (oldBox != null) {
            saveDescription();
        }
        super.rebuildWidgets();
        if (oldBox != null && this.descriptionBox != null) {
            this.descriptionBox.setValue(oldBox.getValue());
            rebuildVisualElementsFromRaw();
        }
    }

    @Override
    protected void init() {
        super.init();

        this.createModal = addTemporary(new CreateObjectModal(this.width, this.height));
        initLayoutAndConfig();
        initTaskAndRewardLists();
        initDescriptionAndBaseModals();
        initTopActionButtons();
        initToolbarWidgets();
        initControllersAndFinalState();
    }

    private void initLayoutAndConfig() {
        int contentWidth = (int) (this.width * 0.63f);
        int contentHeight = this.height - 17;
        int contentX = (int) (this.width * 0.31f);
        int contentY = 17;
        setupDescriptionLayout(contentX, contentY, contentWidth, contentHeight);
        this.visualGridEnabled = DisplayConfig.questDescriptionGridEnabled;
        this.visualGridSnap = DisplayConfig.questDescriptionGridSnap;
        this.visualGridSize = normalizeVisualGridSize(DisplayConfig.questDescriptionGridSize);
        this.guideSnapEnabled = DisplayConfig.questDescriptionGuideSnapEnabled;
        this.centerSnapEnabled = DisplayConfig.questDescriptionCenterSnapEnabled;
        this.guideSnapDistance = Mth.clamp(DisplayConfig.questDescriptionGuideSnapDistance, 1, 24);
    }

    private void initTaskAndRewardLists() {
        var widgets = QuestTaskRewardController.createWidgets(
            this.contentX,
            this.contentY,
            this.contentWidth,
            this.contentHeight,
            this.content.id(),
            this.content.fromGroup(),
            this.entry(),
            this.content.progress(),
            this.content.quests(),
            this::handleTaskClick,
            this::openTaskCreateModal,
            this::handleRewardClick,
            this::openRewardCreateModal
        );
        this.taskList = widgets.taskList();
        this.rewardList = widgets.rewardList();
    }

    private void handleTaskClick(QuestTask<?, ?, ?> task, boolean isRemoving) {
        if (isRemoving) {
            ClientQuests.updateQuest(this.entry(), quest -> {
                quest.tasks().remove(task.id());
                return NetworkQuestData.builder().tasks(quest.tasks());
            });
            this.taskList.update(this.quest().tasks().values());
            return;
        }
        taskPopup(ModUtils.cast(task.type()), task.id(), ModUtils.cast(task), this.taskList::updateTask);
    }

    private void openTaskCreateModal() {
        QuestTaskRewardController.openTaskCreateModal(
            this.createModal,
            this::quest,
            (id, type) -> taskPopup(ModUtils.cast(type), id, null, newTask -> {
                ClientQuests.updateQuest(this.entry(), quest -> {
                    quest.tasks().put(id, newTask);
                    return NetworkQuestData.builder().tasks(quest.tasks());
                });
                this.taskList.update(this.quest().tasks().values());
            })
        );
    }

    private void handleRewardClick(QuestReward<?> reward, boolean isRemoving) {
        if (isRemoving) {
            ClientQuests.updateQuest(this.entry(), quest -> {
                quest.rewards().remove(reward.id());
                return NetworkQuestData.builder().rewards(quest.rewards());
            });
            this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());
            return;
        }
        rewardPopup(ModUtils.cast(reward.type()), reward.id(), ModUtils.cast(reward), this.rewardList::updateReward);
    }

    private void openRewardCreateModal() {
        QuestTaskRewardController.openRewardCreateModal(
            this.createModal,
            this::quest,
            (id, type) -> rewardPopup(ModUtils.cast(type), id, null, newReward -> {
                ClientQuests.updateQuest(this.entry(), quest -> {
                    quest.rewards().put(id, newReward);
                    return NetworkQuestData.builder().rewards(quest.rewards());
                });
                this.rewardList.update(this.content.fromGroup(), this.content.id(), this.quest());
            })
        );
    }

    private void initDescriptionAndBaseModals() {
        this.descriptionBox = new QuestMultiLineEditBox(this.descriptionX, this.descriptionY, this.descriptionWidth, this.descriptionHeight);
        this.descriptionBox.setValue(String.join("\n", this.quest().display().description()).replace("§", "&&"));
        this.lastRawDescription = this.descriptionBox.getValue();
        rebuildVisualElementsFromRaw();

        this.itemModal = addTemporary(new ItemModal(this.width, this.height));
    }

    private void initTopActionButtons() {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
            addRenderableWidget(new ImageButton(this.width - 24, 1, 11, 11, 33, 15, 11, HEADING, 256, 256, (button) ->
                NetworkHandler.CHANNEL.sendToServer(new OpenQuestPacket(this.content.fromGroup(), this.content.id(), false))
            )).setTooltip(Tooltip.create(ConstantComponents.TOGGLE_EDIT));
        }

        if (Minecraft.getInstance().isLocalServer()) {
            addRenderableWidget(new ImageButton(this.width - 36, 1, 11, 11, 33, 59, 11, HEADING, 256, 256, (button) -> {
                Path path = QuestHandler.getQuestPath(this.quest(), this.getQuestId());
                if (path.toFile().isFile() && path.toFile().exists()) {
                    Util.getPlatform().openFile(path.toFile());
                }
            })).setTooltip(Tooltip.create(ConstantComponents.OPEN_QUEST_FILE));
        }
    }

    private void initToolbarWidgets() {
        VisualToolbarCoordinator toolbarCoordinator = new VisualToolbarCoordinator(
            this.font,
            this.contentX,
            HEADING,
            GRID_TOGGLE_CUSTOM_TEXTURE,
            GRID_LOCK_CUSTOM_TEXTURE,
            () -> switchDescriptionTab(this.descriptionTab == DescriptionTab.VISUAL ? DescriptionTab.RAW : DescriptionTab.VISUAL),
            () -> this.toolbar,
            widgets -> this.toolbar = widgets,
            () -> this.visualGridEnabled,
            value -> this.visualGridEnabled = value,
            () -> this.visualGridSnap,
            value -> this.visualGridSnap = value,
            () -> this.guideSnapEnabled,
            value -> this.guideSnapEnabled = value,
            () -> this.centerSnapEnabled,
            value -> this.centerSnapEnabled = value,
            () -> this.visualGridSize,
            value -> this.visualGridSize = value,
            () -> this.guideSnapDistance,
            value -> this.guideSnapDistance = value,
            () -> this.visualBackgroundOpacity,
            value -> {
                this.visualBackgroundOpacity = value;
                this.interactionState.pendingVisualSync = true;
            },
            this::updateModeButtons,
            () -> applyVisualBackgroundOpacity(false),
            () -> applyVisualBackgroundOpacity(true),
            this::addRenderableWidget
        );
        this.toolbar = toolbarCoordinator.build();
    }

    private void initControllersAndFinalState() {
        this.canvasController = new VisualCanvasController(
            this.visualElements,
            this.visualState,
            this.interactionState,
            () -> this.descriptionX,
            () -> this.descriptionY,
            this::getVisualCanvasWidth,
            this::getVisualCanvasHeight,
            () -> this.visualGridSize,
            () -> this.visualGridSnap,
            () -> this.guideSnapEnabled,
            () -> this.centerSnapEnabled,
            () -> this.guideSnapDistance,
            this::positionVisualTextEditor,
            this::syncRawFromVisual
        );
        this.selectionOps = new VisualSelectionOps(
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            () -> this.clipboardVisualElement,
            value -> this.clipboardVisualElement = value,
            () -> this.interactionState.contextVisualX,
            () -> this.interactionState.contextVisualY,
            this.canvasController::clampElement,
            index -> {
                if (this.visualState.editingTextElement == index) {
                    finishVisualTextEditing(false);
                }
            },
            this::syncRawFromVisual
        );
        this.visualActions = new VisualActionController(
            () -> this.width,
            () -> this.height,
            () -> this.descriptionTab == DescriptionTab.VISUAL,
            () -> this.itemModal,
            () -> this.descriptionBox,
            () -> this.visualTextEditor,
            this.visualElements,
            this.visualState,
            this.interactionState,
            this.entityPreviewCache,
            this.entityPreviewIds,
            () -> {
                if (this.descriptionContextMenu != null) this.descriptionContextMenu.setVisible(false);
            },
            this::resolveInsertedImageSize,
            snippet -> {
                if (this.descriptionBox == null || snippet == null || snippet.isBlank()) return;
                this.descriptionBox.insertTextAtCursor(snippet);
                syncVisualFromRawIfChanged();
            },
            element -> {
                if (element == null) return;
                this.visualElements.add(element);
                this.visualState.selectedVisualElement = this.visualElements.size() - 1;
                this.canvasController.clampElement(element);
                syncRawFromVisual();
            },
            this::syncRawFromVisual,
            () -> this.visualBackgroundSrc,
            src -> {
                this.visualBackgroundSrc = src;
                this.interactionState.pendingVisualSync = true;
            },
            () -> {
                this.visualBackgroundSrc = "";
                this.visualBackgroundOpacity = 100;
                this.interactionState.pendingVisualSync = true;
            },
            () -> this.visualBackgroundOpacity,
            opacity -> {
                this.visualBackgroundOpacity = opacity;
                this.interactionState.pendingVisualSync = true;
                syncRawFromVisual();
            },
            this.selectionOps::setSelectedOpacity,
            this.selectionOps::setSelectedEntityVariant,
            () -> {
                this.interactionState.pendingVisualSync = true;
                syncRawFromVisual();
            },
            this::addTemporary,
            this::addTemporary,
            this::addTemporary,
            this::addTemporary,
            this::addTemporary,
            this::addTemporary
        );
        QuestVisualMenuLauncher visualMenuLauncher = new QuestVisualMenuLauncher(
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            this.canvasController,
            this.selectionOps,
            this.visualActions,
            this::addTextElementAtContext,
            this::startVisualTextEditing,
            this::addTemporary
        );
        this.visualRenderCoordinator = new QuestVisualRenderCoordinator(
            () -> this.canvasController,
            () -> this.visualBackgroundSrc,
            this::resolveImageTexture,
            () -> this.visualBackgroundOpacity,
            () -> this.descriptionX,
            () -> this.descriptionY,
            this::getVisualCanvasWidth,
            this::getVisualCanvasHeight,
            () -> this.visualGridEnabled,
            () -> this.visualGridSize,
            this.font,
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            () -> this.visualTextEditor,
            this::getOrCreatePreviewEntity,
            (g, pos) -> renderInlineToolbar(g, pos[0], pos[1]),
            this::positionVisualTextEditor
        );
        this.mouseClickCoordinator = new QuestMouseClickCoordinator(
            () -> this.descriptionContextMenu,
            value -> this.descriptionContextMenu = value,
            this::isOverviewVisible,
            () -> this.descriptionTab == DescriptionTab.VISUAL,
            () -> this.descriptionTab == DescriptionTab.RAW,
            this::isInDescriptionArea,
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            this.canvasController,
            () -> this.visualTextEditor,
            this::setFocused,
            this::handleInlineToolbarClick,
            () -> finishVisualTextEditing(true),
            this::startVisualTextEditing,
            visualMenuLauncher,
            () -> this.descriptionBox,
            () -> {
                this.descriptionBox.pasteFromClipboard();
                rebuildVisualElementsFromRaw();
            }
        );
        // Ensure elements parsed before controller init are clamped/normalized.
        rebuildVisualElementsFromRaw();
        this.visualTextEditor = null;
        updateModeButtons();
        updateProgress(null);
    }

    private <T extends QuestTask<?, ?, T>> void taskPopup(QuestTaskType<T> type, String id, @Nullable T task, Consumer<T> consumer) {
        QuestObjectModalOps.taskPopup(type, id, task, consumer, this::findOrCreateEditWidget, ConstantComponents.Tasks.EDIT);
    }

    private <T extends QuestReward<T>> void rewardPopup(QuestRewardType<T> type, String id, @Nullable T reward, Consumer<T> consumer) {
        QuestObjectModalOps.rewardPopup(type, id, reward, consumer, this::findOrCreateEditWidget, ConstantComponents.Rewards.EDIT);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (hasBlockingTemporaryModal()) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (QuestInputRouter.handleKeyPressed(
            keyCode,
            this.visualState,
            this.visualTextEditor,
            () -> finishVisualTextEditing(true),
            this::saveDescription,
            isOverviewVisible(),
            this.descriptionTab == DescriptionTab.VISUAL,
            this.visualElements,
            this.canvasController,
            this.selectionOps::removeSelectedVisualElement,
            this::startVisualTextEditing
        )) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void removed() {
        finishVisualTextEditing(true);
        super.removed();
        saveDescription();
    }

    private void saveDescription() {
        if (this.descriptionTab == DescriptionTab.VISUAL) {
            finishVisualTextEditing(true);
            if (this.interactionState.pendingVisualSync) {
                syncRawFromVisual();
            }
        }
        String current = this.descriptionBox.getValue();
        this.lastRawDescription = current;
        ClientQuests.updateQuest(
            entry(),
            quest -> NetworkQuestData.builder().description(List.of(current.split("\n"))),
            false
        );
    }

    private void setupDescriptionLayout(int contentX, int contentY, int contentWidth, int contentHeight) {
        this.contentX = contentX;
        this.contentY = contentY;
        this.contentWidth = contentWidth;
        this.contentHeight = contentHeight;
        this.descriptionX = contentX;
        this.descriptionY = contentY;
        this.descriptionWidth = contentWidth;
        this.descriptionHeight = contentHeight;
    }

    private void switchDescriptionTab(DescriptionTab tab) {
        if (this.descriptionTab == tab) return;
        if (this.descriptionTab == DescriptionTab.VISUAL) {
            finishVisualTextEditing(true);
            if (this.interactionState.pendingVisualSync) {
                syncRawFromVisual();
            }
        }
        this.descriptionTab = tab;
        if (tab == DescriptionTab.VISUAL) {
            rebuildVisualElementsFromRaw();
        }
        updateModeButtons();
    }

    private void updateModeButtons() {
        VisualToolbarStateApplier.apply(
            this.descriptionTab == DescriptionTab.VISUAL,
            this.toolbar == null ? null : this.toolbar.modeToggleButton(),
            this.toolbar == null ? null : this.toolbar.gridButton(),
            this.toolbar == null ? null : this.toolbar.snapButton(),
            this.toolbar == null ? null : this.toolbar.guideSnapButton(),
            this.toolbar == null ? null : this.toolbar.centerSnapButton(),
            this.toolbar == null ? null : this.toolbar.gridSizeMinusButton(),
            this.toolbar == null ? null : this.toolbar.gridSizePlusButton(),
            this.toolbar == null ? null : this.toolbar.gridSizeField(),
            this.toolbar == null ? null : this.toolbar.guideSnapDistanceField(),
            this.toolbar == null ? null : this.toolbar.backgroundOpacitySlider(),
            this.toolbar == null ? null : this.toolbar.backgroundOpacityField(),
            this.visualGridEnabled,
            this.visualGridSnap,
            this.guideSnapEnabled,
            this.centerSnapEnabled,
            this.visualGridSize,
            this.guideSnapDistance,
            this.visualBackgroundOpacity,
            this.visualTextEditor,
            this.visualState
        );
    }

    private boolean isOverviewVisible() {
        return this.overview == null || this.overview.isSelected();
    }

    private boolean isInDescriptionArea(double mouseX, double mouseY) {
        int visualWidth = getVisualCanvasWidth();
        int visualHeight = getVisualCanvasHeight();
        return mouseX >= this.descriptionX && mouseX <= this.descriptionX + visualWidth
            && mouseY >= this.descriptionY && mouseY <= this.descriptionY + visualHeight;
    }

    private void rebuildVisualElementsFromRaw() {
        this.visualElements.clear();
        this.entityPreviewCache.clear();
        this.entityPreviewIds.clear();
        if (this.descriptionBox == null) return;
        var parsed = VisualModelCodec.parse(this.descriptionBox.getValue(), this.descriptionWidth);
        this.visualElements.addAll(parsed.elements());
        this.visualBackgroundSrc = parsed.backgroundSrc();
        this.visualBackgroundOpacity = parsed.backgroundOpacity();

        if (this.canvasController != null) {
            for (VisualElement element : this.visualElements) {
                this.canvasController.clampElementWithoutGridSnap(element);
            }
        }
        if (this.visualState.selectedVisualElement >= this.visualElements.size()) {
            this.visualState.selectedVisualElement = -1;
        }
        if (this.canvasController != null) {
            this.canvasController.clampVisualScroll();
        }
        VisualInteractionController.clearAlignmentGuides(this.visualState);
    }

    private void syncRawFromVisual() {
        if (this.descriptionBox == null) return;
        this.syncingRawFromVisual = true;
        this.descriptionBox.setValue(VisualModelCodec.serialize(this.visualBackgroundSrc, this.visualBackgroundOpacity, this.visualElements));
        this.lastRawDescription = this.descriptionBox.getValue();
        this.syncingRawFromVisual = false;
        this.interactionState.pendingVisualSync = false;
        this.entityPreviewCache.clear();
        this.entityPreviewIds.clear();
    }

    private void syncVisualFromRawIfChanged() {
        if (this.descriptionBox == null || this.syncingRawFromVisual) return;
        String current = this.descriptionBox.getValue();
        if (!current.equals(this.lastRawDescription)) {
            this.lastRawDescription = current;
            rebuildVisualElementsFromRaw();
        }
    }

    private ResourceLocation resolveImageTexture(String src) {
        if (src == null || src.isBlank()) return null;
        if (src.startsWith("assets/")) {
            return CustomImageManager.getTexture(src);
        }
        ResourceLocation location = ResourceLocation.tryParse(src);
        if (location != null) return location;
        return CustomImageManager.getTexture(src);
    }

    private int[] resolveInsertedImageSize(String src) {
        if (src == null || src.isBlank()) return null;
        var size = CustomImageManager.getImageSize(src);
        if (size == null) return null;
        int w = Math.max(24, size.x);
        int h = Math.max(14, size.y);
        int maxW = Math.max(24, getVisualCanvasWidth() - 8);
        int maxH = Math.max(14, getVisualCanvasHeight() - 8);
        if (w > maxW || h > maxH) {
            float scale = Math.min(maxW / (float) Math.max(1, w), maxH / (float) Math.max(1, h));
            w = Math.max(24, Math.round(w * scale));
            h = Math.max(14, Math.round(h * scale));
        }
        return new int[]{w, h};
    }


    private void startVisualTextEditing(int index) {
        this.visualTextEditor = VisualTextEditorLifecycle.startVisualTextEditing(
            index,
            this.visualElements,
            this.visualState,
            this.visualTextEditor,
            this.font,
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY,
            this::addRenderableWidget,
            this::removeWidget,
            this::setFocused,
            this.width,
            this.height
        );
    }

    private void finishVisualTextEditing(boolean syncImmediately) {
        this.visualTextEditor = VisualTextEditorLifecycle.finishVisualTextEditing(
            this.visualTextEditor,
            this.visualElements,
            this.visualState,
            this.interactionState,
            this::removeWidget,
            this::setFocused,
            syncImmediately,
            this::syncRawFromVisual
        );
    }

    private void positionVisualTextEditor(VisualElement element) {
        VisualTextEditorLifecycle.positionVisualTextEditor(
            this.visualTextEditor,
            element,
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY,
            this.width,
            this.height
        );
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

    private boolean hasBlockingTemporaryModal() {
        return VisualInteractionController.hasBlockingModal(this.temporaryWidgets, this.descriptionContextMenu, w -> w.isVisible());
    }

    private void renderInlineToolbar(GuiGraphics graphics, int mouseX, int mouseY) {
        VisualInlineToolbarController.renderInlineToolbar(
            graphics,
            this.font,
            INLINE_TOOLBAR_TEXTURE,
            mouseX,
            mouseY,
            this.visualElements,
            this.visualState,
            this.descriptionX,
            this.descriptionY,
            getVisualCanvasWidth(),
            getVisualCanvasHeight(),
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY,
            INLINE_TOOL_COLUMNS,
            INLINE_TEXT_TOOLS.length
        );
    }

    private boolean handleInlineToolbarClick(double mouseX, double mouseY) {
        return VisualInlineToolbarController.handleInlineToolbarClick(
            mouseX,
            mouseY,
            this.visualElements,
            this.visualState,
            this.interactionState,
            this.visualTextEditor,
            this.descriptionX,
            this.descriptionY,
            getVisualCanvasWidth(),
            getVisualCanvasHeight(),
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY,
            INLINE_TOOL_COLUMNS,
            INLINE_TEXT_TOOLS.length,
            this::syncRawFromVisual,
            this.visualActions::openSelectedTextColorPicker,
            () -> VisualTextEditingActions.toggleCaseSelectionOrElement(this.visualElements, this.visualState, this.visualTextEditor, this.interactionState, this::syncRawFromVisual),
            this.visualActions::openSelectedTextSizeModal
        );
    }

    private void applyVisualBackgroundOpacity(boolean normalize) {
        int parsed = VisualModelCodec.parseInt(this.toolbar == null || this.toolbar.backgroundOpacityField() == null ? "" : this.toolbar.backgroundOpacityField().getValue(), -1, 0, 100);
        if (parsed < 0) return;
        this.visualBackgroundOpacity = parsed;
        if (this.toolbar != null && this.toolbar.backgroundOpacitySlider() != null) this.toolbar.backgroundOpacitySlider().setValue(parsed);
        if (normalize && this.toolbar != null && this.toolbar.backgroundOpacityField() != null) this.toolbar.backgroundOpacityField().setValue(String.valueOf(parsed));
        this.interactionState.pendingVisualSync = true;
    }

    private void addTextElementAtContext() {
        VisualElement element = VisualElement.text(this.interactionState.contextVisualX, this.interactionState.contextVisualY, 220, 60, "");
        this.visualElements.add(element);
        this.visualState.selectedVisualElement = this.visualElements.size() - 1;
        this.canvasController.clampElement(element);
        syncRawFromVisual();
    }

    @Override
    protected void renderBg(net.minecraft.client.gui.GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        if (!isOverviewVisible()) return;
        syncVisualFromRawIfChanged();
        if (this.descriptionTab == DescriptionTab.VISUAL) {
            this.visualRenderCoordinator.renderVisualTab(graphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (hasBlockingTemporaryModal()) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (this.mouseClickCoordinator.handleMouseClicked(mouseX, mouseY, button)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (hasBlockingTemporaryModal()) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        if (QuestPointerInputRouter.handleMouseDragged(
            mouseX,
            mouseY,
            button,
            dragX,
            dragY,
            isOverviewVisible(),
            this.descriptionTab == DescriptionTab.VISUAL,
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            this.visualTextEditor,
            this.visualGridSize,
            this.visualGridSnap,
            this.descriptionX,
            this.descriptionY,
            x -> this.canvasController.screenToCanvasX((double) x),
            y -> this.canvasController.screenToCanvasY((double) y),
            (targetX, targetY) -> this.canvasController.moveElement(this.visualState.selectedVisualElement, targetX, targetY, false, true),
            (targetW, targetH) -> this.canvasController.resizeElement(this.visualState.selectedVisualElement, targetW, targetH, false),
            this.canvasController::clampElement,
            this.canvasController::clampElementPositionToCanvas,
            this::positionVisualTextEditor,
            this.canvasController::elementLocalOffsetToScreen,
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY
        )) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (hasBlockingTemporaryModal()) {
            return super.mouseReleased(mouseX, mouseY, button);
        }
        if (QuestPointerInputRouter.handleMouseReleased(
            mouseX,
            mouseY,
            button,
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            this.visualTextEditor,
            this.descriptionX,
            this.descriptionY,
            this::syncRawFromVisual
        )) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!hasBlockingTemporaryModal()
            && isOverviewVisible()
            && this.descriptionTab == DescriptionTab.VISUAL
            && isInDescriptionArea(mouseX, mouseY)) {
            int viewH = getVisualCanvasHeight();
            int canvasH = VisualCanvasMetrics.getCanvasHeight(viewH, this.visualElements, this.visualGridSize);
            int maxScrollY = Math.max(0, canvasH - viewH);
            int next = this.visualState.visualScrollY - (int) Math.round(delta * 24.0);
            this.visualState.visualScrollY = Mth.clamp(next, 0, maxScrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
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
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // Intentionally hidden in visual editor to keep top tools unobstructed.
    }

    @Override
    public GuiEventListener getDescriptionWidget() {
        return this.descriptionTab == DescriptionTab.RAW ? this.descriptionBox : null;
    }

    @Override
    public String getDescriptionError() {
        return null;
    }

    public ItemModal itemModal() {
        return this.itemModal;
    }

    private int getVisualCanvasWidth() {
        return snapDownToStep(this.descriptionWidth, VISUAL_CANVAS_STEP);
    }

    private int getVisualCanvasHeight() {
        return this.descriptionHeight;
    }

    private static int normalizeVisualGridSize(int value) {
        if (value <= 4) return 4;
        if (value <= 8) return 8;
        if (value <= 16) return 16;
        return 32;
    }

    private static int snapDownToStep(int value, int step) {
        int s = Math.max(1, step);
        if (value <= s) return value;
        int snapped = (value / s) * s;
        return Math.max(s, snapped);
    }

}
