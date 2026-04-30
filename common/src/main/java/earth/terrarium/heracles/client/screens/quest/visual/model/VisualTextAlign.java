package earth.terrarium.heracles.client.screens.quest.visual.model;

public enum VisualTextAlign {
    LEFT,
    CENTER,
    RIGHT;

    public static VisualTextAlign byName(String raw) {
        if (raw == null) return LEFT;
        return switch (raw.trim().toLowerCase()) {
            case "center" -> CENTER;
            case "right" -> RIGHT;
            default -> LEFT;
        };
    }
}
