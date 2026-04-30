package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import java.util.Set;

public final class VisualTransformOrchestrator {
    @FunctionalInterface
    public interface MoveSelectedElement {
        boolean move(int targetX, int targetY);
    }

    @FunctionalInterface
    public interface ResizeSelectedElement {
        boolean resize(int targetW, int targetH);
    }

    @FunctionalInterface
    public interface LocalOffsetToScreen {
        double[] resolve(int rotation, int baseW, int baseH, double localX, double localY);
    }

    private VisualTransformOrchestrator() {
    }

    public static boolean isAnyVisualTransformActive(VisualInteractionState interactionState) {
        return interactionState.draggingVisualElement
            || interactionState.draggingMultiSelection
            || interactionState.resizingVisualElement
            || interactionState.resizingMultiSelection
            || interactionState.rotatingVisualElement
            || interactionState.rotatingMultiSelection;
    }

    public static void resetVisualTransformState(VisualInteractionState interactionState) {
        interactionState.draggingVisualElement = false;
        interactionState.draggingMultiSelection = false;
        interactionState.resizingVisualElement = false;
        interactionState.resizingMultiSelection = false;
        interactionState.rotatingVisualElement = false;
        interactionState.rotatingMultiSelection = false;
        interactionState.resizeLastSnappedW = 0;
        interactionState.resizeLastSnappedH = 0;
        interactionState.resizeAnchorScreenX = 0.0;
        interactionState.resizeAnchorScreenY = 0.0;
        interactionState.multiDragStartPositions.clear();
        interactionState.multiResizeStartBounds.clear();
        interactionState.multiRotateStartRotations.clear();
    }

    public static boolean handleVisualTextEditorDrag(
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        @Nullable MultiLineEditBox visualTextEditor
    ) {
        if (isAnyVisualTransformActive(interactionState)) return false;
        if (visualState.editingTextElement < 0 || visualTextEditor == null || !visualTextEditor.visible) return false;
        if (!visualTextEditor.isMouseOver(mouseX, mouseY)) return false;
        return visualTextEditor.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public static boolean handleActiveVisualTransformsDrag(
        double mouseX,
        double mouseY,
        boolean overviewVisible,
        boolean visualTab,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int visualGridSize,
        boolean visualGridSnap,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        boolean altDown,
        boolean shiftDown,
        Consumer<VisualElement> clampElement,
        MoveSelectedElement moveSelectedElement,
        ResizeSelectedElement resizeSelectedElement,
        LocalOffsetToScreen elementLocalOffsetToScreen,
        int descriptionX,
        int descriptionY,
        Consumer<VisualElement> clampElementPositionToCanvas,
        Consumer<VisualElement> positionVisualTextEditor,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        if (!(overviewVisible && visualTab && visualState.selectedVisualElement >= 0)) return false;
        if (VisualInteractionController.applyActiveMultiSelectionTransforms(
            mouseX,
            mouseY,
            visualElements,
            visualState,
            interactionState,
            visualGridSize,
            visualGridSnap,
            screenToCanvasX,
            screenToCanvasY,
            altDown,
            shiftDown,
            clampElement
        )) return true;
        if (interactionState.draggingVisualElement) {
            int mouseCanvasX = screenToCanvasX.applyAsInt((int) mouseX);
            int mouseCanvasY = screenToCanvasY.applyAsInt((int) mouseY);
            boolean changed = VisualInteractionController.applySingleDrag(
                mouseCanvasX,
                mouseCanvasY,
                interactionState.dragStartMouseCanvasX,
                interactionState.dragStartMouseCanvasY,
                interactionState.dragElementStartX,
                interactionState.dragElementStartY,
                moveSelectedElement::move
            );
            if (changed) {
                interactionState.pendingVisualSync = true;
            }
            return true;
        }
        if (interactionState.resizingVisualElement) {
            VisualElement element = visualElements.get(visualState.selectedVisualElement);
            int[] resizeTargets = VisualInteractionController.computeResizeTargets(
                element,
                mouseX,
                mouseY,
                interactionState.resizeElementStartW,
                interactionState.resizeElementStartH,
                interactionState.resizeStartMouseX,
                interactionState.resizeStartMouseY,
                interactionState.resizeAnchorScreenX,
                interactionState.resizeAnchorScreenY
            );
            int targetW = resizeTargets[0];
            int targetH = resizeTargets[1];
            boolean rotatedResize = resizeTargets[2] == 1;
            boolean changed = resizeSelectedElement.resize(targetW, targetH);
            if (changed) {
                if (rotatedResize) {
                    double[] topLeftOffset = elementLocalOffsetToScreen.resolve(element.rotation, element.w, element.h, 0.0, 0.0);
                    double borderX = interactionState.resizeAnchorScreenX - topLeftOffset[0];
                    double borderY = interactionState.resizeAnchorScreenY - topLeftOffset[1];
                    int oldX = element.x;
                    int oldY = element.y;
                    element.x = (int) Math.round(borderX - descriptionX - 1 + visualState.visualScrollX);
                    element.y = (int) Math.round(borderY - descriptionY - 1 + visualState.visualScrollY);
                    clampElementPositionToCanvas.accept(element);
                    if ((element.x != oldX || element.y != oldY) && visualState.editingTextElement == visualState.selectedVisualElement) {
                        positionVisualTextEditor.accept(element);
                    }
                }
                interactionState.pendingVisualSync = true;
            }
            return true;
        }
        if (interactionState.rotatingVisualElement
            && visualState.selectedVisualElement >= 0
            && visualState.selectedVisualElement < visualElements.size()) {
            VisualElement element = visualElements.get(visualState.selectedVisualElement);
            int centerX = canvasToScreenX.applyAsInt(element.x + element.w / 2);
            int centerY = canvasToScreenY.applyAsInt(element.y + element.h / 2);
            boolean changed = VisualInteractionController.applyRotationFromMouse(element, mouseX, mouseY, centerX, centerY, shiftDown);
            interactionState.pendingVisualSync = interactionState.pendingVisualSync || changed;
            return true;
        }
        return false;
    }

    public static boolean handleSelectionBoxDrag(
        double mouseX,
        double mouseY,
        int button,
        boolean overviewVisible,
        boolean visualTab,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int descriptionX,
        int descriptionY
    ) {
        if (!(overviewVisible && visualTab && interactionState.selectingVisualElements && button == 0)) {
            return false;
        }
        interactionState.selectBoxEndX = (int) mouseX;
        interactionState.selectBoxEndY = (int) mouseY;
        VisualInteractionController.applySelectionBoxToSelectionState(
            visualElements,
            multiSelectedVisualElements,
            visualState,
            interactionState,
            descriptionX,
            descriptionY
        );
        return true;
    }

    public static void releaseVisualTextEditor(
        double mouseX,
        double mouseY,
        int button,
        VisualEditorState visualState,
        @Nullable MultiLineEditBox visualTextEditor
    ) {
        if (visualState.editingTextElement >= 0 && visualTextEditor != null && visualTextEditor.visible) {
            visualTextEditor.mouseReleased(mouseX, mouseY, button);
        }
    }

    public static boolean handleSelectionBoxRelease(
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int descriptionX,
        int descriptionY
    ) {
        if (!interactionState.selectingVisualElements) return false;
        interactionState.selectingVisualElements = false;
        VisualInteractionController.applySelectionBoxToSelectionState(
            visualElements,
            multiSelectedVisualElements,
            visualState,
            interactionState,
            descriptionX,
            descriptionY
        );
        return true;
    }

    public static boolean handleVisualTransformRelease(
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Runnable onSyncRawFromVisual
    ) {
        if (!isAnyVisualTransformActive(interactionState)) return false;
        resetVisualTransformState(interactionState);
        VisualInteractionController.clearAlignmentGuides(visualState);
        if (interactionState.pendingVisualSync) {
            onSyncRawFromVisual.run();
        }
        return true;
    }
}
