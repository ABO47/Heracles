package earth.terrarium.heracles.client.screens.quest.flow;

import com.mojang.blaze3d.platform.InputConstants;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualCanvasController;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;

import java.util.List;
import java.util.function.IntConsumer;

public final class QuestInputRouter {
    private QuestInputRouter() {
    }

    public static boolean handleKeyPressed(
        int keyCode,
        VisualEditorState visualState,
        MultiLineEditBox visualTextEditor,
        Runnable finishVisualTextEditing,
        Runnable saveDescription,
        boolean overviewVisible,
        boolean visualTab,
        List<VisualElement> visualElements,
        VisualCanvasController canvasController,
        Runnable removeSelectedVisualElement,
        IntConsumer startVisualTextEditing
    ) {
        if (visualState.editingTextElement >= 0 && visualTextEditor != null && visualTextEditor.visible) {
            if (Screen.hasControlDown() && (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER)) {
                finishVisualTextEditing.run();
                return true;
            }
        }
        if (Screen.hasControlDown() && keyCode == InputConstants.KEY_S) {
            saveDescription.run();
            return true;
        }
        if (!(overviewVisible && visualTab && visualState.selectedVisualElement >= 0 && visualState.editingTextElement < 0)) {
            return false;
        }
        int step = Screen.hasShiftDown() ? 1 : 8;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        if (keyCode == InputConstants.KEY_LEFT) return canvasController.moveElement(visualState.selectedVisualElement, element.x - step, element.y, true, false);
        if (keyCode == InputConstants.KEY_RIGHT) return canvasController.moveElement(visualState.selectedVisualElement, element.x + step, element.y, true, false);
        if (keyCode == InputConstants.KEY_UP) return canvasController.moveElement(visualState.selectedVisualElement, element.x, element.y - step, true, false);
        if (keyCode == InputConstants.KEY_DOWN) return canvasController.moveElement(visualState.selectedVisualElement, element.x, element.y + step, true, false);
        if (keyCode == InputConstants.KEY_DELETE || keyCode == InputConstants.KEY_BACKSPACE) {
            removeSelectedVisualElement.run();
            return true;
        }
        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
            startVisualTextEditing.accept(visualState.selectedVisualElement);
            return true;
        }
        return false;
    }
}
