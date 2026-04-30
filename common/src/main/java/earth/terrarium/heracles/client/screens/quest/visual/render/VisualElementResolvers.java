package earth.terrarium.heracles.client.screens.quest.visual.render;

import earth.terrarium.heracles.Heracles;
import earth.terrarium.heracles.client.screens.quest.visual.model.VisualElement;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class VisualElementResolvers {
    private VisualElementResolvers() {
    }

    public static ItemStack parseItemStack(VisualElement element) {
        if (element.itemId != null && !element.itemId.isBlank()) {
            ResourceLocation location = ResourceLocation.tryParse(element.itemId);
            if (location == null) return ItemStack.EMPTY;
            Item item = BuiltInRegistries.ITEM.getOptional(location).orElse(Items.AIR);
            if (item == Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(item);
        }
        if (element.itemTag != null && !element.itemTag.isBlank()) {
            ResourceLocation location = ResourceLocation.tryParse(element.itemTag.replace("#", ""));
            if (location == null) return ItemStack.EMPTY;
            TagKey<Item> key = TagKey.create(Registries.ITEM, location);
            Item tagItem = Heracles.getRegistryAccess()
                .registry(Registries.ITEM)
                .flatMap(registry -> registry.getTag(key))
                .flatMap(set -> set.stream().findFirst().map(holder -> holder.value()))
                .orElse(Items.AIR);
            if (tagItem == Items.AIR) return ItemStack.EMPTY;
            return new ItemStack(tagItem);
        }
        return ItemStack.EMPTY;
    }

    public static EntityType<?> parseEntityType(VisualElement element) {
        if (element.entityId == null || element.entityId.isBlank()) return null;
        ResourceLocation location = ResourceLocation.tryParse(element.entityId);
        if (location == null) return null;
        return BuiltInRegistries.ENTITY_TYPE.getOptional(location).orElse(null);
    }

    public static boolean isVillagerLike(EntityType<?> type) {
        return type == EntityType.VILLAGER || type == EntityType.ZOMBIE_VILLAGER;
    }

    public static void applyEntityVariant(VisualElement element, net.minecraft.world.entity.Entity entity) {
        if (element == null || entity == null || element.entityVariant == null || element.entityVariant.isBlank()) return;
        ResourceLocation id = ResourceLocation.tryParse(element.entityVariant);
        if (id == null) return;
        VillagerProfession profession = BuiltInRegistries.VILLAGER_PROFESSION.getOptional(id).orElse(null);
        if (profession == null) return;
        if (entity instanceof Villager villager) {
            VillagerData data = villager.getVillagerData().setProfession(profession);
            villager.setVillagerData(data);
        } else if (entity instanceof ZombieVillager zombieVillager) {
            VillagerData data = zombieVillager.getVillagerData().setProfession(profession);
            zombieVillager.setVillagerData(data);
        }
    }
}
