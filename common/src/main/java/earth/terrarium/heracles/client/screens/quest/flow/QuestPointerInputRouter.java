package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualTransformOrchestrator;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;

public final class QuestPointerInputRouter {
    @FunctionalInterface
    public interface MoveElementFn {
        boolean move(int targetX, int targetY);
    }

    @FunctionalInterface
    public interface ResizeElementFn {
        boolean resize(int targetW, int targetH);
    }

    @FunctionalInterface
    public interface ClampElementFn {
        void clamp(VisualElement element);
    }

    @FunctionalInterface
    public interface ClampElementPositionFn {
        void clamp(VisualElement element);
    }

    @FunctionalInterface
    public interface PositionTextEditorFn {
        void position(VisualElement element);
    }

    @FunctionalInterface
    public interface ElementOffsetToScreenFn {
        double[] resolve(int rotation, int baseW, int baseH, double localX, double localY);
    }

    private QuestPointerInputRouter() {
    }

    public static boolean handleMouseDragged(
        double mouseX,
        double mouseY,
        int button,
        double dragX,
        double dragY,
        boolean overviewVisible,
        boolean visualTab,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        MultiLineEditBox visualTextEditor,
        int visualGridSize,
        boolean visualGridSnap,
        int descriptionX,
        int descriptionY,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        MoveElementFn moveElement,
        ResizeElementFn resizeElement,
        ClampElementFn clampElement,
        ClampElementPositionFn clampElementPositionToCanvas,
        PositionTextEditorFn positionVisualTextEditor,
        ElementOffsetToScreenFn elementLocalOffsetToScreen,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        if (VisualTransformOrchestrator.handleVisualTextEditorDrag(
            mouseX,
            mouseY,
            button,
            dragX,
            dragY,
            visualState,
            interactionState,
            visualTextEditor
        )) return true;

        if (button == 0 && VisualTransformOrchestrator.handleActiveVisualTransformsDrag(
            mouseX,
            mouseY,
            overviewVisible,
            visualTab,
            visualElements,
            visualState,
            interactionState,
            visualGridSize,
            visualGridSnap,
            screenToCanvasX,
            screenToCanvasY,
            Screen.hasAltDown(),
            Screen.hasShiftDown(),
            clampElement::clamp,
            moveElement::move,
            resizeElement::resize,
            elementLocalOffsetToScreen::resolve,
            descriptionX,
            descriptionY,
            clampElementPositionToCanvas::clamp,
            positionVisualTextEditor::position,
            canvasToScreenX,
            canvasToScreenY
        )) return true;

        return VisualTransformOrchestrator.handleSelectionBoxDrag(
            mouseX,
            mouseY,
            button,
            overviewVisible,
            visualTab,
            visualElements,
            multiSelectedVisualElements,
            visualState,
            interactionState,
            descriptionX,
            descriptionY
        );
    }

    public static boolean handleMouseReleased(
        double mouseX,
        double mouseY,
        int button,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        MultiLineEditBox visualTextEditor,
        int descriptionX,
        int descriptionY,
        Runnable syncRawFromVisual
    ) {
        VisualTransformOrchestrator.releaseVisualTextEditor(mouseX, mouseY, button, visualState, visualTextEditor);
        if (VisualTransformOrchestrator.handleSelectionBoxRelease(
            visualElements,
            multiSelectedVisualElements,
            visualState,
            interactionState,
            descriptionX,
            descriptionY
        )) return true;
        return VisualTransformOrchestrator.handleVisualTransformRelease(visualState, interactionState, syncRawFromVisual);
    }

}
