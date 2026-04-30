package earth.terrarium.heracles.client.screens.quest.visual.toolbar;

import java.util.Locale;

public final class InlineToolbarActions {
    private InlineToolbarActions() {
    }

    public static String toggleObfuscated(String value) {
        String safe = value == null ? "" : value;
        if (safe.length() >= 4 && safe.startsWith("||") && safe.endsWith("||")) {
            return safe.substring(2, safe.length() - 2);
        }
        return "||" + safe + "||";
    }

    public static String toggleBlockquote(String value) {
        String safe = value == null ? "" : value;
        if (safe.isEmpty()) return safe;
        String[] lines = safe.split("\n", -1);
        boolean allQuoted = true;
        for (String line : lines) {
            if (!line.startsWith("> ")) {
                allQuoted = false;
                break;
            }
        }
        for (int i = 0; i < lines.length; i++) {
            if (allQuoted) {
                if (lines[i].startsWith("> ")) lines[i] = lines[i].substring(2);
            } else {
                lines[i] = "> " + lines[i];
            }
        }
        return String.join("\n", lines);
    }

    public static String toggleCase(String value) {
        String safe = value == null ? "" : value;
        if (safe.isBlank()) return safe;
        boolean hasLower = false;
        for (int i = 0; i < safe.length(); i++) {
            if (Character.isLowerCase(safe.charAt(i))) {
                hasLower = true;
                break;
            }
        }
        return hasLower ? safe.toUpperCase(Locale.ROOT) : safe.toLowerCase(Locale.ROOT);
    }

}
