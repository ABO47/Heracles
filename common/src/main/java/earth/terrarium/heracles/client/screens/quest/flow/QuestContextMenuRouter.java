package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualContextMenuController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.modals.EditorTextContextMenu;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;

public final class QuestContextMenuRouter {
    private QuestContextMenuRouter() {
    }

    public static EditorTextContextMenu openDescriptionContextMenu(
        EditorTextContextMenu currentMenu,
        int mouseX,
        int mouseY,
        MultiLineEditBox descriptionBox,
        Runnable onPasteSync,
        Runnable openInlineItemInserter,
        Runnable openInlineImageInserter,
        Runnable openInlineEntityInserter,
        Function<EditorTextContextMenu, EditorTextContextMenu> addTemporary
    ) {
        if (currentMenu != null) {
            currentMenu.setVisible(false);
        }
        EditorTextContextMenu menu = VisualContextMenuController.createDescriptionContextMenu(
            descriptionBox,
            mouseX,
            mouseY,
            onPasteSync,
            openInlineItemInserter,
            openInlineImageInserter,
            openInlineEntityInserter
        );
        return menu == null ? null : addTemporary.apply(menu);
    }

    public static EditorTextContextMenu openVisualContextMenu(
        EditorTextContextMenu currentMenu,
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
        Runnable pasteVisualElementAtContext,
        Function<EditorTextContextMenu, EditorTextContextMenu> addTemporary
    ) {
        if (currentMenu != null) {
            currentMenu.setVisible(false);
        }
        EditorTextContextMenu menu = VisualContextMenuController.createVisualContextMenu(
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
            hasClipboardElement,
            openVisualBackgroundPicker,
            removeVisualBackground,
            addTextElementAtContext,
            openInlineItemInserter,
            openInlineImageInserter,
            openInlineEntityInserter,
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
        return addTemporary.apply(menu);
    }
}
