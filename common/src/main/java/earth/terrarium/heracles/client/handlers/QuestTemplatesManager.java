package earth.terrarium.heracles.client.handlers;

import earth.terrarium.heracles.api.quests.Quest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class QuestTemplatesManager {
    private static final List<Template> TEMPLATES = new ArrayList<>();

    public static void addTemplate(String name, Quest quest) {
        TEMPLATES.add(new Template(name, quest));
    }

    public static List<Template> listTemplates() {
        return Collections.unmodifiableList(TEMPLATES);
    }

    public static Optional<Quest> getTemplate(String name) {
        return TEMPLATES.stream().filter(t -> t.name.equals(name)).map(t -> t.quest).findFirst();
    }

    public static record Template(String name, Quest quest) {}
}
