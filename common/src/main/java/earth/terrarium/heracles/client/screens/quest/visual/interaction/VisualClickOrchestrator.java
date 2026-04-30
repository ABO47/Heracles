package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class VisualClickOrchestrator {
    private VisualClickOrchestrator() {
    }

    public static int findTopVisualElement(
        double mouseX,
        double mouseY,
        List<VisualElement> visualElements,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        return VisualInteractionController.findTopElement(
            visualElements,
            mouseX,
            mouseY,
            screenToCanvasX.applyAsInt((int) mouseX),
            screenToCanvasY.applyAsInt((int) mouseY),
            canvasToScreenX,
            canvasToScreenY
        );
    }

    public static void finishVisualEditingIfClickedAway(
        double mouseX,
        double mouseY,
        int hit,
        VisualEditorState visualState,
        boolean textEditorVisible,
        boolean textEditorMouseOver,
        Runnable finishVisualTextEditing
    ) {
        if (visualState.editingTextElement >= 0 && textEditorVisible && !textEditorMouseOver && hit != visualState.editingTextElement) {
            finishVisualTextEditing.run();
        }
    }

    public static boolean handleVisualSelectionMouseClick(
        double mouseX,
        double mouseY,
        int hit,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        VisualInteractionController.ElementLocalToScreen elementLocalToScreen
    ) {
        boolean ctrl = Screen.hasControlDown();
        if (VisualInteractionController.tryStartMultiSelectionTransform(
            mouseX,
            mouseY,
            hit,
            ctrl,
            multiSelectedVisualElements,
            visualElements,
            interactionState,
            screenToCanvasX,
            screenToCanvasY,
            canvasToScreenX,
            canvasToScreenY
        )) return true;
        if (!ctrl && hit >= 0 && multiSelectedVisualElements.size() > 1 && multiSelectedVisualElements.contains(hit)) {
            visualState.selectedVisualElement = hit;
            VisualInteractionController.startSingleSelectionInteraction(
                hit,
                mouseX,
                mouseY,
                visualElements,
                multiSelectedVisualElements,
                visualState,
                interactionState,
                screenToCanvasX,
                screenToCanvasY,
                canvasToScreenX,
                canvasToScreenY,
                elementLocalToScreen
            );
            return true;
        }
        if (ctrl && hit >= 0) {
            if (!multiSelectedVisualElements.add(hit)) {
                multiSelectedVisualElements.remove(hit);
            }
            visualState.selectedVisualElement = hit;
            return true;
        }
        if (!ctrl) {
            multiSelectedVisualElements.clear();
        }
        visualState.selectedVisualElement = hit;
        if (hit >= 0) {
            multiSelectedVisualElements.add(hit);
            VisualInteractionController.startSingleSelectionInteraction(
                hit,
                mouseX,
                mouseY,
                visualElements,
                multiSelectedVisualElements,
                visualState,
                interactionState,
                screenToCanvasX,
                screenToCanvasY,
                canvasToScreenX,
                canvasToScreenY,
                elementLocalToScreen
            );
        } else {
            VisualInteractionController.startSelectionBox(visualState, interactionState, mouseX, mouseY);
        }
        return true;
    }
}
