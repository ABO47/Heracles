package earth.terrarium.heracles.client.screens.quest.visual.geometry;

public final class VisualGeometry {
    private VisualGeometry() {
    }

    public static int normalizeRotation(int rotation) {
        return ((rotation % 360) + 360) % 360;
    }

    public static double[] screenDeltaToElementLocalDelta(int rotation, double screenDX, double screenDY) {
        double rad = Math.toRadians(-rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double localDX = screenDX * cos - screenDY * sin;
        double localDY = screenDX * sin + screenDY * cos;
        return new double[]{localDX, localDY};
    }

    public static double[] elementLocalDeltaToScreenDelta(int rotation, double localDX, double localDY) {
        double rad = Math.toRadians(rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double screenDX = localDX * cos - localDY * sin;
        double screenDY = localDX * sin + localDY * cos;
        return new double[]{screenDX, screenDY};
    }
}
