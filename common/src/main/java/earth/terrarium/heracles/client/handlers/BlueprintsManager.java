package earth.terrarium.heracles.client.handlers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.teamresourceful.resourcefullib.common.lib.Constants;
import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.api.quests.Quest;
import net.minecraft.resources.RegistryOps;
import org.apache.commons.io.FileUtils;
import org.joml.Vector2i;

import java.io.File;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BlueprintsManager {
    private static final String SHARE_CODE_PREFIX = "HBC1:";
    private static final LinkedHashMap<String, List<ClientQuestClipboard.CopiedQuest>> BLUEPRINTS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, File> BLUEPRINT_FILES = new LinkedHashMap<>();
    private static final java.util.Set<String> LOADED_CONTENT = new java.util.HashSet<>();
    private static Path directory;
    private static boolean loaded = false;

    public static synchronized void init(Path gameDir) {
        directory = gameDir.resolve("config").resolve("heracles").resolve("blueprints");
        loaded = false;
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        BLUEPRINTS.clear();
        BLUEPRINT_FILES.clear();
        LOADED_CONTENT.clear();
        if (directory == null) return;
        try {
            File dir = directory.toFile();
            if (!dir.exists()) {
                dir.mkdirs();
                return;
            }
            File[] files = dir.listFiles((d, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
            if (files == null) return;
            for (File file : files) {
                try {
                    String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    JsonObject object = JsonParser.parseString(json).getAsJsonObject();
                    String name = object.has("name") ? object.get("name").getAsString() : file.getName().replaceFirst("\\.json$", "");
                    BLUEPRINTS.put(name, new ArrayList<>());
                    BLUEPRINT_FILES.put(name, file);
                } catch (Exception ex) {
                    Heracles.LOGGER.error("Failed to load blueprint file {}", file.getName(), ex);
                }
            }
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to load blueprint directory {}", directory, e);
        }
    }

    private static String safeFilename(String name) {
        String safe = name.trim().toLowerCase(Locale.ROOT).replace(" ", "_").replaceAll("[^a-z0-9_\\-]", "");
        return safe.isBlank() ? "blueprint" : safe;
    }

    private static File fileFor(String name) {
        if (directory == null) return null;
        return directory.resolve(safeFilename(name) + ".json").toFile();
    }

    private static List<ClientQuestClipboard.CopiedQuest> parseBlueprintFile(File file) {
        List<ClientQuestClipboard.CopiedQuest> list = new ArrayList<>();
        if (file == null || !file.exists()) return list;
        try {
            String json = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            JsonObject object = JsonParser.parseString(json).getAsJsonObject();
            list.addAll(parseEntries(object.has("entries") ? object.getAsJsonArray("entries") : new JsonArray()));
        } catch (Exception ex) {
            Heracles.LOGGER.error("Failed to parse blueprint file {}", file.getName(), ex);
        }
        return list;
    }

    private static void saveBlueprint(String name, List<ClientQuestClipboard.CopiedQuest> entries) {
        if (directory == null) return;
        try {
            File dir = directory.toFile();
            if (!dir.exists()) dir.mkdirs();
            JsonObject object = toJson(name, entries);
            FileUtils.write(fileFor(name), Constants.PRETTY_GSON.toJson(object), StandardCharsets.UTF_8);
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to save blueprint {}", name, e);
        }
    }

    private static JsonObject toJson(String name, List<ClientQuestClipboard.CopiedQuest> entries) {
        JsonObject object = new JsonObject();
        object.addProperty("name", name);
        JsonArray array = new JsonArray();
        for (ClientQuestClipboard.CopiedQuest cq : entries) {
            JsonObject e = new JsonObject();
            e.addProperty("originalId", cq.originalId);
            e.addProperty("sourceGroup", cq.sourceGroup);
            JsonArray pos = new JsonArray();
            pos.add(cq.sourcePos.x());
            pos.add(cq.sourcePos.y());
            e.add("sourcePos", pos);
            var encoded = Quest.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE, Heracles.getRegistryAccess()), cq.quest).result().orElse(null);
            if (encoded == null) continue;
            e.add("quest", encoded);
            array.add(e);
        }
        object.add("entries", array);
        return object;
    }

    private static List<ClientQuestClipboard.CopiedQuest> parseEntries(JsonArray entries) {
        List<ClientQuestClipboard.CopiedQuest> list = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            JsonObject e = entries.get(i).getAsJsonObject();
            String originalId = e.get("originalId").getAsString();
            String sourceGroup = e.get("sourceGroup").getAsString();
            JsonArray pos = e.getAsJsonArray("sourcePos");
            Vector2i sourcePos = new Vector2i(pos.get(0).getAsInt(), pos.get(1).getAsInt());
            Quest quest = Quest.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, Heracles.getRegistryAccess()), e.get("quest"))
                .result()
                .orElse(null);
            if (quest != null) {
                list.add(new ClientQuestClipboard.CopiedQuest(originalId, ClientQuestClipboard.deepCopy(quest), sourceGroup, sourcePos));
            }
        }
        return list;
    }

    public static synchronized void addBlueprint(String name, List<ClientQuestClipboard.CopiedQuest> entries) {
        ensureLoaded();
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) return;
        List<ClientQuestClipboard.CopiedQuest> copy = new ArrayList<>();
        for (ClientQuestClipboard.CopiedQuest cq : entries) {
            copy.add(new ClientQuestClipboard.CopiedQuest(cq.originalId, ClientQuestClipboard.deepCopy(cq.quest), cq.sourceGroup, new Vector2i(cq.sourcePos.x(), cq.sourcePos.y())));
        }
        BLUEPRINTS.put(trimmed, copy);
        BLUEPRINT_FILES.put(trimmed, fileFor(trimmed));
        LOADED_CONTENT.add(trimmed);
        saveBlueprint(trimmed, copy);
    }

    public static synchronized List<String> listNames() {
        ensureLoaded();
        return new ArrayList<>(BLUEPRINTS.keySet());
    }

    public static synchronized boolean exists(String name) {
        ensureLoaded();
        return BLUEPRINTS.containsKey(name);
    }

    public static synchronized Optional<List<ClientQuestClipboard.CopiedQuest>> get(String name) {
        ensureLoaded();
        if (BLUEPRINTS.containsKey(name) && !LOADED_CONTENT.contains(name)) {
            File file = BLUEPRINT_FILES.getOrDefault(name, fileFor(name));
            List<ClientQuestClipboard.CopiedQuest> parsed = parseBlueprintFile(file);
            BLUEPRINTS.put(name, parsed);
            LOADED_CONTENT.add(name);
        }
        return Optional.ofNullable(BLUEPRINTS.get(name));
    }

    public static synchronized void remove(String name) {
        ensureLoaded();
        BLUEPRINTS.remove(name);
        BLUEPRINT_FILES.remove(name);
        LOADED_CONTENT.remove(name);
        File file = fileFor(name);
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public static synchronized void rename(String oldName, String newName) {
        ensureLoaded();
        if (!BLUEPRINTS.containsKey(oldName) || BLUEPRINTS.containsKey(newName)) return;
        List<ClientQuestClipboard.CopiedQuest> v = BLUEPRINTS.remove(oldName);
        BLUEPRINTS.put(newName, v);
        File oldFile = fileFor(oldName);
        File newFile = fileFor(newName);
        if (oldFile != null && oldFile.exists() && newFile != null) {
            oldFile.renameTo(newFile);
        }
        BLUEPRINT_FILES.remove(oldName);
        BLUEPRINT_FILES.put(newName, newFile);
        if (LOADED_CONTENT.contains(oldName)) {
            LOADED_CONTENT.remove(oldName);
            LOADED_CONTENT.add(newName);
        }
        saveBlueprint(newName, v);
    }

    public static synchronized void duplicate(String sourceName, String targetName) {
        ensureLoaded();
        if (!BLUEPRINTS.containsKey(sourceName) || BLUEPRINTS.containsKey(targetName)) return;
        List<ClientQuestClipboard.CopiedQuest> source = BLUEPRINTS.get(sourceName);
        List<ClientQuestClipboard.CopiedQuest> copy = new ArrayList<>();
        for (ClientQuestClipboard.CopiedQuest cq : source) {
            copy.add(new ClientQuestClipboard.CopiedQuest(cq.originalId, ClientQuestClipboard.deepCopy(cq.quest), cq.sourceGroup, new Vector2i(cq.sourcePos.x(), cq.sourcePos.y())));
        }
        BLUEPRINTS.put(targetName, copy);
        saveBlueprint(targetName, copy);
    }

    public static synchronized Optional<String> exportToCode(String name) {
        ensureLoaded();
        Optional<List<ClientQuestClipboard.CopiedQuest>> optional = get(name);
        if (optional.isEmpty()) return Optional.empty();
        try {
            JsonObject payload = toJson(name, optional.get());
            byte[] raw = Constants.PRETTY_GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(baos)) {
                gzip.write(raw);
            }
            String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(baos.toByteArray());
            return Optional.of(SHARE_CODE_PREFIX + encoded);
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to export blueprint {} as code", name, e);
            return Optional.empty();
        }
    }

    public static synchronized Optional<String> importFromCode(String code) {
        ensureLoaded();
        if (code == null) return Optional.empty();
        String normalized = code.trim().replaceAll("\\s+", "");
        if (normalized.startsWith(SHARE_CODE_PREFIX)) {
            normalized = normalized.substring(SHARE_CODE_PREFIX.length());
        }
        try {
            byte[] compressed = Base64.getUrlDecoder().decode(normalized);
            String json;
            try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
                json = new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
            }
            JsonObject payload = JsonParser.parseString(json).getAsJsonObject();
            String baseName = payload.has("name") ? payload.get("name").getAsString().trim() : "blueprint";
            if (baseName.isEmpty()) baseName = "blueprint";
            String name = uniqueName(baseName);
            List<ClientQuestClipboard.CopiedQuest> entries = parseEntries(payload.has("entries") ? payload.getAsJsonArray("entries") : new JsonArray());
            if (entries.isEmpty()) return Optional.empty();
            addBlueprint(name, entries);
            return Optional.of(name);
        } catch (Exception e) {
            Heracles.LOGGER.error("Failed to import blueprint code", e);
            return Optional.empty();
        }
    }

    private static String uniqueName(String base) {
        String name = base;
        int suffix = 1;
        while (exists(name)) {
            name = base + "_" + suffix++;
        }
        return name;
    }

    public static synchronized Map<String, List<ClientQuestClipboard.CopiedQuest>> all() {
        ensureLoaded();
        return Collections.unmodifiableMap(BLUEPRINTS);
    }
}
