package earth.terrarium.heracles.client.handlers;

public class PlaytestManager {
    private static final PlaytestManager INSTANCE = new PlaytestManager();

    private boolean preview = false;

    public static PlaytestManager getInstance() {
        return INSTANCE;
    }

    public boolean isPreviewEnabled() {
        return preview;
    }

    public void setPreviewEnabled(boolean enabled) {
        this.preview = enabled;
    }
}
