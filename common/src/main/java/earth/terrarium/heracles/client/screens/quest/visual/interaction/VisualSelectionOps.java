package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class VisualSelectionOps {
    private final List<VisualElement> visualElements;
    private final Set<Integer> multiSelectedVisualElements;
    private final VisualEditorState visualState;
    private final VisualInteractionState interactionState;
    private final Supplier<VisualElement> clipboardGetter;
    private final Consumer<VisualElement> clipboardSetter;
    private final IntSupplier contextVisualX;
    private final IntSupplier contextVisualY;
    private final Consumer<VisualElement> clampElement;
    private final IntConsumer beforeRemoveIndex;
    private final Runnable onSyncRequired;

    public VisualSelectionOps(
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Supplier<VisualElement> clipboardGetter,
        Consumer<VisualElement> clipboardSetter,
        IntSupplier contextVisualX,
        IntSupplier contextVisualY,
        Consumer<VisualElement> clampElement,
        IntConsumer beforeRemoveIndex,
        Runnable onSyncRequired
    ) {
        this.visualElements = visualElements;
        this.multiSelectedVisualElements = multiSelectedVisualElements;
        this.visualState = visualState;
        this.interactionState = interactionState;
        this.clipboardGetter = clipboardGetter;
        this.clipboardSetter = clipboardSetter;
        this.contextVisualX = contextVisualX;
        this.contextVisualY = contextVisualY;
        this.clampElement = clampElement;
        this.beforeRemoveIndex = beforeRemoveIndex;
        this.onSyncRequired = onSyncRequired;
    }

    public boolean hasClipboardElement() {
        return this.clipboardGetter.get() != null;
    }

    public void copySelectedVisualElement() {
        this.clipboardSetter.accept(VisualElementActions.copySelectedOrNull(this.visualElements, this.visualState));
    }

    public void duplicateSelectedVisualElement() {
        boolean changed = VisualElementActions.duplicateSelected(
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.clampElement
        );
        if (changed) this.onSyncRequired.run();
    }

    public void bringSelectedToFront() {
        boolean changed = VisualElementActions.bringSelectedToFront(this.visualElements, this.multiSelectedVisualElements, this.visualState);
        if (changed) this.onSyncRequired.run();
    }

    public void sendSelectedToBack() {
        boolean changed = VisualElementActions.sendSelectedToBack(this.visualElements, this.multiSelectedVisualElements, this.visualState);
        if (changed) this.onSyncRequired.run();
    }

    public void removeSelectedVisualElement() {
        boolean changed = VisualElementActions.removeSelected(
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.beforeRemoveIndex
        );
        if (changed) this.onSyncRequired.run();
    }

    public void pasteVisualElementAtContext() {
        boolean changed = VisualElementActions.pasteFromClipboard(
            this.clipboardGetter.get(),
            this.contextVisualX.getAsInt(),
            this.contextVisualY.getAsInt(),
            this.visualElements,
            this.multiSelectedVisualElements,
            this.visualState,
            this.clampElement
        );
        if (changed) this.onSyncRequired.run();
    }

    public void setSelectedOpacity(int opacity) {
        boolean changed = VisualElementActions.setSelectedOpacity(opacity, this.visualElements, this.multiSelectedVisualElements, this.visualState);
        if (changed) this.onSyncRequired.run();
    }

    public void setSelectedEntityVariant(String variantId) {
        boolean changed = VisualElementActions.setSelectedEntityVariant(this.visualElements, this.visualState, variantId);
        if (changed) {
            this.interactionState.pendingVisualSync = true;
            this.onSyncRequired.run();
        }
    }
}
