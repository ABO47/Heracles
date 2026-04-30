package earth.terrarium.heracles.client.screens.quest.visual.interaction;

import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualCanvasMetrics;
import earth.terrarium.heracles.client.screens.quest.visual.geometry.VisualGeometry;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElementType;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class VisualCanvasController {
    private final List<VisualElement> visualElements;
    private final VisualEditorState visualState;
    private final VisualInteractionState interactionState;
    private final Supplier<Integer> descriptionX;
    private final Supplier<Integer> descriptionY;
    private final Supplier<Integer> descriptionWidth;
    private final Supplier<Integer> descriptionHeight;
    private final Supplier<Integer> visualGridSize;
    private final Supplier<Boolean> visualGridSnap;
    private final Supplier<Boolean> guideSnapEnabled;
    private final Supplier<Boolean> centerSnapEnabled;
    private final Supplier<Integer> guideSnapDistance;
    private final Consumer<VisualElement> onTextEditorReposition;
    private final Runnable onSyncRequired;

    public VisualCanvasController(
        List<VisualElement> visualElements,
        VisualEditorState visualState,
        VisualInteractionState interactionState,
        Supplier<Integer> descriptionX,
        Supplier<Integer> descriptionY,
        Supplier<Integer> descriptionWidth,
        Supplier<Integer> descriptionHeight,
        Supplier<Integer> visualGridSize,
        Supplier<Boolean> visualGridSnap,
        Supplier<Boolean> guideSnapEnabled,
        Supplier<Boolean> centerSnapEnabled,
        Supplier<Integer> guideSnapDistance,
        Consumer<VisualElement> onTextEditorReposition,
        Runnable onSyncRequired
    ) {
        this.visualElements = visualElements;
        this.visualState = visualState;
        this.interactionState = interactionState;
        this.descriptionX = descriptionX;
        this.descriptionY = descriptionY;
        this.descriptionWidth = descriptionWidth;
        this.descriptionHeight = descriptionHeight;
        this.visualGridSize = visualGridSize;
        this.visualGridSnap = visualGridSnap;
        this.guideSnapEnabled = guideSnapEnabled;
        this.centerSnapEnabled = centerSnapEnabled;
        this.guideSnapDistance = guideSnapDistance;
        this.onTextEditorReposition = onTextEditorReposition;
        this.onSyncRequired = onSyncRequired;
    }

    public void clampElement(VisualElement element) {
        clampElement(element, this.visualGridSnap.get(), this.visualGridSnap.get());
    }

    public void clampElementWithoutGridSnap(VisualElement element) {
        int minW = element.type == VisualElementType.ITEM ? 16 : 24;
        int minH = element.type == VisualElementType.ITEM ? 16 : 14;
        element.w = Math.max(minW, element.w);
        element.h = Math.max(minH, element.h);
        clampElementPositionToCanvas(element);
        clampVisualScroll();
    }

    public void clampElement(VisualElement element, boolean snapPositionX, boolean snapPositionY) {
        int gs = Math.max(1, this.visualGridSize.get());
        int minW = element.type == VisualElementType.ITEM ? 16 : 24;
        int minH = element.type == VisualElementType.ITEM ? 16 : 14;
        element.w = Math.max(minW, element.w);
        element.h = Math.max(minH, element.h);
        if (snapPositionX) element.x = Math.round((float) element.x / gs) * gs;
        if (snapPositionY) element.y = Math.round((float) element.y / gs) * gs;
        if (this.visualGridSnap.get()) {
            element.w = Math.max(minW, Math.round((float) element.w / gs) * gs);
            element.h = Math.max(minH, Math.round((float) element.h / gs) * gs);
        }
        clampElementPositionToCanvas(element);
        clampVisualScroll();
    }

    public void clampElementPositionToCanvas(VisualElement element) {
        int[] bounds = getElementPositionBounds(element);
        element.x = Mth.clamp(element.x, bounds[0], bounds[1]);
        element.y = Mth.clamp(element.y, bounds[2], bounds[3]);
    }

    public int[] getElementPositionBounds(VisualElement element) {
        int minX = -Math.max(1, element.w) + 1;
        int minY = -Math.max(1, element.h) + 1;
        int maxX = VisualCanvasMetrics.FIXED_CANVAS_WIDTH - 1;
        int maxY = VisualCanvasMetrics.MAX_CANVAS_HEIGHT - 1;
        return new int[]{minX, maxX, minY, maxY};
    }

    public boolean moveElement(int index, int targetX, int targetY, boolean syncImmediately, boolean applyAlignmentGuides) {
        return VisualInteractionController.moveElement(
            this.visualElements,
            this.visualState,
            this.interactionState,
            index,
            targetX,
            targetY,
            this.visualGridSize.get(),
            this.visualGridSnap.get(),
            this.guideSnapEnabled.get(),
            this.centerSnapEnabled.get(),
            this.guideSnapDistance.get(),
            this.descriptionWidth.get(),
            this.descriptionHeight.get(),
            Screen.hasAltDown(),
            syncImmediately,
            applyAlignmentGuides,
            this::clampElement,
            this.onTextEditorReposition,
            this.onSyncRequired,
            this::clampVisualScroll
        );
    }

    public int screenToCanvasX(double mouseX) {
        return (int) Math.floor(mouseX) - this.descriptionX.get() + this.visualState.visualScrollX;
    }

    public int screenToCanvasY(double mouseY) {
        return (int) Math.floor(mouseY) - this.descriptionY.get() - 1 + this.visualState.visualScrollY;
    }

    public int canvasToScreenX(int canvasX) {
        return this.descriptionX.get() + canvasX - this.visualState.visualScrollX + 1;
    }

    public int canvasToScreenY(int canvasY) {
        return this.descriptionY.get() + canvasY - this.visualState.visualScrollY + 1;
    }

    public void clampVisualScroll() {
        VisualCanvasMetrics.clampVisualScroll(
            this.visualState,
            this.descriptionWidth.get(),
            this.descriptionHeight.get(),
            this.visualGridSize.get(),
            this.visualElements
        );
    }

    public boolean resizeElement(int index, int targetW, int targetH, boolean syncImmediately) {
        return VisualInteractionController.resizeElement(
            this.visualElements,
            this.visualState,
            this.interactionState,
            index,
            targetW,
            targetH,
            this.visualGridSize.get(),
            this.visualGridSnap.get(),
            Screen.hasShiftDown(),
            Screen.hasAltDown(),
            syncImmediately,
            this.onTextEditorReposition,
            this.onSyncRequired,
            this::clampVisualScroll
        );
    }

    public double[] elementLocalOffsetToScreen(int rotation, int baseW, int baseH, double localX, double localY) {
        int borderW = Math.max(1, baseW - 1);
        int borderH = Math.max(1, baseH - 1);
        double localDX = localX - borderW / 2.0;
        double localDY = localY - borderH / 2.0;
        double[] screenDelta = VisualGeometry.elementLocalDeltaToScreenDelta(rotation, localDX, localDY);
        return new double[]{borderW / 2.0 + screenDelta[0], borderH / 2.0 + screenDelta[1]};
    }

    public double[] elementLocalToScreen(VisualElement element, double localX, double localY, int baseW, int baseH) {
        int borderX = canvasToScreenX(element.x);
        int borderY = canvasToScreenY(element.y);
        double[] offset = elementLocalOffsetToScreen(element.rotation, baseW, baseH, localX, localY);
        return new double[]{borderX + offset[0], borderY + offset[1]};
    }
}
