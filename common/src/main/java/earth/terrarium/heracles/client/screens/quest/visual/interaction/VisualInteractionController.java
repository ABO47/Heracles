package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualGeometry;
import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualCanvasMetrics;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;

public final class VisualInteractionController {
    @FunctionalInterface
    public interface MoveAttempt {
        boolean move(int targetX, int targetY);
    }

    @FunctionalInterface
    public interface ClampWithSnap {
        void clamp(VisualElement element, boolean snapX, boolean snapY);
    }

    @FunctionalInterface
    public interface ElementLocalToScreen {
        double[] toScreen(VisualElement element, double localX, double localY, int baseW, int baseH);
    }

    private VisualInteractionController() {
    }

    public static <T> boolean hasBlockingModal(List<T> temporaryWidgets, T descriptionContextMenu, Predicate<T> isVisible) {
        for (T widget : temporaryWidgets) {
            if (!isVisible.test(widget)) continue;
            if (widget == descriptionContextMenu) continue;
            return true;
        }
        return false;
    }

    public static int[] getMultiSelectionBounds(Set<Integer> selectedIndexes, List<VisualElement> elements) {
        if (selectedIndexes.size() < 2) return null;
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int index : selectedIndexes) {
            if (index < 0 || index >= elements.size()) continue;
            VisualElement element = elements.get(index);
            int[] bounds = elementBoundsAabb(element);
            minX = Math.min(minX, bounds[0]);
            minY = Math.min(minY, bounds[1]);
            maxX = Math.max(maxX, bounds[2]);
            maxY = Math.max(maxY, bounds[3]);
        }
        if (minX == Integer.MAX_VALUE) return null;
        return new int[]{minX, minY, maxX, maxY};
    }

    public static void captureMultiResizeStartBounds(Set<Integer> selectedIndexes, List<VisualElement> elements, Map<Integer, int[]> outStartBounds) {
        outStartBounds.clear();
        for (int index : selectedIndexes) {
            if (index < 0 || index >= elements.size()) continue;
            VisualElement element = elements.get(index);
            outStartBounds.put(index, new int[]{element.x, element.y, element.w, element.h});
        }
    }

    public static void captureMultiDragStartPositions(Set<Integer> selectedIndexes, List<VisualElement> elements, Map<Integer, int[]> outStartPositions) {
        outStartPositions.clear();
        for (int index : selectedIndexes) {
            if (index < 0 || index >= elements.size()) continue;
            VisualElement element = elements.get(index);
            outStartPositions.put(index, new int[]{element.x, element.y});
        }
    }

    public static boolean applyMultiDrag(List<VisualElement> elements, Map<Integer, int[]> startPositions, int deltaX, int deltaY, Consumer<VisualElement> clampElement) {
        if (startPositions.isEmpty()) return false;
        boolean changed = false;
        for (Map.Entry<Integer, int[]> entry : startPositions.entrySet()) {
            int index = entry.getKey();
            if (index < 0 || index >= elements.size()) continue;
            int[] start = entry.getValue();
            VisualElement element = elements.get(index);
            int oldX = element.x;
            int oldY = element.y;
            element.x = start[0] + deltaX;
            element.y = start[1] + deltaY;
            clampElement.accept(element);
            if (oldX != element.x || oldY != element.y) {
                changed = true;
            }
        }
        return changed;
    }

    public static boolean applyMultiResize(
        List<VisualElement> elements,
        Map<Integer, int[]> startBounds,
        int selectionStartX,
        int selectionStartY,
        int selectionStartW,
        int selectionStartH,
        int mouseX,
        int mouseY,
        int resizeStartMouseX,
        int resizeStartMouseY,
        int gridSize,
        boolean gridSnap,
        boolean altDown,
        boolean shiftDown,
        Consumer<VisualElement> clampElement
    ) {
        if (startBounds.isEmpty()) return false;
        int snappedGrid = Math.max(1, gridSize);
        int targetW = selectionStartW + (mouseX - resizeStartMouseX);
        int targetH = selectionStartH + (mouseY - resizeStartMouseY);
        if (gridSnap && !altDown) {
            targetW = Math.max(snappedGrid, Math.round((float) targetW / snappedGrid) * snappedGrid);
            targetH = Math.max(snappedGrid, Math.round((float) targetH / snappedGrid) * snappedGrid);
        } else {
            targetW = Math.max(1, targetW);
            targetH = Math.max(1, targetH);
        }

        float scaleX = targetW / (float) Math.max(1, selectionStartW);
        float scaleY = targetH / (float) Math.max(1, selectionStartH);
        if (shiftDown) {
            float uniform = Math.max(scaleX, scaleY);
            scaleX = uniform;
            scaleY = uniform;
        }

        boolean changed = false;
        for (Map.Entry<Integer, int[]> entry : startBounds.entrySet()) {
            int index = entry.getKey();
            if (index < 0 || index >= elements.size()) continue;
            int[] start = entry.getValue();
            VisualElement element = elements.get(index);
            int oldX = element.x;
            int oldY = element.y;
            int oldW = element.w;
            int oldH = element.h;
            element.x = selectionStartX + Math.round((start[0] - selectionStartX) * scaleX);
            element.y = selectionStartY + Math.round((start[1] - selectionStartY) * scaleY);
            element.w = Math.max(1, Math.round(start[2] * scaleX));
            element.h = Math.max(1, Math.round(start[3] * scaleY));
            clampElement.accept(element);
            if (oldX != element.x || oldY != element.y || oldW != element.w || oldH != element.h) {
                changed = true;
            }
        }
        return changed;
    }

    public static int[] toCanvasSelectionRect(int startScreenX, int startScreenY, int endScreenX, int endScreenY, int descriptionX, int descriptionY, int scrollX, int scrollY) {
        int left = Math.min(startScreenX, endScreenX) - descriptionX + scrollX;
        int top = Math.min(startScreenY, endScreenY) - descriptionY + scrollY;
        int right = Math.max(startScreenX, endScreenX) - descriptionX + scrollX;
        int bottom = Math.max(startScreenY, endScreenY) - descriptionY + scrollY;
        return new int[]{left, top, right, bottom};
    }

    public static int selectElementsInRect(List<VisualElement> elements, Set<Integer> outSelectedIndexes, int left, int top, int right, int bottom) {
        outSelectedIndexes.clear();
        for (int i = 0; i < elements.size(); i++) {
            VisualElement element = elements.get(i);
            int[] bounds = elementBoundsAabb(element);
            if (bounds[2] >= left && bounds[0] <= right && bounds[3] >= top && bounds[1] <= bottom) {
                outSelectedIndexes.add(i);
            }
        }
        if (outSelectedIndexes.isEmpty()) return -1;
        List<Integer> ordered = new ArrayList<>(outSelectedIndexes);
        ordered.sort(Collections.reverseOrder());
        return ordered.get(0);
    }

    public static void applySelectionBoxToSelectionState(
        List<VisualElement> elements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int descriptionX,
        int descriptionY
    ) {
        int[] selectionRect = toCanvasSelectionRect(
            interactionState.selectBoxStartX,
            interactionState.selectBoxStartY,
            interactionState.selectBoxEndX,
            interactionState.selectBoxEndY,
            descriptionX,
            descriptionY,
            visualState.visualScrollX,
            visualState.visualScrollY
        );
        visualState.selectedVisualElement = selectElementsInRect(
            elements,
            multiSelectedVisualElements,
            selectionRect[0],
            selectionRect[1],
            selectionRect[2],
            selectionRect[3]
        );
    }

    public static boolean isPointInBounds(double mouseX, double mouseY, int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX <= right && mouseY >= top && mouseY <= bottom;
    }

    public static boolean isOverBottomRightHandle(double mouseX, double mouseY, int right, int bottom, int handleSize) {
        int hx = right - handleSize;
        int hy = bottom - handleSize;
        return mouseX >= hx && mouseX < hx + handleSize && mouseY >= hy && mouseY < hy + handleSize;
    }

    public static boolean isInMultiSelectionBounds(
        double mouseX,
        double mouseY,
        int[] bounds,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        int left = canvasToScreenX.applyAsInt(bounds[0]);
        int top = canvasToScreenY.applyAsInt(bounds[1]);
        int right = canvasToScreenX.applyAsInt(bounds[2]);
        int bottom = canvasToScreenY.applyAsInt(bounds[3]);
        return isPointInBounds(mouseX, mouseY, left, top, right, bottom);
    }

    public static boolean isOverMultiResizeHandle(
        double mouseX,
        double mouseY,
        int[] bounds,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        int handleSize
    ) {
        int right = canvasToScreenX.applyAsInt(bounds[2]);
        int bottom = canvasToScreenY.applyAsInt(bounds[3]);
        return isOverBottomRightHandle(mouseX, mouseY, right, bottom, handleSize);
    }

    public static boolean isOverMultiRotateHandle(
        double mouseX,
        double mouseY,
        int[] bounds,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        int handleSize
    ) {
        int right = canvasToScreenX.applyAsInt(bounds[2]);
        int top = canvasToScreenY.applyAsInt(bounds[1]);
        int hx = right - handleSize;
        int hy = top;
        return mouseX >= hx && mouseX < hx + handleSize && mouseY >= hy && mouseY < hy + handleSize;
    }

    public static boolean tryStartMultiSelectionTransform(
        double mouseX,
        double mouseY,
        int hit,
        boolean ctrl,
        Set<Integer> multiSelectedVisualElements,
        List<VisualElement> visualElements,
        VisualInteractionState interactionState,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        int[] existingMultiBounds = getMultiSelectionBounds(multiSelectedVisualElements, visualElements);
        if (ctrl || existingMultiBounds == null || multiSelectedVisualElements.size() <= 1) return false;
        if (isOverMultiRotateHandle(mouseX, mouseY, existingMultiBounds, canvasToScreenX, canvasToScreenY, 8)) {
            interactionState.rotatingMultiSelection = true;
            interactionState.multiSelectionStartX = existingMultiBounds[0];
            interactionState.multiSelectionStartY = existingMultiBounds[1];
            interactionState.multiSelectionStartW = Math.max(1, existingMultiBounds[2] - existingMultiBounds[0]);
            interactionState.multiSelectionStartH = Math.max(1, existingMultiBounds[3] - existingMultiBounds[1]);
            interactionState.multiSelectionCenterX = interactionState.multiSelectionStartX + interactionState.multiSelectionStartW / 2;
            interactionState.multiSelectionCenterY = interactionState.multiSelectionStartY + interactionState.multiSelectionStartH / 2;
            int mouseCanvasX = screenToCanvasX.applyAsInt((int) mouseX);
            int mouseCanvasY = screenToCanvasY.applyAsInt((int) mouseY);
            interactionState.multiRotateStartAngle = Math.toDegrees(Math.atan2(
                mouseCanvasY - interactionState.multiSelectionCenterY,
                mouseCanvasX - interactionState.multiSelectionCenterX
            ));
            interactionState.multiResizeStartBounds.clear();
            interactionState.multiRotateStartRotations.clear();
            for (int index : multiSelectedVisualElements) {
                if (index < 0 || index >= visualElements.size()) continue;
                VisualElement element = visualElements.get(index);
                interactionState.multiResizeStartBounds.put(index, new int[]{element.x, element.y, element.w, element.h});
                interactionState.multiRotateStartRotations.put(index, element.rotation);
            }
            return true;
        }
        if (isOverMultiResizeHandle(mouseX, mouseY, existingMultiBounds, canvasToScreenX, canvasToScreenY, 8)) {
            interactionState.resizingMultiSelection = true;
            interactionState.resizeStartMouseX = (int) mouseX;
            interactionState.resizeStartMouseY = (int) mouseY;
            interactionState.multiSelectionStartX = existingMultiBounds[0];
            interactionState.multiSelectionStartY = existingMultiBounds[1];
            interactionState.multiSelectionStartW = Math.max(1, existingMultiBounds[2] - existingMultiBounds[0]);
            interactionState.multiSelectionStartH = Math.max(1, existingMultiBounds[3] - existingMultiBounds[1]);
            captureMultiResizeStartBounds(multiSelectedVisualElements, visualElements, interactionState.multiResizeStartBounds);
            return true;
        }
        if (isInMultiSelectionBounds(mouseX, mouseY, existingMultiBounds, canvasToScreenX, canvasToScreenY)) {
            interactionState.draggingMultiSelection = true;
            interactionState.dragStartMouseCanvasX = screenToCanvasX.applyAsInt((int) mouseX);
            interactionState.dragStartMouseCanvasY = screenToCanvasY.applyAsInt((int) mouseY);
            captureMultiDragStartPositions(multiSelectedVisualElements, visualElements, interactionState.multiDragStartPositions);
            return true;
        }
        return false;
    }

    public static boolean tryHandleVisualDoubleClick(int hit, VisualInteractionState interactionState, IntConsumer onDoubleClick) {
        long now = System.currentTimeMillis();
        if (hit >= 0 && hit == interactionState.lastClickedElement && now - interactionState.lastElementClickTime < 250L) {
            onDoubleClick.accept(hit);
            interactionState.lastElementClickTime = 0L;
            return true;
        }
        interactionState.lastClickedElement = hit;
        interactionState.lastElementClickTime = now;
        return false;
    }

    public static void startSelectionBox(VisualEditorState visualState, VisualInteractionState interactionState, double mouseX, double mouseY) {
        visualState.selectedVisualElement = -1;
        interactionState.selectingVisualElements = true;
        interactionState.selectBoxStartX = (int) mouseX;
        interactionState.selectBoxStartY = (int) mouseY;
        interactionState.selectBoxEndX = (int) mouseX;
        interactionState.selectBoxEndY = (int) mouseY;
    }

    public static void startSingleSelectionInteraction(
        int hit,
        double mouseX,
        double mouseY,
        List<VisualElement> visualElements,
        Set<Integer> multiSelectedVisualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY,
        ElementLocalToScreen elementLocalToScreen
    ) {
        VisualElement element = visualElements.get(hit);
        if (isOverRotateHandle(element, mouseX, mouseY, canvasToScreenX, canvasToScreenY)) {
            interactionState.rotatingVisualElement = true;
            return;
        }
        if (isOverResizeHandle(element, mouseX, mouseY, canvasToScreenX, canvasToScreenY)) {
            interactionState.resizingVisualElement = true;
            interactionState.resizeStartMouseX = (int) mouseX;
            interactionState.resizeStartMouseY = (int) mouseY;
            interactionState.resizeElementStartW = element.w;
            interactionState.resizeElementStartH = element.h;
            interactionState.resizeLastSnappedW = interactionState.resizeElementStartW;
            interactionState.resizeLastSnappedH = interactionState.resizeElementStartH;
            double[] anchor = elementLocalToScreen.toScreen(element, 0.0, 0.0, interactionState.resizeElementStartW, interactionState.resizeElementStartH);
            interactionState.resizeAnchorScreenX = anchor[0];
            interactionState.resizeAnchorScreenY = anchor[1];
            return;
        }

        interactionState.dragStartMouseCanvasX = screenToCanvasX.applyAsInt((int) mouseX);
        interactionState.dragStartMouseCanvasY = screenToCanvasY.applyAsInt((int) mouseY);
        interactionState.dragElementStartX = element.x;
        interactionState.dragElementStartY = element.y;
        if (multiSelectedVisualElements.size() > 1 && multiSelectedVisualElements.contains(hit)) {
            interactionState.draggingMultiSelection = true;
            captureMultiDragStartPositions(multiSelectedVisualElements, visualElements, interactionState.multiDragStartPositions);
            return;
        }
        interactionState.draggingVisualElement = true;
    }

    public static void clearAlignmentGuides(VisualEditorState visualState) {
        visualState.alignmentGuideX = -1;
        visualState.alignmentGuideY = -1;
    }

    public static int[] alignToNearbyElements(
        List<VisualElement> elements,
        VisualEditorState visualState,
        boolean guideSnapEnabled,
        boolean centerSnapEnabled,
        int gridSize,
        int guideSnapDistance,
        int descriptionWidth,
        int descriptionHeight,
        int movingIndex,
        int targetX,
        int targetY
    ) {
        if (!guideSnapEnabled) {
            clearAlignmentGuides(visualState);
            return new int[]{targetX, targetY};
        }
        if (movingIndex < 0 || movingIndex >= elements.size()) {
            clearAlignmentGuides(visualState);
            return new int[]{targetX, targetY};
        }

        VisualElement moving = elements.get(movingIndex);
        double[] movingXAnchors = new double[]{targetX, targetX + moving.w / 2.0, targetX + moving.w};
        double[] movingYAnchors = new double[]{targetY, targetY + moving.h / 2.0, targetY + moving.h};
        double threshold = Math.max(1, guideSnapDistance);

        double bestXDelta = Double.NaN;
        double bestYDelta = Double.NaN;
        double bestXGuide = Double.NaN;
        double bestYGuide = Double.NaN;

        for (int i = 0; i < elements.size(); i++) {
            if (i == movingIndex) continue;
            VisualElement other = elements.get(i);
            double[] otherXAnchors = new double[]{other.x, other.x + other.w / 2.0, other.x + other.w};
            double[] otherYAnchors = new double[]{other.y, other.y + other.h / 2.0, other.y + other.h};

            for (double movingAnchor : movingXAnchors) {
                for (double otherAnchor : otherXAnchors) {
                    double delta = otherAnchor - movingAnchor;
                    double abs = Math.abs(delta);
                    if (abs <= threshold && (Double.isNaN(bestXDelta) || abs < Math.abs(bestXDelta))) {
                        bestXDelta = delta;
                        bestXGuide = otherAnchor;
                    }
                }
            }

            for (double movingAnchor : movingYAnchors) {
                for (double otherAnchor : otherYAnchors) {
                    double delta = otherAnchor - movingAnchor;
                    double abs = Math.abs(delta);
                    if (abs <= threshold && (Double.isNaN(bestYDelta) || abs < Math.abs(bestYDelta))) {
                        bestYDelta = delta;
                        bestYGuide = otherAnchor;
                    }
                }
            }
        }

        if (centerSnapEnabled) {
            int step = Math.max(1, gridSize);
            double rawCenterX = visualState.visualScrollX + descriptionWidth / 2.0;
            double rawCenterY = visualState.visualScrollY + descriptionHeight / 2.0;
            double canvasCenterX = Math.round(rawCenterX / step) * (double) step;
            double canvasCenterY = Math.round(rawCenterY / step) * (double) step;
            for (double movingAnchor : movingXAnchors) {
                double delta = canvasCenterX - movingAnchor;
                double abs = Math.abs(delta);
                if (abs <= threshold && (Double.isNaN(bestXDelta) || abs < Math.abs(bestXDelta))) {
                    bestXDelta = delta;
                    bestXGuide = canvasCenterX;
                }
            }
            for (double movingAnchor : movingYAnchors) {
                double delta = canvasCenterY - movingAnchor;
                double abs = Math.abs(delta);
                if (abs <= threshold && (Double.isNaN(bestYDelta) || abs < Math.abs(bestYDelta))) {
                    bestYDelta = delta;
                    bestYGuide = canvasCenterY;
                }
            }
        }

        if (!Double.isNaN(bestXDelta)) {
            targetX = (int) Math.round(targetX + bestXDelta);
            visualState.alignmentGuideX = (int) Math.round(bestXGuide);
        } else {
            visualState.alignmentGuideX = -1;
        }
        if (!Double.isNaN(bestYDelta)) {
            targetY = (int) Math.round(targetY + bestYDelta);
            visualState.alignmentGuideY = (int) Math.round(bestYGuide);
        } else {
            visualState.alignmentGuideY = -1;
        }
        return new int[]{targetX, targetY};
    }

    public static boolean applySingleDrag(
        int mouseCanvasX,
        int mouseCanvasY,
        int dragStartMouseCanvasX,
        int dragStartMouseCanvasY,
        int dragElementStartX,
        int dragElementStartY,
        MoveAttempt moveAttempt
    ) {
        int targetX = dragElementStartX + (mouseCanvasX - dragStartMouseCanvasX);
        int targetY = dragElementStartY + (mouseCanvasY - dragStartMouseCanvasY);
        return moveAttempt.move(targetX, targetY);
    }

    public static boolean moveElement(
        List<VisualElement> elements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int index,
        int targetX,
        int targetY,
        int visualGridSize,
        boolean visualGridSnap,
        boolean guideSnapEnabled,
        boolean centerSnapEnabled,
        int guideSnapDistance,
        int descriptionWidth,
        int descriptionHeight,
        boolean altDown,
        boolean syncImmediately,
        boolean applyAlignmentGuides,
        ClampWithSnap clampElement,
        Consumer<VisualElement> onTextEditorReposition,
        Runnable onSyncRawFromVisual,
        Runnable onClampVisualScroll
    ) {
        if (index < 0 || index >= elements.size()) return false;
        VisualElement element = elements.get(index);
        int gs = Math.max(1, visualGridSize);
        boolean bypassSnap = altDown;
        if (applyAlignmentGuides && !bypassSnap) {
            int[] aligned = alignToNearbyElements(
                elements,
                visualState,
                guideSnapEnabled,
                centerSnapEnabled,
                visualGridSize,
                guideSnapDistance,
                descriptionWidth,
                descriptionHeight,
                index,
                targetX,
                targetY
            );
            targetX = aligned[0];
            targetY = aligned[1];
        } else {
            clearAlignmentGuides(visualState);
        }
        if (!bypassSnap && visualGridSnap) {
            if (visualState.alignmentGuideX < 0) {
                targetX = Math.round((float) targetX / gs) * gs;
            }
            if (visualState.alignmentGuideY < 0) {
                targetY = Math.round((float) targetY / gs) * gs;
            }
        }
        int oldX = element.x;
        int oldY = element.y;
        element.x = targetX;
        element.y = targetY;
        boolean snapX = !bypassSnap && visualGridSnap && visualState.alignmentGuideX < 0;
        boolean snapY = !bypassSnap && visualGridSnap && visualState.alignmentGuideY < 0;
        clampElement.clamp(element, snapX, snapY);
        if (oldX == element.x && oldY == element.y) return false;
        interactionState.pendingVisualSync = true;
        if (visualState.editingTextElement == index) {
            onTextEditorReposition.accept(element);
        }
        if (syncImmediately) {
            onSyncRawFromVisual.run();
        }
        onClampVisualScroll.run();
        return true;
    }

    public static boolean resizeElement(
        List<VisualElement> elements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int index,
        int targetW,
        int targetH,
        int visualGridSize,
        boolean visualGridSnap,
        boolean shiftDown,
        boolean altDown,
        boolean syncImmediately,
        Consumer<VisualElement> onTextEditorReposition,
        Runnable onSyncRawFromVisual,
        Runnable onClampVisualScroll
    ) {
        if (index < 0 || index >= elements.size()) return false;
        VisualElement element = elements.get(index);
        int gs = Math.max(1, visualGridSize);
        int minW = element.type == VisualElementType.ITEM ? 16 : 24;
        int minH = element.type == VisualElementType.ITEM ? 16 : 14;
        if (shiftDown && interactionState.resizeElementStartW > 0 && interactionState.resizeElementStartH > 0) {
            float ratio = interactionState.resizeElementStartW / (float) interactionState.resizeElementStartH;
            if (Math.abs(targetW - interactionState.resizeElementStartW) >= Math.abs(targetH - interactionState.resizeElementStartH)) {
                targetH = Math.max(minH, Math.round(targetW / Math.max(0.01f, ratio)));
            } else {
                targetW = Math.max(minW, Math.round(targetH * ratio));
            }
        }
        if (visualGridSnap && !altDown) {
            int snappedW = Math.round((float) targetW / gs) * gs;
            int snappedH = Math.round((float) targetH / gs) * gs;
            double halfGrid = gs / 2.0;
            if (interactionState.resizingVisualElement && index == visualState.selectedVisualElement && interactionState.resizeLastSnappedW > 0) {
                int baseW = interactionState.resizeLastSnappedW;
                if (snappedW != baseW && Math.abs(snappedW - baseW) < halfGrid) {
                    snappedW = baseW;
                } else if (snappedW != baseW) {
                    interactionState.resizeLastSnappedW = snappedW;
                }
            } else if (snappedW != element.w && Math.abs(snappedW - element.w) < halfGrid) {
                snappedW = element.w;
            }

            if (interactionState.resizingVisualElement && index == visualState.selectedVisualElement && interactionState.resizeLastSnappedH > 0) {
                int baseH = interactionState.resizeLastSnappedH;
                if (snappedH != baseH && Math.abs(snappedH - baseH) < halfGrid) {
                    snappedH = baseH;
                } else if (snappedH != baseH) {
                    interactionState.resizeLastSnappedH = snappedH;
                }
            } else if (snappedH != element.h && Math.abs(snappedH - element.h) < halfGrid) {
                snappedH = element.h;
            }

            targetW = snappedW;
            targetH = snappedH;
        }
        int oldW = element.w;
        int oldH = element.h;
        int maxW = Math.max(minW, VisualCanvasMetrics.FIXED_CANVAS_WIDTH - element.x);
        int maxH = Math.max(minH, VisualCanvasMetrics.MAX_CANVAS_HEIGHT - element.y);
        if (visualGridSnap && !altDown) {
            maxW = Math.max(minW, (maxW / gs) * gs);
            maxH = Math.max(minH, (maxH / gs) * gs);
            minW = Math.max(minW, ((minW + gs - 1) / gs) * gs);
            minH = Math.max(minH, ((minH + gs - 1) / gs) * gs);
        }
        element.w = Math.max(minW, Math.min(maxW, targetW));
        element.h = Math.max(minH, Math.min(maxH, targetH));
        if (oldW == element.w && oldH == element.h) return false;
        interactionState.pendingVisualSync = true;
        if (visualState.editingTextElement == index) {
            onTextEditorReposition.accept(element);
        }
        if (syncImmediately) {
            onSyncRawFromVisual.run();
        }
        onClampVisualScroll.run();
        return true;
    }

    public static int[] computeResizeTargets(
        VisualElement element,
        double mouseX,
        double mouseY,
        int resizeElementStartW,
        int resizeElementStartH,
        int resizeStartMouseX,
        int resizeStartMouseY,
        double resizeAnchorScreenX,
        double resizeAnchorScreenY
    ) {
        boolean rotatedResize = isRotated(element);
        int targetW;
        int targetH;
        if (rotatedResize) {
            double anchorToMouseX = mouseX - resizeAnchorScreenX;
            double anchorToMouseY = mouseY - resizeAnchorScreenY;
            double[] localSize = VisualGeometry.screenDeltaToElementLocalDelta(element.rotation, anchorToMouseX, anchorToMouseY);
            targetW = (int) Math.round(localSize[0]) + 1;
            targetH = (int) Math.round(localSize[1]) + 1;
        } else {
            targetW = resizeElementStartW + ((int) mouseX - resizeStartMouseX);
            targetH = resizeElementStartH + ((int) mouseY - resizeStartMouseY);
        }
        return new int[]{targetW, targetH, rotatedResize ? 1 : 0};
    }

    public static boolean applyRotationFromMouse(VisualElement element, double mouseX, double mouseY, int centerScreenX, int centerScreenY, boolean snapTo45) {
        double dx = mouseX - centerScreenX;
        double dy = mouseY - centerScreenY;
        int angle = (int) Math.round(Math.toDegrees(Math.atan2(dy, dx)));
        angle = (angle + 360) % 360;
        if (snapTo45) {
            angle = Math.round(angle / 45.0f) * 45;
        }
        if (element.rotation == angle) return false;
        element.rotation = angle;
        return true;
    }

    public static boolean applyActiveMultiSelectionTransforms(
        double mouseX,
        double mouseY,
        List<VisualElement> elements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        int visualGridSize,
        boolean visualGridSnap,
        IntUnaryOperator screenToCanvasX,
        IntUnaryOperator screenToCanvasY,
        boolean altDown,
        boolean shiftDown,
        Consumer<VisualElement> clampElement
    ) {
        if (visualState.selectedVisualElement < 0) return false;
        if (interactionState.rotatingMultiSelection
            && !interactionState.multiResizeStartBounds.isEmpty()
            && !interactionState.multiRotateStartRotations.isEmpty()) {
            int mouseCanvasX = screenToCanvasX.applyAsInt((int) mouseX);
            int mouseCanvasY = screenToCanvasY.applyAsInt((int) mouseY);
            double currentAngle = Math.toDegrees(Math.atan2(
                mouseCanvasY - interactionState.multiSelectionCenterY,
                mouseCanvasX - interactionState.multiSelectionCenterX
            ));
            double delta = currentAngle - interactionState.multiRotateStartAngle;
            if (shiftDown) {
                delta = Math.round(delta / 45.0) * 45.0;
            }
            double radians = Math.toRadians(delta);
            double cos = Math.cos(radians);
            double sin = Math.sin(radians);
            boolean changed = false;
            for (Map.Entry<Integer, int[]> entry : interactionState.multiResizeStartBounds.entrySet()) {
                int index = entry.getKey();
                if (index < 0 || index >= elements.size()) continue;
                int[] start = entry.getValue();
                VisualElement element = elements.get(index);

                double startCx = start[0] + start[2] / 2.0;
                double startCy = start[1] + start[3] / 2.0;
                double relX = startCx - interactionState.multiSelectionCenterX;
                double relY = startCy - interactionState.multiSelectionCenterY;
                double rotatedCx = interactionState.multiSelectionCenterX + relX * cos - relY * sin;
                double rotatedCy = interactionState.multiSelectionCenterY + relX * sin + relY * cos;
                int newX = (int) Math.round(rotatedCx - start[2] / 2.0);
                int newY = (int) Math.round(rotatedCy - start[3] / 2.0);

                int oldX = element.x;
                int oldY = element.y;
                int oldRot = element.rotation;
                element.x = newX;
                element.y = newY;
                int baseRot = interactionState.multiRotateStartRotations.getOrDefault(index, element.rotation);
                int rotated = (int) Math.round(baseRot + delta) % 360;
                if (rotated < 0) rotated += 360;
                element.rotation = rotated;
                clampElement.accept(element);
                if (element.x != oldX || element.y != oldY || element.rotation != oldRot) {
                    changed = true;
                }
            }
            interactionState.pendingVisualSync = interactionState.pendingVisualSync || changed;
            return true;
        }
        if (interactionState.draggingMultiSelection && !interactionState.multiDragStartPositions.isEmpty()) {
            int dx = screenToCanvasX.applyAsInt((int) mouseX) - interactionState.dragStartMouseCanvasX;
            int dy = screenToCanvasY.applyAsInt((int) mouseY) - interactionState.dragStartMouseCanvasY;
            boolean changed = applyMultiDrag(elements, interactionState.multiDragStartPositions, dx, dy, clampElement);
            interactionState.pendingVisualSync = interactionState.pendingVisualSync || changed;
            return true;
        }
        if (interactionState.resizingMultiSelection && !interactionState.multiResizeStartBounds.isEmpty()) {
            boolean changed = applyMultiResize(
                elements,
                interactionState.multiResizeStartBounds,
                interactionState.multiSelectionStartX,
                interactionState.multiSelectionStartY,
                interactionState.multiSelectionStartW,
                interactionState.multiSelectionStartH,
                (int) mouseX,
                (int) mouseY,
                interactionState.resizeStartMouseX,
                interactionState.resizeStartMouseY,
                visualGridSize,
                visualGridSnap,
                altDown,
                shiftDown,
                clampElement
            );
            interactionState.pendingVisualSync = interactionState.pendingVisualSync || changed;
            return true;
        }
        return false;
    }

    private static boolean isRotated(VisualElement element) {
        return VisualGeometry.normalizeRotation(element.rotation) != 0;
    }

    private static int[] elementBoundsAabb(VisualElement element) {
        int w = Math.max(1, element.w);
        int h = Math.max(1, element.h);
        if (!isRotatable(element) || !isRotated(element)) {
            return new int[]{element.x, element.y, element.x + w, element.y + h};
        }

        double cx = element.x + (w / 2.0);
        double cy = element.y + (h / 2.0);
        double halfW = w / 2.0;
        double halfH = h / 2.0;

        double[][] corners = new double[][]{
            {-halfW, -halfH},
            {halfW, -halfH},
            {-halfW, halfH},
            {halfW, halfH}
        };

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (double[] corner : corners) {
            double[] screenDelta = VisualGeometry.elementLocalDeltaToScreenDelta(element.rotation, corner[0], corner[1]);
            double px = cx + screenDelta[0];
            double py = cy + screenDelta[1];
            minX = Math.min(minX, px);
            minY = Math.min(minY, py);
            maxX = Math.max(maxX, px);
            maxY = Math.max(maxY, py);
        }

        return new int[]{
            (int) Math.floor(minX),
            (int) Math.floor(minY),
            (int) Math.ceil(maxX),
            (int) Math.ceil(maxY)
        };
    }

    public static int findTopElement(
        List<VisualElement> elements,
        double mouseX,
        double mouseY,
        int localX,
        int localY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        for (int i = elements.size() - 1; i >= 0; i--) {
            VisualElement element = elements.get(i);
            if (isPointInsideElement(element, mouseX, mouseY, localX, localY, canvasToScreenX, canvasToScreenY)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isOverResizeHandle(VisualElement element, double mouseX, double mouseY, IntUnaryOperator canvasToScreenX, IntUnaryOperator canvasToScreenY) {
        if (isRotatable(element) && VisualGeometry.normalizeRotation(element.rotation) != 0) {
            int borderW = Math.max(1, element.w - 1);
            int borderH = Math.max(1, element.h - 1);
            return isOverRotatedHandle(element, mouseX, mouseY, borderW - 7, borderH - 7, 7, 7, canvasToScreenX, canvasToScreenY);
        }
        int left = canvasToScreenX.applyAsInt(element.x + element.w - 8);
        int top = canvasToScreenY.applyAsInt(element.y + element.h - 8);
        return mouseX >= left && mouseX < left + 8 && mouseY >= top && mouseY < top + 8;
    }

    public static boolean isOverRotateHandle(VisualElement element, double mouseX, double mouseY, IntUnaryOperator canvasToScreenX, IntUnaryOperator canvasToScreenY) {
        if (!isRotatable(element)) return false;
        if (VisualGeometry.normalizeRotation(element.rotation) != 0) {
            int borderW = Math.max(1, element.w - 1);
            return isOverRotatedHandle(element, mouseX, mouseY, borderW - 7, 0, 7, 7, canvasToScreenX, canvasToScreenY);
        }
        int left = canvasToScreenX.applyAsInt(element.x + element.w - 8);
        int top = canvasToScreenY.applyAsInt(element.y);
        return mouseX >= left && mouseX < left + 8 && mouseY >= top && mouseY < top + 8;
    }

    private static boolean isPointInsideElement(
        VisualElement element,
        double mouseX,
        double mouseY,
        int localX,
        int localY,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        if (element == null) return false;
        if (!isRotatable(element) || VisualGeometry.normalizeRotation(element.rotation) == 0) {
            return localX >= element.x && localX < element.x + element.w && localY >= element.y && localY < element.y + element.h;
        }
        double[] local = toElementLocalSpace(element, mouseX, mouseY, element.w, element.h, canvasToScreenX, canvasToScreenY);
        int borderW = Math.max(1, element.w - 1);
        int borderH = Math.max(1, element.h - 1);
        return local[0] >= 0 && local[0] < borderW && local[1] >= 0 && local[1] < borderH;
    }

    private static boolean isOverRotatedHandle(
        VisualElement element,
        double mouseX,
        double mouseY,
        int handleX,
        int handleY,
        int handleW,
        int handleH,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        double[] local = toElementLocalSpace(element, mouseX, mouseY, element.w, element.h, canvasToScreenX, canvasToScreenY);
        return local[0] >= handleX && local[0] < handleX + handleW && local[1] >= handleY && local[1] < handleY + handleH;
    }

    private static double[] toElementLocalSpace(
        VisualElement element,
        double mouseX,
        double mouseY,
        int baseW,
        int baseH,
        IntUnaryOperator canvasToScreenX,
        IntUnaryOperator canvasToScreenY
    ) {
        int borderX = canvasToScreenX.applyAsInt(element.x);
        int borderY = canvasToScreenY.applyAsInt(element.y);
        int borderW = Math.max(1, baseW - 1);
        int borderH = Math.max(1, baseH - 1);
        double cx = borderX + borderW / 2.0;
        double cy = borderY + borderH / 2.0;
        double dx = mouseX - cx;
        double dy = mouseY - cy;
        double[] localDelta = VisualGeometry.screenDeltaToElementLocalDelta(element.rotation, dx, dy);
        double localX = localDelta[0] + borderW / 2.0;
        double localY = localDelta[1] + borderH / 2.0;
        return new double[]{localX, localY};
    }

    private static boolean isRotatable(VisualElement element) {
        return element != null && (element.type == VisualElementType.TEXT
            || element.type == VisualElementType.ITEM
            || element.type == VisualElementType.IMAGE
            || element.type == VisualElementType.ENTITY);
    }
}
