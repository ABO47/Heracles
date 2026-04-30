package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import org.jetbrains.annotations.Nullable;

public final class VisualElementActions {
    private VisualElementActions() {
    }

    public static boolean removeSelected(
        List<VisualElement> elements,
        Set<Integer> multiSelected,
        VisualEditorState state,
        IntConsumer beforeRemove
    ) {
        if (multiSelected.size() > 1) {
            List<Integer> indexes = new ArrayList<>(multiSelected);
            indexes.sort(Collections.reverseOrder());
            for (int index : indexes) {
                if (index < 0 || index >= elements.size()) continue;
                beforeRemove.accept(index);
                elements.remove(index);
            }
            multiSelected.clear();
            state.selectedVisualElement = -1;
            return true;
        }
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return false;
        beforeRemove.accept(state.selectedVisualElement);
        elements.remove(state.selectedVisualElement);
        multiSelected.clear();
        state.selectedVisualElement = -1;
        return true;
    }

    public static boolean pasteFromClipboard(
        VisualElement clipboard,
        int contextX,
        int contextY,
        List<VisualElement> elements,
        Set<Integer> multiSelected,
        VisualEditorState state,
        Consumer<VisualElement> clampElement
    ) {
        if (clipboard == null) return false;
        VisualElement pasted = clipboard.copy();
        pasted.x = contextX;
        pasted.y = contextY;
        clampElement.accept(pasted);
        elements.add(pasted);
        state.selectedVisualElement = elements.size() - 1;
        multiSelected.clear();
        multiSelected.add(state.selectedVisualElement);
        return true;
    }

    public static boolean duplicateSelected(
        List<VisualElement> elements,
        Set<Integer> multiSelected,
        VisualEditorState state,
        Consumer<VisualElement> clampElement
    ) {
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return false;
        VisualElement source = elements.get(state.selectedVisualElement);
        VisualElement copy = source.copy();
        copy.x += 8;
        copy.y += 8;
        clampElement.accept(copy);
        elements.add(copy);
        state.selectedVisualElement = elements.size() - 1;
        multiSelected.clear();
        multiSelected.add(state.selectedVisualElement);
        return true;
    }

    public static boolean bringSelectedToFront(
        List<VisualElement> elements,
        Set<Integer> multiSelected,
        VisualEditorState state
    ) {
        if (multiSelected.size() > 1) {
            List<Integer> indexes = new ArrayList<>(multiSelected);
            indexes.removeIf(i -> i < 0 || i >= elements.size());
            indexes.sort(Integer::compareTo);
            if (indexes.isEmpty()) return false;
            List<VisualElement> moved = new ArrayList<>();
            for (int i = indexes.size() - 1; i >= 0; i--) {
                moved.add(0, elements.remove((int) indexes.get(i)));
            }
            int start = elements.size();
            elements.addAll(moved);
            multiSelected.clear();
            for (int i = 0; i < moved.size(); i++) multiSelected.add(start + i);
            state.selectedVisualElement = start + moved.size() - 1;
            return true;
        }
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return false;
        VisualElement element = elements.remove(state.selectedVisualElement);
        elements.add(element);
        state.selectedVisualElement = elements.size() - 1;
        multiSelected.clear();
        multiSelected.add(state.selectedVisualElement);
        return true;
    }

    public static boolean sendSelectedToBack(
        List<VisualElement> elements,
        Set<Integer> multiSelected,
        VisualEditorState state
    ) {
        if (multiSelected.size() > 1) {
            List<Integer> indexes = new ArrayList<>(multiSelected);
            indexes.removeIf(i -> i < 0 || i >= elements.size());
            indexes.sort(Integer::compareTo);
            if (indexes.isEmpty()) return false;
            List<VisualElement> moved = new ArrayList<>();
            for (int i = indexes.size() - 1; i >= 0; i--) {
                moved.add(0, elements.remove((int) indexes.get(i)));
            }
            elements.addAll(0, moved);
            multiSelected.clear();
            for (int i = 0; i < moved.size(); i++) multiSelected.add(i);
            state.selectedVisualElement = moved.size() - 1;
            return true;
        }
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return false;
        VisualElement element = elements.remove(state.selectedVisualElement);
        elements.add(0, element);
        state.selectedVisualElement = 0;
        multiSelected.clear();
        multiSelected.add(state.selectedVisualElement);
        return true;
    }

    public static boolean setSelectedOpacity(
        int opacity,
        List<VisualElement> elements,
        Set<Integer> multiSelected,
        VisualEditorState state
    ) {
        int clamped = Math.max(0, Math.min(100, opacity));
        if (!multiSelected.isEmpty()) {
            boolean changed = false;
            for (int index : multiSelected) {
                if (index < 0 || index >= elements.size()) continue;
                VisualElement element = elements.get(index);
                if (element.type == VisualElementType.TEXT || element.type == VisualElementType.IMAGE) {
                    element.opacity = clamped;
                    changed = true;
                }
            }
            return changed;
        }
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return false;
        VisualElement element = elements.get(state.selectedVisualElement);
        if (element.type != VisualElementType.TEXT && element.type != VisualElementType.IMAGE) return false;
        element.opacity = clamped;
        return true;
    }

    public static @Nullable VisualElement copySelectedOrNull(List<VisualElement> elements, VisualEditorState state) {
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return null;
        return elements.get(state.selectedVisualElement).copy();
    }

    public static boolean setSelectedEntityVariant(
        List<VisualElement> elements,
        VisualEditorState state,
        @Nullable String variantId
    ) {
        if (state.selectedVisualElement < 0 || state.selectedVisualElement >= elements.size()) return false;
        VisualElement element = elements.get(state.selectedVisualElement);
        if (element.type != VisualElementType.ENTITY) return false;
        element.entityVariant = variantId == null ? "" : variantId;
        return true;
    }
}
