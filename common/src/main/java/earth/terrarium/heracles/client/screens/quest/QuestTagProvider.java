package earth.terrarium.heracles.client.screens.quest;

import earth.terrarium.heracles.client.tags.BackgroundTagElement;
import earth.terrarium.heracles.client.tags.EntityTagElement;
import earth.terrarium.heracles.client.tags.ImageTagElement;
import earth.terrarium.heracles.client.tags.ItemTagElement;
import earth.terrarium.heracles.client.tags.SubtitleTagElement;
import earth.terrarium.heracles.client.tags.TextboxTagElement;
import earth.terrarium.heracles.client.tags.WidgetTagElement;
import earth.terrarium.hermes.api.DefaultTagProvider;

public class QuestTagProvider extends DefaultTagProvider {

    public QuestTagProvider() {
        super();
        addSerializer("subtitle", SubtitleTagElement::new);
        addSerializer("task", WidgetTagElement::ofTask);
        addSerializer("reward", WidgetTagElement::ofReward);
        addSerializer("item", ItemTagElement::new);
        addSerializer("image", ImageTagElement::new);
        addSerializer("entity", EntityTagElement::new);
        addSerializer("background", BackgroundTagElement::new);
        addSerializer("textbox", TextboxTagElement::new);
    }
}
