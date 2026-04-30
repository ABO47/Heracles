package earth.terrarium.heracles.client.screens.quest.visual.geometry;

import earth.terrarium.heracles.client.screens.quest.visual.model.VisualEditorState;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import net.minecraft.util.Mth;

import java.util.List;

public final class VisualCanvasMetrics {
    public static final int FIXED_CANVAS_WIDTH = 8192;
    public static final int MAX_CANVAS_HEIGHT = 1_000_000;

    private VisualCanvasMetrics() {
    }

    public static int getCanvasWidth(int descriptionWidth, List<VisualElement> elements, int gridSize) {
        return FIXED_CANVAS_WIDTH;
    }

    public static int getCanvasHeight(int descriptionHeight, List<VisualElement> elements, int gridSize) {
        int max = Math.max(0, descriptionHeight);
        int extra = Math.max(32, gridSize);
        for (VisualElement element : elements) {
            max = Math.max(max, element.y + element.h + extra);
        }
        return Math.min(MAX_CANVAS_HEIGHT, snapUpToGrid(max, gridSize));
    }

    public static void clampVisualScroll(
        VisualEditorState visualState,
        int descriptionWidth,
        int descriptionHeight,
        int gridSize,
        List<VisualElement> elements
    ) {
        int canvasWidth = getCanvasWidth(descriptionWidth, elements, gridSize);
        int canvasHeight = getCanvasHeight(descriptionHeight, elements, gridSize);
        visualState.visualScrollX = Mth.clamp(visualState.visualScrollX, 0, Math.max(0, canvasWidth - descriptionWidth));
        visualState.visualScrollY = Mth.clamp(visualState.visualScrollY, 0, Math.max(0, canvasHeight - descriptionHeight));
    }

    private static int snapUpToGrid(int value, int gridSize) {
        int step = Math.max(1, gridSize);
        if (value <= 0) return step;
        return ((value + step - 1) / step) * step;
    }

}
