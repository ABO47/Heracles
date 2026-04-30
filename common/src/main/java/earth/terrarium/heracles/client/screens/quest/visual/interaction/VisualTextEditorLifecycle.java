package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualGeometry;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

public final class VisualTextEditorLifecycle {
    private VisualTextEditorLifecycle() {
    }

    public static @Nullable MultiLineEditBox startVisualTextEditing(
        int index,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        @Nullable MultiLineEditBox currentEditor,
        Font font,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        Function<MultiLineEditBox, MultiLineEditBox> addWidget,
        Consumer<MultiLineEditBox> removeWidget,
        Consumer<GuiEventListener> setFocused,
        int screenWidth,
        int screenHeight
    ) {
        if (index < 0 || index >= visualElements.size()) return currentEditor;
        VisualElement element = visualElements.get(index);
        if (element.type != VisualElementType.TEXT) return currentEditor;
        if (currentEditor != null) {
            removeWidget.accept(currentEditor);
            currentEditor = null;
        }
        visualState.editingTextElement = index;
        MultiLineEditBox editor = addWidget.apply(new MultiLineEditBox(
            font,
            canvasToScreenX.applyAsInt(element.x) + 2,
            canvasToScreenY.applyAsInt(element.y) + 2,
            Math.max(24, element.w - 4),
            Math.max(14, element.h - 4),
            Component::literal
        ));
        editor.visible = true;
        editor.setRenderTextContents(false);
        editor.setValue(element.text);
        setFocused.accept(editor);
        editor.setFocused(true);
        editor.seekCursorTo(editor.getValue().length());
        positionVisualTextEditor(editor, element, canvasToScreenX, canvasToScreenY, screenWidth, screenHeight);
        return editor;
    }

    public static @Nullable MultiLineEditBox finishVisualTextEditing(
        @Nullable MultiLineEditBox visualTextEditor,
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Consumer<MultiLineEditBox> removeWidget,
        Consumer<GuiEventListener> setFocused,
        boolean syncImmediately,
        Runnable onSyncRequired
    ) {
        if (visualTextEditor == null || visualState.editingTextElement < 0 || visualState.editingTextElement >= visualElements.size()) {
            visualState.editingTextElement = -1;
            if (visualTextEditor != null) {
                visualTextEditor.visible = false;
                visualTextEditor.setFocused(false);
                removeWidget.accept(visualTextEditor);
            }
            return null;
        }
        VisualElement element = visualElements.get(visualState.editingTextElement);
        if (element.type == VisualElementType.TEXT) {
            element.text = visualTextEditor.getValue();
            interactionState.pendingVisualSync = true;
        }
        visualTextEditor.visible = false;
        visualTextEditor.setFocused(false);
        removeWidget.accept(visualTextEditor);
        setFocused.accept(null);
        visualState.editingTextElement = -1;
        if (syncImmediately && interactionState.pendingVisualSync) {
            onSyncRequired.run();
        }
        return null;
    }

    public static void positionVisualTextEditor(
        @Nullable MultiLineEditBox visualTextEditor,
        VisualElement element,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        int screenWidth,
        int screenHeight
    ) {
        if (visualTextEditor == null) return;
        if (element.type == VisualElementType.TEXT && VisualGeometry.normalizeRotation(element.rotation) != 0) {
            visualTextEditor.setX(screenWidth + 1000);
            visualTextEditor.setY(screenHeight + 1000);
            visualTextEditor.setWidth(Math.max(24, element.w - 4));
            return;
        }
        visualTextEditor.setX(canvasToScreenX.applyAsInt(element.x) + 2);
        visualTextEditor.setY(canvasToScreenY.applyAsInt(element.y) + 2);
        visualTextEditor.setWidth(Math.max(24, element.w - 4));
    }

    public static boolean tryHandleVisualTextEditorMouseClick(
        double mouseX,
        double mouseY,
        int button,
        @Nullable MultiLineEditBox visualTextEditor,
        VisualEditorState visualState,
        Consumer<GuiEventListener> setFocused
    ) {
        if (visualState.editingTextElement < 0 || visualTextEditor == null || !visualTextEditor.visible) return false;
        if (!visualTextEditor.isMouseOver(mouseX, mouseY)) return false;
        setFocused.accept(visualTextEditor);
        visualTextEditor.setFocused(true);
        visualTextEditor.mouseClicked(mouseX, mouseY, button);
        return true;
    }
}
