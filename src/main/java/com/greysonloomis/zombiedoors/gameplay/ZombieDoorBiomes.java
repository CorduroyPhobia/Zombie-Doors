package com.greysonloomis.zombiedoors.gameplay;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import com.greysonloomis.zombiedoors.ZombieDoors;
import java.util.Map;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

public final class ZombieDoorBiomes {
    private ZombieDoorBiomes() {}

    public static Item doorFor(Holder<Biome> biome) {
        return doorFor(biome, ZombieDoors.configOrDefaults().biomeDoorOverrides());
    }

    public static Item doorFor(Holder<Biome> biome, Map<String, String> overrides) {
        String exact = biome.unwrapKey().map(key -> overrides.get(key.identifier().toString())).orElse(null);
        Item selected = configuredDoor(exact);
        if (selected != null) return selected;
        for (var entry : overrides.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
            if (entry.getKey().startsWith("#") && biome.is(TagKey.create(Registries.BIOME,
                Identifier.parse(entry.getKey().substring(1))))) {
                selected = configuredDoor(entry.getValue());
                if (selected != null) return selected;
            }
        }
        return defaultDoorFor(biome);
    }

    private static Item configuredDoor(String id) {
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return item != null && ZombieDoorShieldBehavior.isWoodenDoor(new ItemStack(item)) ? item : null;
    }

    private static Item defaultDoorFor(Holder<Biome> biome) {
        // Check bamboo jungle before IS_JUNGLE.
        if (biome.is(Biomes.BAMBOO_JUNGLE)) return Items.BAMBOO_DOOR;
        if (biome.is(Biomes.CHERRY_GROVE)) return Items.CHERRY_DOOR;
        if (biome.is(Biomes.MANGROVE_SWAMP)) return Items.MANGROVE_DOOR;
        if (biome.is(Biomes.PALE_GARDEN)) return Items.PALE_OAK_DOOR;
        if (biome.is(Biomes.DARK_FOREST)) return Items.DARK_OAK_DOOR;
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return Items.BIRCH_DOOR;
        }
        if (biome.is(Biomes.CRIMSON_FOREST)) return Items.CRIMSON_DOOR;
        if (biome.is(Biomes.WARPED_FOREST)) return Items.WARPED_DOOR;
        if (biome.is(BiomeTags.IS_JUNGLE)) return Items.JUNGLE_DOOR;
        if (biome.is(BiomeTags.IS_SAVANNA)) return Items.ACACIA_DOOR;
        if (biome.is(BiomeTags.IS_TAIGA) || biome.is(BiomeTags.IS_HILL)
            || biome.is(Biomes.GROVE) || biome.is(Biomes.SNOWY_PLAINS)
            || biome.is(Biomes.ICE_SPIKES) || biome.is(Biomes.SNOWY_SLOPES)
            || biome.is(Biomes.FROZEN_PEAKS) || biome.is(Biomes.JAGGED_PEAKS)
            || biome.is(Biomes.SNOWY_BEACH) || biome.is(Biomes.FROZEN_RIVER)) {
            return Items.SPRUCE_DOOR;
        }
        return Items.OAK_DOOR;
    }
}
