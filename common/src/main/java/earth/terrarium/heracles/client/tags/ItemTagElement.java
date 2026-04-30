package earth.terrarium.heracles.client.tags;

import earth.terrarium.heracles.Heracles;
import earth.terrarium.hermes.api.TagElement;
import earth.terrarium.hermes.api.themes.Theme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import com.mojang.math.Axis;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Map;

public class ItemTagElement implements TagElement {

    private final ItemStack stack;
    private final int offsetX;
    private final int offsetY;
    private final int boxWidth;
    private final int boxHeight;
    private final int rotation;

    public ItemTagElement(Map<String, String> parameters) {
        String itemId = value(parameters, "id");
        String tagId = value(parameters, "tag");
        this.offsetX = parseInt(value(parameters, "x"), 0, -1024, 4096);
        this.offsetY = parseInt(value(parameters, "y"), 0, -1024, 4096);
        this.boxWidth = parseInt(value(parameters, "w"), 16, 16, 4096);
        this.boxHeight = parseInt(value(parameters, "h"), 16, 16, 4096);
        this.rotation = parseInt(value(parameters, "rotation"), 0, -3600, 3600);

        if (!itemId.isBlank()) {
            Item item = parseItem(itemId);
            this.stack = new ItemStack(item);
            return;
        }

        if (!tagId.isBlank()) {
            Item item = resolveFirstTagItem(tagId);
            this.stack = new ItemStack(item);
            return;
        }

        this.stack = ItemStack.EMPTY;
    }

    @Override
    public void render(Theme theme, GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        int drawX = x + this.offsetX;
        int drawY = y + this.offsetY;
        if (!this.stack.isEmpty()) {
            int drawSize = Math.max(8, Math.min(this.boxWidth, this.boxHeight));
            int iconX = drawX + Math.max(0, (this.boxWidth - drawSize) / 2);
            int iconY = drawY + Math.max(0, (this.boxHeight - drawSize) / 2);
            float scale = drawSize / 16.0f;
            graphics.pose().pushPose();
            graphics.pose().translate(iconX + drawSize / 2.0f, iconY + drawSize / 2.0f, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(this.rotation));
            graphics.pose().translate(-drawSize / 2.0f, -drawSize / 2.0f, 0);
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.renderFakeItem(this.stack, 0, 0);
            graphics.pose().popPose();
        }
    }

    @Override
    public int getHeight(int width) {
        return 0;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button, int width) {
        return false;
    }

    private static String value(Map<String, String> parameters, String key) {
        String value = parameters.getOrDefault(key, "");
        return value == null ? "" : value.trim();
    }

    private static Item parseItem(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) return Items.AIR;
        return BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR);
    }

    private static Item resolveFirstTagItem(String tag) {
        String normalized = tag.replace("#", "").trim();
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null) return Items.AIR;
        TagKey<Item> key = TagKey.create(Registries.ITEM, id);
        return Heracles.getRegistryAccess()
            .registry(Registries.ITEM)
            .flatMap(registry -> registry.getTag(key))
            .flatMap(set -> set.stream().findFirst().map(holder -> holder.value()))
            .orElse(Items.AIR);
    }

    private static int parseInt(String raw, int fallback, int min, int max) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return net.minecraft.util.Mth.clamp(Integer.parseInt(raw.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
