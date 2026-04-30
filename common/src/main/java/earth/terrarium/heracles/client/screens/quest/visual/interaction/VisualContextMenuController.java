package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualCanvasMetrics;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.render.VisualElementResolvers;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.modals.EditorTextContextMenu;
import net.minecraft.world.entity.EntityType;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

public final class VisualContextMenuController {
    private VisualContextMenuController() {
    }

    public static boolean handleDescriptionContextMenuClick(
        @Nullable EditorTextContextMenu descriptionContextMenu,
        double mouseX,
        double mouseY,
        int button,
        Consumer<EditorTextContextMenu> hideMenu
    ) {
        if (descriptionContextMenu == null || !descriptionContextMenu.isVisible()) return false;
        if (descriptionContextMenu.mouseClicked(mouseX, mouseY, button)) return true;
        if (button == 0 && !descriptionContextMenu.isMouseOver(mouseX, mouseY)) {
            hideMenu.accept(descriptionContextMenu);
            return true;
        }
        return false;
    }

    public static @Nullable EditorTextContextMenu createDescriptionContextMenu(
        @Nullable MultiLineEditBox descriptionBox,
        int mouseX,
        int mouseY,
        Runnable onPasteSync,
        Runnable openInlineItemInserter,
        Runnable openInlineImageInserter,
        Runnable openInlineEntityInserter
    ) {
        if (descriptionBox == null) return null;
        List<EditorTextContextMenu.MenuItem> items = VisualContextMenuBuilder.buildDescriptionMenu(
            descriptionBox::copySelectionToClipboard,
            descriptionBox::cutSelectionToClipboard,
            onPasteSync,
            descriptionBox::selectAllText,
            openInlineItemInserter,
            openInlineImageInserter,
            openInlineEntityInserter
        );
        return new EditorTextContextMenu(mouseX, mouseY, items);
    }

    public static void resolveContextTarget(
        int mouseX,
        int mouseY,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        int mouseCanvasX = screenToCanvasX.applyAsInt(mouseX);
        int mouseCanvasY = screenToCanvasY.applyAsInt(mouseY);
        int hit = VisualInteractionController.findTopElement(
            visualElements,
            mouseX,
            mouseY,
            mouseCanvasX,
            mouseCanvasY,
            canvasToScreenX,
            canvasToScreenY
        );

        int[] multiBounds = VisualInteractionController.getMultiSelectionBounds(multiSelectedVisualElements, visualElements);
        boolean insideMultiSelection = multiBounds != null
            && VisualInteractionController.isInMultiSelectionBounds(mouseX, mouseY, multiBounds, canvasToScreenX, canvasToScreenY);

        if (!insideMultiSelection && hit >= 0) {
            visualState.selectedVisualElement = hit;
            multiSelectedVisualElements.clear();
            multiSelectedVisualElements.add(hit);
        }
        if (insideMultiSelection && (visualState.selectedVisualElement < 0 || !multiSelectedVisualElements.contains(visualState.selectedVisualElement))) {
            visualState.selectedVisualElement = multiSelectedVisualElements.stream().findFirst().orElse(-1);
        }
        interactionState.contextVisualX = Mth.clamp(mouseCanvasX, 0, Math.max(0, VisualCanvasMetrics.FIXED_CANVAS_WIDTH - 24));
        interactionState.contextVisualY = Mth.clamp(mouseCanvasY, 0, Math.max(0, VisualCanvasMetrics.MAX_CANVAS_HEIGHT - 14));
    }

    public static @Nullable VisualElement getSelectedVisualElementOrNull(List<VisualElement> visualElements, VisualEditorState visualState) {
        if (visualState.selectedVisualElement < 0 || visualState.selectedVisualElement >= visualElements.size()) {
            return null;
        }
        return visualElements.get(visualState.selectedVisualElement);
    }

    public static EditorTextContextMenu createVisualContextMenu(
        int mouseX,
        int mouseY,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        boolean hasClipboardElement,
        Runnable openVisualBackgroundPicker,
        Runnable removeVisualBackground,
        Runnable addTextElementAtContext,
        Runnable openInlineItemInserter,
        Runnable openInlineImageInserter,
        Runnable openInlineEntityInserter,
        Runnable copySelectedVisualElement,
        Runnable duplicateSelectedVisualElement,
        Runnable startVisualTextEditing,
        Runnable replaceSelectedWithInlineItem,
        Runnable replaceSelectedWithInlineImage,
        Runnable replaceSelectedWithInlineEntity,
        Runnable openEntityVariantPicker,
        Runnable openSelectedEntityMotionModal,
        Runnable openSelectedOpacityModal,
        Runnable bringSelectedToFront,
        Runnable sendSelectedToBack,
        Runnable removeSelectedVisualElement,
        Runnable pasteVisualElementAtContext
    ) {
        resolveContextTarget(
            mouseX,
            mouseY,
            visualElements,
            multiSelectedVisualElements,
            visualState,
            interactionState,
            screenToCanvasX,
            screenToCanvasY,
            canvasToScreenX,
            canvasToScreenY
        );
        VisualElement selected = getSelectedVisualElementOrNull(visualElements, visualState);
        EntityType<?> type = selected == null ? null : VisualElementResolvers.parseEntityType(selected);
        boolean villagerLike = selected != null && VisualElementResolvers.isVillagerLike(type);
        List<EditorTextContextMenu.MenuItem> items = VisualContextMenuBuilder.buildVisualMenu(
            openVisualBackgroundPicker,
            removeVisualBackground,
            addTextElementAtContext,
            openInlineItemInserter,
            openInlineImageInserter,
            openInlineEntityInserter,
            selected,
            villagerLike,
            hasClipboardElement,
            copySelectedVisualElement,
            duplicateSelectedVisualElement,
            startVisualTextEditing,
            replaceSelectedWithInlineItem,
            replaceSelectedWithInlineImage,
            replaceSelectedWithInlineEntity,
            openEntityVariantPicker,
            openSelectedEntityMotionModal,
            openSelectedOpacityModal,
            bringSelectedToFront,
            sendSelectedToBack,
            removeSelectedVisualElement,
            pasteVisualElementAtContext
        );
        return new EditorTextContextMenu(mouseX, mouseY, items);
    }
}
