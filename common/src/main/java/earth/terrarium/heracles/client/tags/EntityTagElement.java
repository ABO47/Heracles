package earth.terrarium.heracles.client.tags;

import earth.terrarium.heracles.api.client.WidgetUtils;
import earth.terrarium.hermes.api.TagElement;
import earth.terrarium.hermes.api.themes.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;

import java.util.Map;

public class EntityTagElement implements TagElement {

    private final String entityId;
    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final String variantId;
    private final int rotation;
    private final int spinSpeed;
    private net.minecraft.world.entity.Entity cachedEntity;
    private String cachedEntityId = "";

    public EntityTagElement(Map<String, String> parameters) {
        this.entityId = value(parameters, "id");
        this.x = clampInt(value(parameters, "x"), 0, -1024, 4096);
        this.y = clampInt(value(parameters, "y"), 0, -1024, 4096);
        this.w = clampInt(value(parameters, "w"), 64, 24, 4096);
        this.h = clampInt(value(parameters, "h"), 64, 24, 4096);
        this.variantId = value(parameters, "variant");
        this.rotation = clampInt(value(parameters, "rotation"), 0, -3600, 3600);
        this.spinSpeed = clampInt(value(parameters, "spin"), 0, 0, 360);
    }

    @Override
    public void render(Theme theme, GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY, boolean hovered, float partialTicks) {
        ResourceLocation id = ResourceLocation.tryParse(this.entityId);
        if (id == null || Minecraft.getInstance().level == null) return;
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
        if (type == null) return;
        var entity = getOrCreateEntity(type);
        if (entity == null) return;
        if (!this.variantId.isBlank()) {
            ResourceLocation variant = ResourceLocation.tryParse(this.variantId);
            if (variant != null) {
                VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(variant).orElse(null);
                if (profession != null) {
                    if (entity instanceof Villager villager) {
                        VillagerData data = villager.getVillagerData().setProfession(profession);
                        villager.setVillagerData(data);
                    } else if (entity instanceof ZombieVillager zombieVillager) {
                        VillagerData data = zombieVillager.getVillagerData().setProfession(profession);
                        zombieVillager.setVillagerData(data);
                    }
                }
            }
        }

        int drawX = x + this.x + 1;
        int drawY = y + this.y + 1;
        int drawW = Math.max(1, this.w - 1);
        int drawH = Math.max(1, this.h - 1);
        float angle = this.rotation + (((System.currentTimeMillis() % 1000000L) / 1000.0f) * this.spinSpeed);
        WidgetUtils.drawEntityInBox(graphics, drawX, drawY, drawW, drawH, entity, angle);
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

    private static int clampInt(String value, int fallback, int min, int max) {
        if (value == null || value.isBlank()) return fallback;
        try {
            return Mth.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private net.minecraft.world.entity.Entity getOrCreateEntity(EntityType<?> type) {
        if (Minecraft.getInstance().level == null) return null;
        if (this.cachedEntity == null || !this.entityId.equals(this.cachedEntityId)) {
            this.cachedEntity = type.create(Minecraft.getInstance().level);
            this.cachedEntityId = this.entityId;
        }
        return this.cachedEntity;
    }
}
