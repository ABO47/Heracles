package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualActionController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualCanvasController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInsertionController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualSelectionOps;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.modals.EditorTextContextMenu;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntConsumer;

public final class QuestVisualMenuLauncher {
    private final List<VisualElement> visualElements;
    private final Set<Integer> multiSelectedVisualElements;
    private final VisualEditorState visualState;
    private final VisualInteractionState interactionState;
    private final VisualCanvasController canvasController;
    private final VisualSelectionOps selectionOps;
    private final VisualActionController visualActions;
    private final Runnable addTextElementAtContext;
    private final IntConsumer startVisualTextEditing;
    private final Function<EditorTextContextMenu, EditorTextContextMenu> addTemporary;

    public QuestVisualMenuLauncher(
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        VisualCanvasController canvasController,
        VisualSelectionOps selectionOps,
        VisualActionController visualActions,
        Runnable addTextElementAtContext,
        IntConsumer startVisualTextEditing,
        Function<EditorTextContextMenu, EditorTextContextMenu> addTemporary
    ) {
        this.visualElements = visualElements;
        this.multiSelectedVisualElements = multiSelectedVisualElements;
        this.visualState = visualState;
        this.interactionState = interactionState;
        this.canvasController = canvasController;
        this.selectionOps = selectionOps;
        this.visualActions = visualActions;
        this.addTextElementAtContext = addTextElementAtContext;
        this.startVisualTextEditing = startVisualTextEditing;
        this.addTemporary = addTemporary;
    }

    public EditorTextContextMenu openVisualContextMenu(EditorTextContextMenu currentMenu, int mouseX, int mouseY) {
        return QuestContextMenuRouter.openVisualContextMenu(
            currentMenu,
            mouseX,
            mouseY,
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.interactionState,
            x -> this.canvasController.screenToCanvasX((double) x),
            y -> this.canvasController.screenToCanvasY((double) y),
            this.canvasController::canvasToScreenX,
            this.canvasController::canvasToScreenY,
            this.selectionOps.hasClipboardElement(),
            this.visualActions::openVisualBackgroundPicker,
            this.visualActions::removeVisualBackground,
            this.addTextElementAtContext,
            this.visualActions::openInlineItemInserter,
            this.visualActions::openInlineImageInserter,
            this.visualActions::openInlineEntityInserter,
            this.selectionOps::copySelectedVisualElement,
            this.selectionOps::duplicateSelectedVisualElement,
            () -> this.startVisualTextEditing.accept(this.visualState.selectedVisualElement),
            () -> this.visualActions.openInlineInserter(VisualInsertionController.InserterType.ITEM, true),
            () -> this.visualActions.openInlineInserter(VisualInsertionController.InserterType.IMAGE, true),
            () -> this.visualActions.openInlineInserter(VisualInsertionController.InserterType.ENTITY, true),
            this.visualActions::openEntityVariantPicker,
            this.visualActions::openSelectedEntityMotionModal,
            this.visualActions::openSelectedOpacityModal,
            this.selectionOps::bringSelectedToFront,
            this.selectionOps::sendSelectedToBack,
            this.selectionOps::removeSelectedVisualElement,
            this.selectionOps::pasteVisualElementAtContext,
            this.addTemporary
        );
    }

    public EditorTextContextMenu openRawDescriptionContextMenu(
        EditorTextContextMenu currentMenu,
        int mouseX,
        int mouseY,
        MultiLineEditBox descriptionBox,
        Runnable onPasteSync
    ) {
        return QuestContextMenuRouter.openDescriptionContextMenu(
            currentMenu,
            mouseX,
            mouseY,
            descriptionBox,
            onPasteSync,
            this.visualActions::openInlineItemInserter,
            this.visualActions::openInlineImageInserter,
            this.visualActions::openInlineEntityInserter,
            this.addTemporary
        );
    }
}
