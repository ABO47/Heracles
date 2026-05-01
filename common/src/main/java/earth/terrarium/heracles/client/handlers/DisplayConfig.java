package earth.terrarium.heracles.client.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.teamresourceful.resourcefullib.common.lib.Constants;
import earth.terrarium.heracles.Heracles;
import net.minecraft.util.GsonHelper;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayConfig {

    private static final String DISPLAY_FILE = "heracles_options.json";
    private static final String HERACLES_DIR = "heracles";
    private static final String GROUPS_DIR = "groups";
    public static final int MIN_CANVAS_LIMIT_WIDTH = 1446;
    public static final int MIN_CANVAS_LIMIT_HEIGHT = 1037;

    private static Path lastPath;

    public static int pinnedIndex = 0;
    public static int maxEditorHistory = 100;
    public static boolean showTutorial = true;
    public static boolean gridEnabled = false;
    public static boolean gridLock = false;
    public static int gridSize = 16;
    public static int gridOpacity = 30;
    public static boolean questDescriptionGridEnabled = true;
    public static boolean questDescriptionGridSnap = false;
    public static int questDescriptionGridSize = 16;
    public static boolean questDescriptionGuideSnapEnabled = true;
    public static boolean questDescriptionCenterSnapEnabled = true;
    public static int questDescriptionGuideSnapDistance = 6;
    private static final Map<String, String> groupCanvasBackgrounds = new HashMap<>();
    private static final Map<String, String> groupCardTextures = new HashMap<>();
    private static final Map<String, Integer> groupBackgroundOpacity = new HashMap<>();
    private static final Map<String, Integer> groupCanvasLimitWidth = new HashMap<>();
    private static final Map<String, Integer> groupCanvasLimitHeight = new HashMap<>();
    private static final Map<String, List<CanvasSprite>> groupCanvasSprites = new HashMap<>();
    private static final Map<String, Boolean> groupMinimapCollapsed = new HashMap<>();
    private static final Map<String, Boolean> groupInspectorCollapsed = new HashMap<>();
    private static final Map<String, java.util.Set<String>> groupHiddenConnections = new HashMap<>();

    public record CanvasSprite(
        String id,
        String path,
        int x,
        int y,
        int width,
        int height,
        int opacity
    ) {
        public CanvasSprite {
            id = id == null ? "" : id.trim();
            path = path == null ? "" : path.trim();
            width = Math.max(1, width);
            height = Math.max(1, height);
            opacity = Math.max(0, Math.min(100, opacity));
        }

        public boolean isValid() {
            return !id.isBlank() && !path.isBlank();
        }

        public CanvasSprite withPosition(int x, int y) {
            return new CanvasSprite(this.id, this.path, x, y, this.width, this.height, this.opacity);
        }

        public CanvasSprite withSize(int width, int height) {
            return new CanvasSprite(this.id, this.path, this.x, this.y, width, height, this.opacity);
        }

        public CanvasSprite withOpacity(int opacity) {
            return new CanvasSprite(this.id, this.path, this.x, this.y, this.width, this.height, opacity);
        }
    }

    public static boolean isGridEnabled(String group) {
        return gridEnabled;
    }

    public static void setGridEnabled(String group, boolean enabled) {
        gridEnabled = enabled;
        save();
    }

    public static boolean isGridLocked(String group) {
        return gridLock;
    }

    public static void setGridLocked(String group, boolean locked) {
        gridLock = locked;
        save();
    }

    public static int getGridSize(String group) {
        return gridSize;
    }

    public static void setGridSize(String group, int size) {
        gridSize = Math.max(1, size);
        save();
    }

    public static int getGridOpacity(String group) {
        return gridOpacity;
    }

    public static void setGridOpacity(String group, int opacity) {
        gridOpacity = Math.max(0, Math.min(100, opacity));
        save();
    }

    public static String getCanvasBackground(String group) {
        if (group == null || group.isBlank()) return "";
        return groupCanvasBackgrounds.getOrDefault(group, "");
    }

    public static void setCanvasBackground(String group, String background) {
        if (group == null || group.isBlank()) return;
        String value = background == null ? "" : background.trim();
        if (value.isEmpty()) groupCanvasBackgrounds.remove(group);
        else groupCanvasBackgrounds.put(group, value);
        save();
    }

    public static String getGroupCardTexture(String group) {
        if (group == null || group.isBlank()) return "";
        return groupCardTextures.getOrDefault(group, "");
    }

    public static void setGroupCardTexture(String group, String texture) {
        if (group == null || group.isBlank()) return;
        String value = texture == null ? "" : texture.trim();
        if (value.isEmpty()) groupCardTextures.remove(group);
        else groupCardTextures.put(group, value);
        save();
    }

    public static int getBackgroundOpacity(String group) {
        if (group == null || group.isBlank()) return 100;
        return Math.max(0, Math.min(100, groupBackgroundOpacity.getOrDefault(group, 100)));
    }

    public static void setBackgroundOpacity(String group, int opacity) {
        if (group == null || group.isBlank()) return;
        groupBackgroundOpacity.put(group, Math.max(0, Math.min(100, opacity)));
        save();
    }

    public static int getCanvasLimitWidth(String group) {
        if (group == null || group.isBlank()) return 0;
        return Math.max(0, groupCanvasLimitWidth.getOrDefault(group, 0));
    }

    public static int getCanvasLimitHeight(String group) {
        if (group == null || group.isBlank()) return 0;
        return Math.max(0, groupCanvasLimitHeight.getOrDefault(group, 0));
    }

    public static boolean hasCanvasLimit(String group) {
        return getCanvasLimitWidth(group) > 0 && getCanvasLimitHeight(group) > 0;
    }

    public static void setCanvasLimit(String group, int width, int height) {
        if (group == null || group.isBlank()) return;
        int w = Math.max(0, width);
        int h = Math.max(0, height);
        if (w <= 0 || h <= 0) {
            groupCanvasLimitWidth.remove(group);
            groupCanvasLimitHeight.remove(group);
        } else {
            w = Math.max(MIN_CANVAS_LIMIT_WIDTH, w);
            h = Math.max(MIN_CANVAS_LIMIT_HEIGHT, h);
            groupCanvasLimitWidth.put(group, w);
            groupCanvasLimitHeight.put(group, h);
        }
        save();
    }

    public static List<CanvasSprite> getCanvasSprites(String group) {
        if (group == null || group.isBlank()) return List.of();
        List<CanvasSprite> sprites = groupCanvasSprites.get(group);
        if (sprites == null || sprites.isEmpty()) return List.of();
        return new ArrayList<>(sprites);
    }

    public static void setCanvasSprites(String group, List<CanvasSprite> sprites) {
        if (group == null || group.isBlank()) return;
        if (sprites == null || sprites.isEmpty()) {
            groupCanvasSprites.remove(group);
            save();
            return;
        }

        List<CanvasSprite> sanitized = new ArrayList<>();
        for (CanvasSprite sprite : sprites) {
            if (sprite == null) continue;
            CanvasSprite copy = new CanvasSprite(
                sprite.id(),
                sprite.path(),
                sprite.x(),
                sprite.y(),
                sprite.width(),
                sprite.height(),
                sprite.opacity()
            );
            if (copy.isValid()) sanitized.add(copy);
        }

        if (sanitized.isEmpty()) {
            groupCanvasSprites.remove(group);
        } else {
            groupCanvasSprites.put(group, sanitized);
        }
        save();
    }

    public static boolean isMinimapCollapsed(String group) {
        if (group == null || group.isBlank()) return false;
        return groupMinimapCollapsed.getOrDefault(group, false);
    }

    public static void setMinimapCollapsed(String group, boolean collapsed) {
        if (group == null || group.isBlank()) return;
        if (collapsed) {
            groupMinimapCollapsed.put(group, true);
        } else {
            groupMinimapCollapsed.remove(group);
        }
        save();
    }

    public static boolean isInspectorCollapsed(String group) {
        if (group == null || group.isBlank()) return false;
        return groupInspectorCollapsed.getOrDefault(group, false);
    }

    public static void setInspectorCollapsed(String group, boolean collapsed) {
        if (group == null || group.isBlank()) return;
        if (collapsed) {
            groupInspectorCollapsed.put(group, true);
        } else {
            groupInspectorCollapsed.remove(group);
        }
        save();
    }

    private static String connectionKey(String sourceId, String targetId) {
        return (sourceId == null ? "" : sourceId.trim()) + "->" + (targetId == null ? "" : targetId.trim());
    }

    public static boolean isConnectionHidden(String group, String sourceId, String targetId) {
        if (group == null || group.isBlank()) return false;
        java.util.Set<String> hidden = groupHiddenConnections.get(group);
        if (hidden == null || hidden.isEmpty()) return false;
        return hidden.contains(connectionKey(sourceId, targetId));
    }

    public static void setConnectionHidden(String group, String sourceId, String targetId, boolean hidden) {
        if (group == null || group.isBlank()) return;
        String key = connectionKey(sourceId, targetId);
        if (key.equals("->")) return;
        java.util.Set<String> set = groupHiddenConnections.computeIfAbsent(group, g -> new java.util.HashSet<>());
        if (hidden) {
            set.add(key);
        } else {
            set.remove(key);
            if (set.isEmpty()) {
                groupHiddenConnections.remove(group);
            }
        }
        save();
    }

    public static void renameGroupVisuals(String from, String to) {
        if (from == null || to == null || from.isBlank() || to.isBlank() || from.equals(to)) return;
        if (groupCanvasBackgrounds.containsKey(from)) {
            groupCanvasBackgrounds.put(to, groupCanvasBackgrounds.remove(from));
        }
        if (groupCardTextures.containsKey(from)) {
            groupCardTextures.put(to, groupCardTextures.remove(from));
        }
        if (groupBackgroundOpacity.containsKey(from)) {
            groupBackgroundOpacity.put(to, groupBackgroundOpacity.remove(from));
        }
        if (groupCanvasLimitWidth.containsKey(from)) {
            groupCanvasLimitWidth.put(to, groupCanvasLimitWidth.remove(from));
        }
        if (groupCanvasLimitHeight.containsKey(from)) {
            groupCanvasLimitHeight.put(to, groupCanvasLimitHeight.remove(from));
        }
        if (groupCanvasSprites.containsKey(from)) {
            groupCanvasSprites.put(to, new ArrayList<>(groupCanvasSprites.remove(from)));
        }
        if (groupMinimapCollapsed.containsKey(from)) {
            groupMinimapCollapsed.put(to, groupMinimapCollapsed.remove(from));
        }
        if (groupInspectorCollapsed.containsKey(from)) {
            groupInspectorCollapsed.put(to, groupInspectorCollapsed.remove(from));
        }
        if (groupHiddenConnections.containsKey(from)) {
            groupHiddenConnections.put(to, new java.util.HashSet<>(groupHiddenConnections.remove(from)));
        }
        save();
    }

    public static void removeGroupVisuals(String group) {
        if (group == null || group.isBlank()) return;
        groupCanvasBackgrounds.remove(group);
        groupCardTextures.remove(group);
        groupBackgroundOpacity.remove(group);
        groupCanvasLimitWidth.remove(group);
        groupCanvasLimitHeight.remove(group);
        groupCanvasSprites.remove(group);
        groupMinimapCollapsed.remove(group);
        groupInspectorCollapsed.remove(group);
        groupHiddenConnections.remove(group);
        save();
    }

    public static void load(Path path) {
        DisplayConfig.lastPath = resolveDisplayDirectory(path);
        File displayFile = lastPath.resolve(DISPLAY_FILE).toFile();
        File legacyDisplayFile = path.resolve(DISPLAY_FILE).toFile();
        try {
            Files.createDirectories(lastPath);
            if (!displayFile.exists() && legacyDisplayFile.exists()) {
                try {
                    FileUtils.copyFile(legacyDisplayFile, displayFile);
                    Files.deleteIfExists(legacyDisplayFile.toPath());
                    Heracles.LOGGER.info("Migrated {} to {}", DISPLAY_FILE, displayFile.getAbsolutePath());
                } catch (Exception e) {
                    Heracles.LOGGER.warn("Failed to migrate legacy {} from {}", DISPLAY_FILE, legacyDisplayFile.getAbsolutePath(), e);
                }
            }
            if (displayFile.exists()) {
                String displayString = FileUtils.readFileToString(displayFile, StandardCharsets.UTF_8);
                JsonObject displayObject = Constants.PRETTY_GSON.fromJson(displayString, JsonObject.class);
                pinnedIndex = GsonHelper.getAsInt(displayObject, "pinnedIndex", 0);
                showTutorial = GsonHelper.getAsBoolean(displayObject, "showTutorial", true);
                maxEditorHistory = GsonHelper.getAsInt(displayObject, "maxEditorHistory", 100);
                gridEnabled = GsonHelper.getAsBoolean(displayObject, "gridEnabled", false);
                gridLock = GsonHelper.getAsBoolean(displayObject, "gridLock", false);
                gridSize = GsonHelper.getAsInt(displayObject, "gridSize", 16);
                gridOpacity = GsonHelper.getAsInt(displayObject, "gridOpacity", 30);
                questDescriptionGridEnabled = GsonHelper.getAsBoolean(displayObject, "questDescriptionGridEnabled", true);
                questDescriptionGridSnap = GsonHelper.getAsBoolean(displayObject, "questDescriptionGridSnap", false);
                questDescriptionGridSize = Math.max(4, GsonHelper.getAsInt(displayObject, "questDescriptionGridSize", 16));
                questDescriptionGuideSnapEnabled = GsonHelper.getAsBoolean(displayObject, "questDescriptionGuideSnapEnabled", true);
                questDescriptionCenterSnapEnabled = GsonHelper.getAsBoolean(displayObject, "questDescriptionCenterSnapEnabled", true);
                questDescriptionGuideSnapDistance = Math.max(1, GsonHelper.getAsInt(displayObject, "questDescriptionGuideSnapDistance", 6));
                
                groupCanvasBackgrounds.clear();
                groupCardTextures.clear();
                groupBackgroundOpacity.clear();
                groupCanvasLimitWidth.clear();
                groupCanvasLimitHeight.clear();
                groupCanvasSprites.clear();
                groupMinimapCollapsed.clear();
                groupInspectorCollapsed.clear();
                groupHiddenConnections.clear();

                if (displayObject.has("groupCanvasBackgrounds")) {
                    JsonObject backgrounds = displayObject.getAsJsonObject("groupCanvasBackgrounds");
                    for (var entry : backgrounds.entrySet()) {
                        try {
                            String key = entry.getKey();
                            String value = backgrounds.get(key).getAsString();
                            if (!key.isBlank() && value != null && !value.isBlank()) {
                                groupCanvasBackgrounds.put(key, value);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupCardTextures")) {
                    JsonObject textures = displayObject.getAsJsonObject("groupCardTextures");
                    for (var entry : textures.entrySet()) {
                        try {
                            String key = entry.getKey();
                            String value = textures.get(key).getAsString();
                            if (!key.isBlank() && value != null && !value.isBlank()) {
                                groupCardTextures.put(key, value);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupBackgroundOpacity")) {
                    JsonObject opacities = displayObject.getAsJsonObject("groupBackgroundOpacity");
                    for (var entry : opacities.entrySet()) {
                        try {
                            String key = entry.getKey();
                            int value = opacities.get(key).getAsInt();
                            if (!key.isBlank()) {
                                groupBackgroundOpacity.put(key, Math.max(0, Math.min(100, value)));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupCanvasLimits")) {
                    JsonObject limits = displayObject.getAsJsonObject("groupCanvasLimits");
                    for (var entry : limits.entrySet()) {
                        try {
                            String key = entry.getKey();
                            if (key == null || key.isBlank()) continue;
                            JsonObject value = limits.getAsJsonObject(key);
                            int width = Math.max(0, GsonHelper.getAsInt(value, "width", 0));
                            int height = Math.max(0, GsonHelper.getAsInt(value, "height", 0));
                            if (width > 0 && height > 0) {
                                groupCanvasLimitWidth.put(key, Math.max(MIN_CANVAS_LIMIT_WIDTH, width));
                                groupCanvasLimitHeight.put(key, Math.max(MIN_CANVAS_LIMIT_HEIGHT, height));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupCanvasSprites")) {
                    JsonObject spritesRoot = displayObject.getAsJsonObject("groupCanvasSprites");
                    for (var entry : spritesRoot.entrySet()) {
                        try {
                            String group = entry.getKey();
                            if (group == null || group.isBlank()) continue;
                            JsonArray sprites = spritesRoot.getAsJsonArray(group);
                            if (sprites == null || sprites.size() == 0) continue;
                            List<CanvasSprite> list = new ArrayList<>();
                            for (int i = 0; i < sprites.size(); i++) {
                                try {
                                    JsonObject sprite = sprites.get(i).getAsJsonObject();
                                    CanvasSprite value = new CanvasSprite(
                                        GsonHelper.getAsString(sprite, "id", ""),
                                        GsonHelper.getAsString(sprite, "path", ""),
                                        GsonHelper.getAsInt(sprite, "x", 0),
                                        GsonHelper.getAsInt(sprite, "y", 0),
                                        Math.max(1, GsonHelper.getAsInt(sprite, "width", 1)),
                                        Math.max(1, GsonHelper.getAsInt(sprite, "height", 1)),
                                        Math.max(0, Math.min(100, GsonHelper.getAsInt(sprite, "opacity", 100)))
                                    );
                                    if (value.isValid()) list.add(value);
                                } catch (Exception ignored) {
                                }
                            }
                            if (!list.isEmpty()) groupCanvasSprites.put(group, list);
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupMinimapCollapsed")) {
                    JsonObject collapsedRoot = displayObject.getAsJsonObject("groupMinimapCollapsed");
                    for (var entry : collapsedRoot.entrySet()) {
                        try {
                            String key = entry.getKey();
                            boolean value = collapsedRoot.get(key).getAsBoolean();
                            if (!key.isBlank() && value) {
                                groupMinimapCollapsed.put(key, true);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupInspectorCollapsed")) {
                    JsonObject collapsedRoot = displayObject.getAsJsonObject("groupInspectorCollapsed");
                    for (var entry : collapsedRoot.entrySet()) {
                        try {
                            String key = entry.getKey();
                            boolean value = collapsedRoot.get(key).getAsBoolean();
                            if (!key.isBlank() && value) {
                                groupInspectorCollapsed.put(key, true);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
                if (displayObject.has("groupHiddenConnections")) {
                    JsonObject hiddenRoot = displayObject.getAsJsonObject("groupHiddenConnections");
                    for (var entry : hiddenRoot.entrySet()) {
                        try {
                            String group = entry.getKey();
                            if (group == null || group.isBlank()) continue;
                            JsonArray hidden = hiddenRoot.getAsJsonArray(group);
                            if (hidden == null || hidden.size() == 0) continue;
                            java.util.Set<String> keys = new java.util.HashSet<>();
                            for (int i = 0; i < hidden.size(); i++) {
                                try {
                                    String key = hidden.get(i).getAsString();
                                    if (key != null && !key.isBlank()) keys.add(key.trim());
                                } catch (Exception ignored) {
                                }
                            }
                            if (!keys.isEmpty()) groupHiddenConnections.put(group, keys);
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (displayObject.has("groupGrid")) {
                    var groupGrid = displayObject.getAsJsonObject("groupGrid");
                    for (var entry : groupGrid.entrySet()) {
                        try {
                            var obj = groupGrid.getAsJsonObject(entry.getKey());
                            gridEnabled = GsonHelper.getAsBoolean(obj, "gridEnabled", gridEnabled);
                            gridLock = GsonHelper.getAsBoolean(obj, "gridLock", gridLock);
                            gridSize = GsonHelper.getAsInt(obj, "gridSize", gridSize);
                            gridOpacity = GsonHelper.getAsInt(obj, "gridOpacity", gridOpacity);
                        } catch (Exception ignored) {
                        }
                    }
                }

                loadGroupFiles();
            } else {
                save();
            }
        } catch (Exception e) {
            Heracles.LOGGER.error("Error parsing {}:", DISPLAY_FILE, e);
        }
    }

    public static void save() {
        if (lastPath == null) return;
        File displayFile = lastPath.resolve(DISPLAY_FILE).toFile();
        JsonObject displayObject = new JsonObject();
        displayObject.addProperty("pinnedIndex", pinnedIndex);
        displayObject.addProperty("showTutorial", showTutorial);
        displayObject.addProperty("maxEditorHistory", maxEditorHistory);
        displayObject.addProperty("gridEnabled", gridEnabled);
        displayObject.addProperty("gridLock", gridLock);
        displayObject.addProperty("gridSize", gridSize);
        displayObject.addProperty("gridOpacity", gridOpacity);
        displayObject.addProperty("questDescriptionGridEnabled", questDescriptionGridEnabled);
        displayObject.addProperty("questDescriptionGridSnap", questDescriptionGridSnap);
        displayObject.addProperty("questDescriptionGridSize", questDescriptionGridSize);
        displayObject.addProperty("questDescriptionGuideSnapEnabled", questDescriptionGuideSnapEnabled);
        displayObject.addProperty("questDescriptionCenterSnapEnabled", questDescriptionCenterSnapEnabled);
        displayObject.addProperty("questDescriptionGuideSnapDistance", questDescriptionGuideSnapDistance);

        try {
            Files.createDirectories(lastPath);
            FileUtils.write(displayFile, Constants.PRETTY_GSON.toJson(displayObject), StandardCharsets.UTF_8);
            saveGroupFiles();
        } catch (Exception e) {
            Heracles.LOGGER.error("Error saving {}:", DISPLAY_FILE, e);
        }
    }

    private static void loadGroupFiles() {
        if (lastPath == null) return;
        Path groupsDir = lastPath.resolve(GROUPS_DIR);
        try {
            if (!Files.exists(groupsDir)) return;
            try (var stream = Files.list(groupsDir)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .forEach(path -> {
                        try {
                            String content = FileUtils.readFileToString(path.toFile(), StandardCharsets.UTF_8);
                            JsonObject root = Constants.PRETTY_GSON.fromJson(content, JsonObject.class);
                            if (root == null) return;
                            String group = GsonHelper.getAsString(root, "group", "");
                            if (group.isBlank()) {
                                group = path.getFileName().toString();
                                group = group.substring(0, group.length() - ".json".length());
                            }
                            if (group.isBlank()) return;
                            readGroupSettings(group, root);
                        } catch (Exception e) {
                            Heracles.LOGGER.warn("Failed to read group settings file {}", path, e);
                        }
                    });
            }
        } catch (Exception e) {
            Heracles.LOGGER.warn("Failed to load group settings from {}", groupsDir, e);
        }
    }

    private static void saveGroupFiles() {
        if (lastPath == null) return;
        Path groupsDir = lastPath.resolve(GROUPS_DIR);
        try {
            Files.createDirectories(groupsDir);
            java.util.Set<String> groups = collectAllGroups();
            java.util.Set<String> expectedFiles = new java.util.HashSet<>();

            for (String group : groups) {
                if (group == null || group.isBlank()) continue;
                JsonObject root = createGroupSettings(group);
                Path file = groupsDir.resolve(sanitizeGroupFileName(group) + ".json");
                expectedFiles.add(file.getFileName().toString());
                Files.writeString(
                    file,
                    Constants.PRETTY_GSON.toJson(root),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
                );
            }

            try (var stream = Files.list(groupsDir)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".json"))
                    .filter(path -> !expectedFiles.contains(path.getFileName().toString()))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            Heracles.LOGGER.warn("Failed to delete stale group settings file {}", path, e);
                        }
                    });
            }
        } catch (Exception e) {
            Heracles.LOGGER.warn("Failed to save group settings files to {}", groupsDir, e);
        }
    }

    private static void readGroupSettings(String group, JsonObject root) {
        String background = GsonHelper.getAsString(root, "canvasBackground", "").trim();
        if (!background.isBlank()) groupCanvasBackgrounds.put(group, background);
        else groupCanvasBackgrounds.remove(group);

        String texture = GsonHelper.getAsString(root, "cardTexture", "").trim();
        if (!texture.isBlank()) groupCardTextures.put(group, texture);
        else groupCardTextures.remove(group);

        int opacity = Math.max(0, Math.min(100, GsonHelper.getAsInt(root, "backgroundOpacity", 100)));
        groupBackgroundOpacity.put(group, opacity);

        JsonObject canvasLimit = GsonHelper.getAsJsonObject(root, "canvasLimit", new JsonObject());
        int width = Math.max(0, GsonHelper.getAsInt(canvasLimit, "width", 0));
        int height = Math.max(0, GsonHelper.getAsInt(canvasLimit, "height", 0));
        if (width > 0 && height > 0) {
            groupCanvasLimitWidth.put(group, Math.max(MIN_CANVAS_LIMIT_WIDTH, width));
            groupCanvasLimitHeight.put(group, Math.max(MIN_CANVAS_LIMIT_HEIGHT, height));
        } else {
            groupCanvasLimitWidth.remove(group);
            groupCanvasLimitHeight.remove(group);
        }

        JsonArray sprites = GsonHelper.getAsJsonArray(root, "canvasSprites", new JsonArray());
        if (sprites.size() > 0) {
            List<CanvasSprite> list = new ArrayList<>();
            for (int i = 0; i < sprites.size(); i++) {
                try {
                    JsonObject sprite = sprites.get(i).getAsJsonObject();
                    CanvasSprite value = new CanvasSprite(
                        GsonHelper.getAsString(sprite, "id", ""),
                        GsonHelper.getAsString(sprite, "path", ""),
                        GsonHelper.getAsInt(sprite, "x", 0),
                        GsonHelper.getAsInt(sprite, "y", 0),
                        Math.max(1, GsonHelper.getAsInt(sprite, "width", 1)),
                        Math.max(1, GsonHelper.getAsInt(sprite, "height", 1)),
                        Math.max(0, Math.min(100, GsonHelper.getAsInt(sprite, "opacity", 100)))
                    );
                    if (value.isValid()) list.add(value);
                } catch (Exception ignored) {
                }
            }
            if (!list.isEmpty()) groupCanvasSprites.put(group, list);
            else groupCanvasSprites.remove(group);
        } else {
            groupCanvasSprites.remove(group);
        }

        if (GsonHelper.getAsBoolean(root, "minimapCollapsed", false)) groupMinimapCollapsed.put(group, true);
        else groupMinimapCollapsed.remove(group);

        if (GsonHelper.getAsBoolean(root, "inspectorCollapsed", false)) groupInspectorCollapsed.put(group, true);
        else groupInspectorCollapsed.remove(group);

        JsonArray hidden = GsonHelper.getAsJsonArray(root, "hiddenConnections", new JsonArray());
        if (hidden.size() > 0) {
            java.util.Set<String> set = new java.util.HashSet<>();
            for (int i = 0; i < hidden.size(); i++) {
                try {
                    String key = hidden.get(i).getAsString();
                    if (key != null && !key.isBlank()) set.add(key.trim());
                } catch (Exception ignored) {
                }
            }
            if (!set.isEmpty()) groupHiddenConnections.put(group, set);
            else groupHiddenConnections.remove(group);
        } else {
            groupHiddenConnections.remove(group);
        }
    }

    private static JsonObject createGroupSettings(String group) {
        JsonObject root = new JsonObject();
        root.addProperty("group", group);
        root.addProperty("canvasBackground", groupCanvasBackgrounds.getOrDefault(group, ""));
        root.addProperty("cardTexture", groupCardTextures.getOrDefault(group, ""));
        root.addProperty("backgroundOpacity", getBackgroundOpacity(group));

        JsonObject canvasLimit = new JsonObject();
        int width = getCanvasLimitWidth(group);
        int height = getCanvasLimitHeight(group);
        canvasLimit.addProperty("width", width);
        canvasLimit.addProperty("height", height);
        root.add("canvasLimit", canvasLimit);

        JsonArray sprites = new JsonArray();
        for (CanvasSprite sprite : getCanvasSprites(group)) {
            if (sprite == null || !sprite.isValid()) continue;
            JsonObject value = new JsonObject();
            value.addProperty("id", sprite.id());
            value.addProperty("path", sprite.path());
            value.addProperty("x", sprite.x());
            value.addProperty("y", sprite.y());
            value.addProperty("width", Math.max(1, sprite.width()));
            value.addProperty("height", Math.max(1, sprite.height()));
            value.addProperty("opacity", Math.max(0, Math.min(100, sprite.opacity())));
            sprites.add(value);
        }
        root.add("canvasSprites", sprites);

        root.addProperty("minimapCollapsed", isMinimapCollapsed(group));
        root.addProperty("inspectorCollapsed", isInspectorCollapsed(group));

        JsonArray hidden = new JsonArray();
        java.util.Set<String> connections = groupHiddenConnections.get(group);
        if (connections != null) {
            for (String key : connections) {
                if (key == null || key.isBlank()) continue;
                hidden.add(key);
            }
        }
        root.add("hiddenConnections", hidden);
        return root;
    }

    private static java.util.Set<String> collectAllGroups() {
        java.util.Set<String> groups = new java.util.HashSet<>();
        groups.addAll(groupCanvasBackgrounds.keySet());
        groups.addAll(groupCardTextures.keySet());
        groups.addAll(groupBackgroundOpacity.keySet());
        groups.addAll(groupCanvasLimitWidth.keySet());
        groups.addAll(groupCanvasLimitHeight.keySet());
        groups.addAll(groupCanvasSprites.keySet());
        groups.addAll(groupMinimapCollapsed.keySet());
        groups.addAll(groupInspectorCollapsed.keySet());
        groups.addAll(groupHiddenConnections.keySet());
        groups.removeIf(group -> group == null || group.isBlank());
        return groups;
    }

    private static String sanitizeGroupFileName(String group) {
        return group.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private static Path resolveDisplayDirectory(Path path) {
        if (path == null) return null;
        String last = path.getFileName() == null ? "" : path.getFileName().toString();
        if ("config".equalsIgnoreCase(last)) {
            return path.resolve(HERACLES_DIR);
        }
        if (HERACLES_DIR.equalsIgnoreCase(last)) {
            return path;
        }
        return path.resolve("config").resolve(HERACLES_DIR);
    }
}
