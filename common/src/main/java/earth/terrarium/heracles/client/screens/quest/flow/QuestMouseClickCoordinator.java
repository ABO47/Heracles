package earth.terrarium.heracles.client.screens.quest.flow;

import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualCanvasController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualContextMenuController;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualInteractionState;
import earth.terrarium.heracles.client.screens.quest.visual.interaction.VisualTextEditorLifecycle;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.widgets.editor.MultiLineEditBox;
import earth.terrarium.heracles.client.widgets.modals.EditorTextContextMenu;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class QuestMouseClickCoordinator {
    @FunctionalInterface
    public interface InDescriptionAreaFn {
        boolean test(double mouseX, double mouseY);
    }

    private final Supplier<EditorTextContextMenu> descriptionContextMenuGetter;
    private final Consumer<EditorTextContextMenu> descriptionContextMenuSetter;
    private final Supplier<Boolean> overviewVisible;
    private final Supplier<Boolean> visualTabActive;
    private final Supplier<Boolean> rawTabActive;
    private final InDescriptionAreaFn isInDescriptionArea;
    private final List<VisualElement> visualElements;
    private final Set<Integer> multiSelectedVisualElements;
    private final VisualEditorState visualState;
    private final VisualInteractionState interactionState;
    private final VisualCanvasController canvasController;
    private final Supplier<MultiLineEditBox> visualTextEditor;
    private final Consumer<GuiEventListener> setFocused;
    private final QuestClickRouter.MousePointHandler handleInlineToolbarClick;
    private final Runnable finishVisualTextEditing;
    private final IntConsumer startVisualTextEditing;
    private final QuestVisualMenuLauncher visualMenuLauncher;
    private final Supplier<MultiLineEditBox> descriptionBox;
    private final Runnable onRawPasteSync;

    public QuestMouseClickCoordinator(
        Supplier<EditorTextContextMenu> descriptionContextMenuGetter,
        Consumer<EditorTextContextMenu> descriptionContextMenuSetter,
        Supplier<Boolean> overviewVisible,
        Supplier<Boolean> visualTabActive,
        Supplier<Boolean> rawTabActive,
        InDescriptionAreaFn isInDescriptionArea,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        VisualCanvasController canvasController,
        Supplier<MultiLineEditBox> visualTextEditor,
        Consumer<GuiEventListener> setFocused,
        QuestClickRouter.MousePointHandler handleInlineToolbarClick,
        Runnable finishVisualTextEditing,
        IntConsumer startVisualTextEditing,
        QuestVisualMenuLauncher visualMenuLauncher,
        Supplier<MultiLineEditBox> descriptionBox,
        Runnable onRawPasteSync
    ) {
        this.descriptionContextMenuGetter = descriptionContextMenuGetter;
        this.descriptionContextMenuSetter = descriptionContextMenuSetter;
        this.overviewVisible = overviewVisible;
        this.visualTabActive = visualTabActive;
        this.rawTabActive = rawTabActive;
        this.isInDescriptionArea = isInDescriptionArea;
        this.visualElements = visualElements;
        this.multiSelectedVisualElements = multiSelectedVisualElements;
        this.visualState = visualState;
        this.interactionState = interactionState;
        this.canvasController = canvasController;
        this.visualTextEditor = visualTextEditor;
        this.setFocused = setFocused;
        this.handleInlineToolbarClick = handleInlineToolbarClick;
        this.finishVisualTextEditing = finishVisualTextEditing;
        this.startVisualTextEditing = startVisualTextEditing;
        this.visualMenuLauncher = visualMenuLauncher;
        this.descriptionBox = descriptionBox;
        this.onRawPasteSync = onRawPasteSync;
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        EditorTextContextMenu menu = this.descriptionContextMenuGetter.get();
        if (VisualContextMenuController.handleDescriptionContextMenuClick(
            menu,
            mouseX,
            mouseY,
            button,
            value -> value.setVisible(false)
        )) return true;

        if (!this.overviewVisible.get()) return false;

        if (this.visualTabActive.get() && this.isInDescriptionArea.test(mouseX, mouseY)) {
            return QuestClickRouter.handleVisualCanvasMouseClick(
                mouseX,
                mouseY,
                button,
                this.visualElements,
                this.multiSelectedVisualElements,
                this.visualState,
                this.interactionState,
                x -> this.canvasController.screenToCanvasX((double) x),
                y -> this.canvasController.screenToCanvasY((double) y),
                this.canvasController::canvasToScreenX,
                this.canvasController::canvasToScreenY,
                this.canvasController::elementLocalToScreen,
                (mx, my, clickButton) -> VisualTextEditorLifecycle.tryHandleVisualTextEditorMouseClick(
                    mx,
                    my,
                    clickButton,
                    this.visualTextEditor.get(),
                    this.visualState,
                    this.setFocused
                ),
                this.handleInlineToolbarClick,
                this.finishVisualTextEditing,
                (mx, my) -> {
                    MultiLineEditBox editor = this.visualTextEditor.get();
                    return editor != null && editor.visible && editor.isMouseOver(mx, my);
                },
                this.startVisualTextEditing,
                (mx, my) -> this.descriptionContextMenuSetter.accept(
                    this.visualMenuLauncher.openVisualContextMenu(
                        this.descriptionContextMenuGetter.get(),
                        (int) mx,
                        (int) my
                    )
                )
            );
        }

        if (this.rawTabActive.get() && button == 1 && this.isInDescriptionArea.test(mouseX, mouseY)) {
            this.descriptionContextMenuSetter.accept(
                this.visualMenuLauncher.openRawDescriptionContextMenu(
                    this.descriptionContextMenuGetter.get(),
                    (int) mouseX,
                    (int) mouseY,
                    this.descriptionBox.get(),
                    this.onRawPasteSync
                )
            );
            return true;
        }
        return false;
    }
}
