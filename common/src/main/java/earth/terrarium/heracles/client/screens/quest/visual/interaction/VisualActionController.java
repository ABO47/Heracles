package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.toolbar.VisualTextEditingActions;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.modals.ColorPickerModal;
import earth.terrarium.heracles.client.widgets.modals.AssetsLibraryModal;
import earth.terrarium.heracles.client.widgets.modals.CanvasSpriteOptionsModal;
import earth.terrarium.heracles.client.widgets.modals.EntityMotionModal;
import earth.terrarium.heracles.client.widgets.modals.EntityVariantLibraryModal;
import earth.terrarium.heracles.client.widgets.modals.ItemModal;
import earth.terrarium.heracles.client.widgets.modals.TextInputModal;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class VisualActionController {
    private final Supplier<Integer> width;
    private final Supplier<Integer> height;
    private final Supplier<Boolean> visualMode;
    private final Supplier<ItemModal> itemModal;
    private final Supplier<MultiLineEditBox> descriptionBox;
    private final Supplier<MultiLineEditBox> visualTextEditor;
    private final List<VisualElement> visualElements;
    private final VisualEditorState visualState;
    private final VisualInteractionState interactionState;
    private final Map<Integer, net.minecraft.world.entity.Entity> entityPreviewCache;
    private final Map<Integer, String> entityPreviewIds;
    private final Runnable closeDescriptionContextMenu;
    private final Function<String, int[]> resolveInsertedImageSize;
    private final Consumer<String> insertRawSnippetAndSync;
    private final Consumer<VisualElement> addVisualElementAndSync;
    private final Runnable syncRawFromVisual;
    private final Supplier<String> visualBackgroundSrc;
    private final Consumer<String> setVisualBackgroundSrc;
    private final Runnable clearVisualBackground;
    private final Supplier<Integer> visualBackgroundOpacity;
    private final IntConsumer setVisualBackgroundOpacity;
    private final IntConsumer setSelectedOpacity;
    private final Consumer<String> setSelectedEntityVariant;
    private final Runnable onSelectedEntityMotionChanged;
    private final Consumer<AssetsLibraryModal> addAssetsModal;
    private final Consumer<CanvasSpriteOptionsModal> addCanvasSpriteModal;
    private final Consumer<EntityVariantLibraryModal> addEntityVariantModal;
    private final Consumer<EntityMotionModal> addEntityMotionModal;
    private final Consumer<ColorPickerModal> addColorPickerModal;
    private final Consumer<TextInputModal<Void>> addTextInputModal;

    public VisualActionController(
        Supplier<Integer> width,
        Supplier<Integer> height,
        Supplier<Boolean> visualMode,
        Supplier<ItemModal> itemModal,
        Supplier<MultiLineEditBox> descriptionBox,
        Supplier<MultiLineEditBox> visualTextEditor,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Map<Integer, net.minecraft.world.entity.Entity> entityPreviewCache,
        Map<Integer, String> entityPreviewIds,
        Runnable closeDescriptionContextMenu,
        Function<String, int[]> resolveInsertedImageSize,
        Consumer<String> insertRawSnippetAndSync,
        Consumer<VisualElement> addVisualElementAndSync,
        Runnable syncRawFromVisual,
        Supplier<String> visualBackgroundSrc,
        Consumer<String> setVisualBackgroundSrc,
        Runnable clearVisualBackground,
        Supplier<Integer> visualBackgroundOpacity,
        IntConsumer setVisualBackgroundOpacity,
        IntConsumer setSelectedOpacity,
        Consumer<String> setSelectedEntityVariant,
        Runnable onSelectedEntityMotionChanged,
        Consumer<AssetsLibraryModal> addAssetsModal,
        Consumer<CanvasSpriteOptionsModal> addCanvasSpriteModal,
        Consumer<EntityVariantLibraryModal> addEntityVariantModal,
        Consumer<EntityMotionModal> addEntityMotionModal,
        Consumer<ColorPickerModal> addColorPickerModal,
        Consumer<TextInputModal<Void>> addTextInputModal
    ) {
        this.width = width;
        this.height = height;
        this.visualMode = visualMode;
        this.itemModal = itemModal;
        this.descriptionBox = descriptionBox;
        this.visualTextEditor = visualTextEditor;
        this.visualElements = visualElements;
        this.visualState = visualState;
        this.interactionState = interactionState;
        this.entityPreviewCache = entityPreviewCache;
        this.entityPreviewIds = entityPreviewIds;
        this.closeDescriptionContextMenu = closeDescriptionContextMenu;
        this.resolveInsertedImageSize = resolveInsertedImageSize;
        this.insertRawSnippetAndSync = insertRawSnippetAndSync;
        this.addVisualElementAndSync = addVisualElementAndSync;
        this.syncRawFromVisual = syncRawFromVisual;
        this.visualBackgroundSrc = visualBackgroundSrc;
        this.setVisualBackgroundSrc = setVisualBackgroundSrc;
        this.clearVisualBackground = clearVisualBackground;
        this.visualBackgroundOpacity = visualBackgroundOpacity;
        this.setVisualBackgroundOpacity = setVisualBackgroundOpacity;
        this.setSelectedOpacity = setSelectedOpacity;
        this.setSelectedEntityVariant = setSelectedEntityVariant;
        this.onSelectedEntityMotionChanged = onSelectedEntityMotionChanged;
        this.addAssetsModal = addAssetsModal;
        this.addCanvasSpriteModal = addCanvasSpriteModal;
        this.addEntityVariantModal = addEntityVariantModal;
        this.addEntityMotionModal = addEntityMotionModal;
        this.addColorPickerModal = addColorPickerModal;
        this.addTextInputModal = addTextInputModal;
    }

    public void openInlineItemInserter() {
        openInlineInserter(VisualInsertionController.InserterType.ITEM, false);
    }

    public void openInlineEntityInserter() {
        openInlineInserter(VisualInsertionController.InserterType.ENTITY, false);
    }

    public void openInlineImageInserter() {
        openInlineInserter(VisualInsertionController.InserterType.IMAGE, false);
    }

    public void openInlineInserter(VisualInsertionController.InserterType type, boolean replaceSelected) {
        VisualInsertionController.openInlineInserter(
            type,
            replaceSelected,
            this.visualMode.get(),
            this.itemModal.get(),
            this.descriptionBox.get(),
            this.visualElements,
            this.visualState,
            this.interactionState,
            this.entityPreviewCache,
            this.entityPreviewIds,
            this.closeDescriptionContextMenu,
            this::openAssetsLibraryModal,
            this.resolveInsertedImageSize,
            this.insertRawSnippetAndSync,
            this.addVisualElementAndSync,
            this.syncRawFromVisual
        );
    }

    public void openVisualBackgroundPicker() {
        VisualModalDispatcher.openVisualBackgroundPicker(
            this.descriptionBox.get(),
            this.closeDescriptionContextMenu,
            this::openAssetsLibraryModal,
            this.setVisualBackgroundSrc,
            this.syncRawFromVisual
        );
    }

    public void removeVisualBackground() {
        VisualModalDispatcher.removeVisualBackground(
            this.visualBackgroundSrc,
            this.clearVisualBackground,
            this.syncRawFromVisual
        );
    }

    public void openBackgroundOpacityModal() {
        VisualModalDispatcher.openBackgroundOpacityModal(
            this.width.get(),
            this.height.get(),
            this.visualBackgroundSrc,
            this.visualBackgroundOpacity.get(),
            this.setVisualBackgroundOpacity,
            this.addCanvasSpriteModal
        );
    }

    public void openSelectedOpacityModal() {
        VisualModalDispatcher.openSelectedOpacityModal(
            this.width.get(),
            this.height.get(),
            this.visualElements,
            this.visualState,
            this.setSelectedOpacity,
            this.addCanvasSpriteModal
        );
    }

    public void openEntityVariantPicker() {
        VisualModalDispatcher.openEntityVariantPicker(
            this.width.get(),
            this.height.get(),
            this.visualElements,
            this.visualState,
            this.setSelectedEntityVariant,
            this.addEntityVariantModal
        );
    }

    public void openSelectedEntityMotionModal() {
        VisualModalDispatcher.openSelectedEntityMotionModal(
            this.width.get(),
            this.height.get(),
            this.visualElements,
            this.visualState,
            this.onSelectedEntityMotionChanged,
            this.addEntityMotionModal
        );
    }

    public void openSelectedTextColorPicker() {
        if (!VisualTextEditingActions.ensureSelectedTextElement(this.visualElements, this.visualState)) return;
        VisualElement element = this.visualElements.get(this.visualState.selectedVisualElement);
        ColorPickerModal modal = new ColorPickerModal(this.width.get(), this.height.get(), element.textColor, color -> {
            if (!VisualTextEditingActions.applyColorToVisualSelection(
                color & 0xFFFFFF,
                this.visualTextEditor.get(),
                this.visualState,
                this.visualElements,
                this.interactionState
            )) {
                element.textColor = color & 0xFFFFFF;
                this.syncRawFromVisual.run();
            }
        });
        this.addColorPickerModal.accept(modal);
        modal.setVisible(true);
    }

    public void openSelectedTextSizeModal() {
        if (!VisualTextEditingActions.ensureSelectedTextElement(this.visualElements, this.visualState)) return;
        VisualElement element = this.visualElements.get(this.visualState.selectedVisualElement);
        TextInputModal<Void> modal = new TextInputModal<>(
            this.width.get(),
            this.height.get(),
            Component.literal("Text Size (%)"),
            (ignored, raw) -> {
                int parsed = parseTextSize(raw);
                if (parsed < 0) return;
                VisualTextEditingActions.setSelectedTextSize(this.visualElements, this.visualState, parsed, this.syncRawFromVisual);
            },
            VisualActionController::isValidTextSize
        );
        modal.setInitialValue(String.valueOf(element.textSize <= 0 ? 100 : element.textSize));
        this.addTextInputModal.accept(modal);
        modal.setVisible(true);
    }

    private void openAssetsLibraryModal(Consumer<String> onSelected) {
        final AssetsLibraryModal[] modalRef = new AssetsLibraryModal[1];
        modalRef[0] = new AssetsLibraryModal(this.width.get(), this.height.get(), selected -> {
            if (selected == null || selected.isBlank()) return;
            onSelected.accept(selected);
            if (modalRef[0] != null) modalRef[0].setVisible(false);
        });
        this.addAssetsModal.accept(modalRef[0]);
        modalRef[0].setVisible(true);
    }

    private static boolean isValidTextSize(String raw) {
        return parseTextSize(raw) >= 0;
    }

    private static int parseTextSize(String raw) {
        if (raw == null) return -1;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return -1;
        try {
            return Math.max(50, Math.min(300, Integer.parseInt(trimmed)));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }
}
