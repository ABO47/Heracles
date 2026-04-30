package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualClickOrchestrator;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;

import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;

public final class QuestClickRouter {
    @FunctionalInterface
    public interface MouseClickHandler {
        boolean handle(double mouseX, double mouseY, int button);
    }

    @FunctionalInterface
    public interface MousePointHandler {
        boolean handle(double mouseX, double mouseY);
    }

    @FunctionalInterface
    public interface MousePointAction {
        void run(double mouseX, double mouseY);
    }

    @FunctionalInterface
    public interface MouseOverFn {
        boolean test(double mouseX, double mouseY);
    }

    private QuestClickRouter() {
    }

    public static boolean handleVisualCanvasMouseClick(
        double mouseX,
        double mouseY,
        int button,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        VisualInteractionController.ElementLocalToScreen elementLocalToScreen,
        MouseClickHandler tryHandleVisualTextEditorMouseClick,
        MousePointHandler handleInlineToolbarClick,
        Runnable finishVisualTextEditing,
        MouseOverFn isVisualTextEditorMouseOver,
        IntConsumer startVisualTextEditing,
        MousePointAction openVisualContextMenu
    ) {
        if (button == 1) {
            finishVisualTextEditing.run();
            openVisualContextMenu.run(mouseX, mouseY);
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (tryHandleVisualTextEditorMouseClick.handle(mouseX, mouseY, button)) {
            return true;
        }
        if (handleInlineToolbarClick.handle(mouseX, mouseY)) {
            return true;
        }

        int hit = VisualClickOrchestrator.findTopVisualElement(
            mouseX,
            mouseY,
            visualElements,
            screenToCanvasX,
            screenToCanvasY,
            canvasToScreenX,
            canvasToScreenY
        );
        VisualClickOrchestrator.finishVisualEditingIfClickedAway(
            mouseX,
            mouseY,
            hit,
            visualState,
            visualState.editingTextElement >= 0,
            isVisualTextEditorMouseOver.test(mouseX, mouseY),
            finishVisualTextEditing
        );
        if (VisualInteractionController.tryHandleVisualDoubleClick(hit, interactionState, startVisualTextEditing::accept)) {
            return true;
        }
        return VisualClickOrchestrator.handleVisualSelectionMouseClick(
            mouseX,
            mouseY,
            hit,
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
    }
}
