package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualTextAlign;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public final class VisualTextEditingActions {
    private VisualTextEditingActions() {
    }

    public static boolean ensureSelectedTextElement(List<VisualElement> visualElements, VisualEditorState visualState) {
        return visualState.selectedVisualElement >= 0
            && visualState.selectedVisualElement < visualElements.size()
            && visualElements.get(visualState.selectedVisualElement).type == VisualElementType.TEXT;
    }

    public static void syncLiveTextEditorToElement(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        @Nullable MultiLineEditBox visualTextEditor
    ) {
        if (visualTextEditor == null) return;
        if (visualState.editingTextElement < 0 || visualState.editingTextElement >= visualElements.size()) return;
        VisualElement element = visualElements.get(visualState.editingTextElement);
        if (element.type == VisualElementType.TEXT) {
            element.text = visualTextEditor.getValue();
            interactionState.pendingVisualSync = true;
        }
    }

    public static boolean applySelectionWrapper(
        @Nullable MultiLineEditBox visualTextEditor,
        VisualEditorState visualState,
        List<VisualElement> visualElements,
        VisualInteractionState interactionState,
        @Nullable String prefix,
        @Nullable String suffix
    ) {
        if (visualTextEditor == null || visualState.editingTextElement != visualState.selectedVisualElement) {
            return false;
        }
        boolean applied = suffix != null
            ? VisualTextActions.applyWrapperToSelection(visualTextEditor, prefix, suffix)
            : VisualTextActions.applyQuoteToSelection(visualTextEditor);
        if (!applied) return false;
        syncLiveTextEditorToElement(visualElements, visualState, interactionState, visualTextEditor);
        return true;
    }

    public static boolean applyColorToVisualSelection(
        int rgb,
        @Nullable MultiLineEditBox visualTextEditor,
        VisualEditorState visualState,
        List<VisualElement> visualElements,
        VisualInteractionState interactionState
    ) {
        String hex = String.format("%06X", rgb & 0xFFFFFF);
        return applySelectionWrapper(visualTextEditor, visualState, visualElements, interactionState, "/#{" + hex + "}/", "/#/");
    }

    public static void applyTextToggle(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        @Nullable MultiLineEditBox visualTextEditor,
        Consumer<VisualElement> action,
        @Nullable Consumer<VisualElement> editorSync,
        Runnable onSyncRequired
    ) {
        if (!ensureSelectedTextElement(visualElements, visualState)) return;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        action.accept(element);
        if (editorSync != null
            && visualTextEditor != null
            && visualState.editingTextElement == visualState.selectedVisualElement) {
            editorSync.accept(element);
        }
        onSyncRequired.run();
    }

    public static void setSelectedTextAlign(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualTextAlign align,
        Runnable onSyncRequired
    ) {
        if (!ensureSelectedTextElement(visualElements, visualState)) return;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        VisualTextActions.setAlign(element, align);
        onSyncRequired.run();
    }

    public static void toggleCaseSelectionOrElement(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        @Nullable MultiLineEditBox visualTextEditor,
        VisualInteractionState interactionState,
        Runnable onSyncRequired
    ) {
        if (!ensureSelectedTextElement(visualElements, visualState)) return;
        if (visualTextEditor != null && visualState.editingTextElement == visualState.selectedVisualElement) {
            if (visualTextEditor.hasSelection()) {
                String selected = visualTextEditor.getSelectedText();
                if (selected != null && !selected.isBlank()) {
                    visualTextEditor.insertTextAtCursor(InlineToolbarActions.toggleCase(selected));
                }
            } else {
                visualTextEditor.setValue(InlineToolbarActions.toggleCase(visualTextEditor.getValue()));
            }
            syncLiveTextEditorToElement(visualElements, visualState, interactionState, visualTextEditor);
            onSyncRequired.run();
            return;
        }
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        element.text = InlineToolbarActions.toggleCase(element.text);
        onSyncRequired.run();
    }

    public static void setSelectedTextSize(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        int sizePercent,
        Runnable onSyncRequired
    ) {
        if (!ensureSelectedTextElement(visualElements, visualState)) return;
        VisualElement element = visualElements.get(visualState.selectedVisualElement);
        element.textSize = Math.max(50, Math.min(300, sizePercent));
        onSyncRequired.run();
    }
}
