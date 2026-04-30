package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualTextAlign;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;

public final class VisualTextActions {
    private VisualTextActions() {
    }

    public static void toggleBold(VisualElement element) {
        element.bold = !element.bold;
    }

    public static void toggleItalic(VisualElement element) {
        element.italic = !element.italic;
    }

    public static void toggleUnderline(VisualElement element) {
        element.underline = !element.underline;
    }

    public static void toggleStrike(VisualElement element) {
        element.strike = !element.strike;
    }

    public static void toggleObfuscated(VisualElement element) {
        element.text = InlineToolbarActions.toggleObfuscated(element.text);
    }

    public static void toggleBlockquote(VisualElement element) {
        element.text = InlineToolbarActions.toggleBlockquote(element.text);
    }

    public static void setAlign(VisualElement element, VisualTextAlign align) {
        element.align = align == null ? VisualTextAlign.LEFT : align;
    }

    public static boolean applyWrapperToSelection(MultiLineEditBox editor, String prefix, String suffix) {
        if (editor == null || !editor.hasSelection()) return false;
        String selected = editor.getSelectedText();
        if (selected == null || selected.isEmpty()) return false;
        editor.insertTextAtCursor(prefix + selected + suffix);
        return true;
    }

    public static boolean applyQuoteToSelection(MultiLineEditBox editor) {
        if (editor == null || !editor.hasSelection()) return false;
        String selected = editor.getSelectedText();
        if (selected == null || selected.isEmpty()) return false;
        String quoted = "> " + selected.replace("\n", "\n> ");
        editor.insertTextAtCursor(quoted);
        return true;
    }
}
