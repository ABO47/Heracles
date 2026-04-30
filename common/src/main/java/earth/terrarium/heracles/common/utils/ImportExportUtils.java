package earth.terrarium.heracles.common.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult;
import earth.terrarium.heracles.api.quests.Quest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ImportExportUtils {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void exportQuests(Map<String, Quest> quests, Path out) throws IOException {
        Map<String, JsonElement> tree = new HashMap<>();
        for (Map.Entry<String, Quest> e : quests.entrySet()) {
            DataResult<JsonElement> result = Quest.CODEC.encodeStart(JsonOps.INSTANCE, e.getValue());
            JsonElement element = result.resultOrPartial(s -> { throw new RuntimeException("Failed to encode quest: " + s); }).orElseThrow();
            tree.put(e.getKey(), element);
        }
        String json = GSON.toJson(tree);
        Files.createDirectories(out.getParent());
        Files.writeString(out, json);
    }

    public static Map<String, Quest> importQuests(Path in) throws IOException {
        String text = Files.readString(in);
        Map<?, ?> parsed = GSON.fromJson(text, Map.class);
        Map<String, Quest> out = new HashMap<>();
        for (Object k : parsed.keySet()) {
            String key = String.valueOf(k);
            Object raw = parsed.get(k);
            JsonElement element = GSON.toJsonTree(raw);
            DataResult<Quest> result = Quest.CODEC.parse(JsonOps.INSTANCE, element);
            Quest q = result.resultOrPartial(s -> { throw new RuntimeException("Failed to parse quest: " + s); }).orElseThrow();
            out.put(key, q);
        }
        return out;
    }
}
