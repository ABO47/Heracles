package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.handlers.DisplayConfig;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.screens.quest.visual.render.VisualElementResolvers;
import earth.terrarium.heracles.client.widgets.modals.CanvasSpriteOptionsModal;
import earth.terrarium.heracles.client.widgets.modals.EntityMotionModal;
import earth.terrarium.heracles.client.widgets.modals.EntityVariantLibraryModal;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class VisualModalDispatcher {
    private VisualModalDispatcher() {
    }

    public static void openVisualBackgroundPicker(
        Object descriptionBox,
        Runnable closeDescriptionContextMenu,
        Consumer<Consumer<String>> openAssetsLibraryModal,
        Consumer<String> setVisualBackgroundSrc,
        Runnable onSyncRawFromVisual
    ) {
        if (descriptionBox == null) return;
        closeDescriptionContextMenu.run();
        openAssetsLibraryModal.accept(selected -> {
            setVisualBackgroundSrc.accept(selected.replace("\"", ""));
            onSyncRawFromVisual.run();
        });
    }

    public static void removeVisualBackground(
        Supplier<String> getVisualBackgroundSrc,
        Runnable clearVisualBackground,
        Runnable onSyncRawFromVisual
    ) {
        String src = getVisualBackgroundSrc.get();
        if (src == null || src.isBlank()) return;
        clearVisualBackground.run();
        onSyncRawFromVisual.run();
    }

    public static void openBackgroundOpacityModal(
        int width,
        int height,
        Supplier<String> getVisualBackgroundSrc,
        int visualBackgroundOpacity,
        IntConsumer setVisualBackgroundOpacity,
        Consumer<CanvasSpriteOptionsModal> addTemporary
    ) {
        DisplayConfig.CanvasSprite sprite = new DisplayConfig.CanvasSprite(
            "quest_description_background",
            getVisualBackgroundSrc.get() == null ? "" : getVisualBackgroundSrc.get(),
            0, 0, 1, 1, visualBackgroundOpacity
        );
        CanvasSpriteOptionsModal modal = new CanvasSpriteOptionsModal(width, height, sprite, opacity -> {
            setVisualBackgroundOpacity.accept(Mth.clamp(opacity, 0, 100));
        });
        addTemporary.accept(modal);
        modal.setVisible(true);
    }

    public static void openSelectedOpacityModal(
        int width,
        int height,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        IntConsumer setSelectedOpacity,
        Consumer<CanvasSpriteOptionsModal> addTemporary
    ) {
        if (visualState.selectedVisualElement < 0 || visualState.selectedVisualElement >= visualElements.size()) return;
        VisualElement selected = visualElements.get(visualState.selectedVisualElement);
        if (selected.type != VisualElementType.TEXT && selected.type != VisualElementType.IMAGE) return;
        DisplayConfig.CanvasSprite sprite = new DisplayConfig.CanvasSprite(
            "quest_description_element",
            "selected_element",
            0, 0, 1, 1, selected.opacity
        );
        CanvasSpriteOptionsModal modal = new CanvasSpriteOptionsModal(width, height, sprite, setSelectedOpacity::accept);
        addTemporary.accept(modal);
        modal.setVisible(true);
    }

    public static void openEntityVariantPicker(
        int width,
        int height,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        Consumer<String> setSelectedEntityVariant,
        Consumer<EntityVariantLibraryModal> addTemporary
    ) {
        if (visualState.selectedVisualElement < 0 || visualState.selectedVisualElement >= visualElements.size()) return;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        if (element.type != VisualElementType.ENTITY) return;
        EntityType<?> type = VisualElementResolvers.parseEntityType(element);
        if (!VisualElementResolvers.isVillagerLike(type)) return;
        EntityVariantLibraryModal modal = new EntityVariantLibraryModal(width, height, type, element.entityVariant, setSelectedEntityVariant::accept);
        addTemporary.accept(modal);
        modal.setVisible(true);
    }

    public static void openSelectedEntityMotionModal(
        int width,
        int height,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        Runnable onSyncRawFromVisual,
        Consumer<EntityMotionModal> addTemporary
    ) {
        if (visualState.selectedVisualElement < 0 || visualState.selectedVisualElement >= visualElements.size()) return;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        if (element.type != VisualElementType.ENTITY) return;
        EntityMotionModal modal = new EntityMotionModal(width, height, element.rotation, element.spinSpeed, state -> {
            element.rotation = state.rotation();
            element.spinSpeed = state.spinSpeed();
            onSyncRawFromVisual.run();
        });
        addTemporary.accept(modal);
        modal.setVisible(true);
    }
}
