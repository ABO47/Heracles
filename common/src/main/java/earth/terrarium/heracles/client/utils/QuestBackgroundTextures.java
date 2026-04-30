package earth.terrarium.heracles.client.utils;

import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.handlers.CustomImageManager;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

public final class QuestBackgroundTextures {

    private static final String CUSTOM_PREFIX = "custom_bg/";
    private static final ResourceLocation DEFAULT_BACKGROUND = new ResourceLocation(Heracles.MOD_ID, "textures/gui/quest_backgrounds/default.png");

    private QuestBackgroundTextures() {}

    public static ResourceLocation defaultBackground() {
        return DEFAULT_BACKGROUND;
    }

    public static ResourceLocation fromManagedPath(String managedPath) {
        if (managedPath == null || managedPath.isBlank()) {
            return DEFAULT_BACKGROUND;
        }
        String normalized = managedPath.trim().replace('\\', '/');
        return new ResourceLocation(Heracles.MOD_ID, CUSTOM_PREFIX + toHex(normalized));
    }

    public static boolean isCustomBackground(ResourceLocation location) {
        return location != null
            && Heracles.MOD_ID.equals(location.getNamespace())
            && location.getPath().startsWith(CUSTOM_PREFIX);
    }

    public static String toManagedPath(ResourceLocation location) {
        if (!isCustomBackground(location)) return "";
        String payload = location.getPath().substring(CUSTOM_PREFIX.length());
        if (payload.isBlank()) return "";
        try {
            return fromHex(payload);
        } catch (Exception ignored) {
            return "";
        }
    }

    public static ResourceLocation resolve(ResourceLocation background) {
        if (background == null) return DEFAULT_BACKGROUND;
        if (!isCustomBackground(background)) return background;
        String managedPath = toManagedPath(background);
        if (managedPath.isBlank()) return DEFAULT_BACKGROUND;
        ResourceLocation texture = CustomImageManager.getTexture(managedPath);
        return texture == null ? DEFAULT_BACKGROUND : texture;
    }

    private static String toHex(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b & 0xFF));
        }
        return builder.toString();
    }

    private static String fromHex(String hex) {
        if ((hex.length() & 1) != 0) {
            throw new IllegalArgumentException("Invalid hex length");
        }
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) {
                throw new IllegalArgumentException("Invalid hex data");
            }
            bytes[i] = (byte) ((hi << 4) + lo);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
