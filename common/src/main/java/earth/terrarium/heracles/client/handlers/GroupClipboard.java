package earth.terrarium.heracles.client.handlers;

public class GroupClipboard {
    private static String copied = null;

    public static void copy(String group) {
        copied = group;
    }

    public static boolean hasCopied() {
        return copied != null && !copied.isEmpty();
    }

    public static String getCopied() {
        return copied;
    }

    public static void clear() {
        copied = null;
    }
}
