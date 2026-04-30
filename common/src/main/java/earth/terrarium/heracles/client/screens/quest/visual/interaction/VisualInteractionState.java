package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import java.util.HashMap;
import java.util.Map;

public final class VisualInteractionState {
    public boolean draggingVisualElement = false;
    public boolean resizingVisualElement = false;
    public boolean rotatingVisualElement = false;
    public boolean pendingVisualSync = false;

    public int dragStartMouseCanvasX = 0;
    public int dragStartMouseCanvasY = 0;
    public int dragElementStartX = 0;
    public int dragElementStartY = 0;

    public int resizeElementStartW = 0;
    public int resizeElementStartH = 0;
    public int resizeStartMouseX = 0;
    public int resizeStartMouseY = 0;
    public double resizeAnchorScreenX = 0.0;
    public double resizeAnchorScreenY = 0.0;
    public int resizeLastSnappedW = 0;
    public int resizeLastSnappedH = 0;

    public int contextVisualX = 8;
    public int contextVisualY = 8;
    public long lastElementClickTime = 0L;
    public int lastClickedElement = -1;

    public boolean selectingVisualElements = false;
    public int selectBoxStartX = 0;
    public int selectBoxStartY = 0;
    public int selectBoxEndX = 0;
    public int selectBoxEndY = 0;

    public boolean draggingMultiSelection = false;
    public boolean resizingMultiSelection = false;
    public boolean rotatingMultiSelection = false;
    public int multiDragStartMouseCanvasX = 0;
    public int multiDragStartMouseCanvasY = 0;
    public int multiResizeStartMouseX = 0;
    public int multiResizeStartMouseY = 0;
    public int multiSelectionStartX = 0;
    public int multiSelectionStartY = 0;
    public int multiSelectionStartW = 0;
    public int multiSelectionStartH = 0;
    public int multiSelectionCenterX = 0;
    public int multiSelectionCenterY = 0;
    public double multiRotateStartAngle = 0.0;
    public final Map<Integer, Integer> multiRotateStartRotations = new HashMap<>();
    public final Map<Integer, int[]> multiDragStartPositions = new HashMap<>();
    public final Map<Integer, int[]> multiResizeStartBounds = new HashMap<>();
}
