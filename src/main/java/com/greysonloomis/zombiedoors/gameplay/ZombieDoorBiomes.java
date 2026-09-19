package com.greysonloomis.zombiedoors.gameplay;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import com.greysonloomis.zombiedoors.ZombieDoors;
import java.util.Map;
import net.minecraft.util.RandomSource;
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

    public static Item doorFor(Holder<Biome> biome, RandomSource random) {
        return doorFor(biome, ZombieDoors.configOrDefaults().biomeDoorOverrides(), random);
    }

    public static Item doorFor(Holder<Biome> biome, Map<String, String> overrides) {
        return doorFor(biome, overrides, null);
    }

    public static Item doorFor(Holder<Biome> biome, Map<String, String> overrides, RandomSource random) {
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
        return defaultDoorFor(biome, random);
    }

    private static Item configuredDoor(String id) {
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return item != null && ZombieDoorShieldBehavior.isWoodenDoor(new ItemStack(item)) ? item : null;
    }

    private static Item defaultDoorFor(Holder<Biome> biome, RandomSource random) {
        if (biome.is(Biomes.BAMBOO_JUNGLE)) return Items.BAMBOO_DOOR;
        if (biome.is(Biomes.CHERRY_GROVE)) return Items.CHERRY_DOOR;
        if (biome.is(Biomes.MANGROVE_SWAMP)) return Items.MANGROVE_DOOR;
        if (biome.is(Biomes.PALE_GARDEN)) return Items.PALE_OAK_DOOR;
        if (biome.is(Biomes.DARK_FOREST) || biome.is(BiomeTags.IS_BADLANDS)) return Items.DARK_OAK_DOOR;
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)) {
            return Items.BIRCH_DOOR;
        }
        if (biome.is(Biomes.CRIMSON_FOREST)) return Items.CRIMSON_DOOR;
        if (biome.is(Biomes.WARPED_FOREST)) return Items.WARPED_DOOR;
        if (biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.DESERT)) return Items.JUNGLE_DOOR;
        if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(Biomes.STONY_PEAKS)) return Items.ACACIA_DOOR;
        if (biome.is(BiomeTags.IS_TAIGA) || biome.value().getBaseTemperature() <= 0.2F
            || biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN)) {
            return Items.SPRUCE_DOOR;
        }
        if (biome.is(Biomes.FOREST) && random != null && random.nextInt(20) == 0) return Items.BIRCH_DOOR;
        return Items.OAK_DOOR;
    }
}
