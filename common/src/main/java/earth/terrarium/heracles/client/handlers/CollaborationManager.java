package earth.terrarium.heracles.client.handlers;

public class CollaborationManager {
    private static final CollaborationManager INSTANCE = new CollaborationManager();

    public static CollaborationManager getInstance() {
        return INSTANCE;
    }

    private boolean enabled = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void joinSession(String sessionId) {
        // TODO: implement session joining/networking
    }

    public void leaveSession() {
        // TODO: implement session leaving
    }

    public void broadcastEdit(Object edit) {
        // TODO: send edit to server
    }

    public void receiveRemoteEdit(Object edit) {
        // TODO: apply remote edit to local UI
    }
}
