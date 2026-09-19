package com.greysonloomis.zombiedoors.gameplay;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import com.greysonloomis.zombiedoors.ZombieDoors;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBiomeTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
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

    public static Item doorFor(Holder<Biome> biome, WorldGenLevel level, BlockPos pos, RandomSource random) {
        return doorFor(biome, ZombieDoors.configOrDefaults().biomeDoorOverrides(), random, level, pos);
    }

    public static Item doorFor(Holder<Biome> biome, Map<String, String> overrides) {
        return doorFor(biome, overrides, null);
    }

    public static Item doorFor(Holder<Biome> biome, Map<String, String> overrides, RandomSource random) {
        return doorFor(biome, overrides, random, null, BlockPos.ZERO);
    }

    private static Item doorFor(Holder<Biome> biome, Map<String, String> overrides, RandomSource random,
                               WorldGenLevel level, BlockPos pos) {
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
        var doors = new LinkedHashSet<Item>();
        biome.tags().forEach(tag -> {
            var id = tag.location();
            if (id.getNamespace().equals("zombiedoors") && id.getPath().startsWith("doors/")) {
                String door = id.getPath().substring(6);
                int separator = door.indexOf('/');
                if (separator > 0 && separator < door.length() - 1) {
                    Item item = configuredDoor(door.substring(0, separator) + ':' + door.substring(separator + 1));
                    if (item != null) doors.add(item);
                }
            }
        });
        String namespace = biome.unwrapKey().map(key -> key.identifier().getNamespace()).orElse("minecraft");
        if (doors.isEmpty() && !namespace.equals("minecraft")) {
            biome.tags().forEach(tag -> {
                var id = tag.location();
                if (id.getNamespace().equals("c") && id.getPath().startsWith("primary_wood_type/")) {
                    String doorName = id.getPath().substring("primary_wood_type/".length()) + "_door";
                    Item local = configuredDoor(namespace + ':' + doorName);
                    if (local != null) doors.add(local);
                    else for (Item item : BuiltInRegistries.ITEM) {
                        if (BuiltInRegistries.ITEM.getKey(item).getPath().equals(doorName)
                            && ZombieDoorShieldBehavior.isWoodenDoor(new ItemStack(item))) doors.add(item);
                    }
                }
            });
            if (doors.isEmpty() && level != null) {
                RandomSource sample = random == null ? RandomSource.create(0L) : random;
                for (var step : biome.value().getGenerationSettings().features()) {
                    for (var placed : step) {
                        placed.value().getFeatures().forEach(feature -> {
                            if (feature.value() instanceof TreeFeature tree) {
                                var log = BuiltInRegistries.BLOCK.getKey(tree.trunkProvider().value().getState(level, sample, pos).getBlock());
                                String doorName = log.getPath().replaceFirst("^stripped_", "")
                                    .replaceFirst("_(log|wood|stem|hyphae)$", "_door");
                                Item door = configuredDoor(log.getNamespace() + ':' + doorName);
                                if (door != null) doors.add(door);
                            }
                        });
                    }
                }
            }
        }
        if (doors.isEmpty()) return defaultDoorFor(biome, random);
        List<Item> choices = new ArrayList<>(doors);
        choices.sort(java.util.Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return choices.get(random == null ? 0 : random.nextInt(choices.size()));
    }

    private static Item configuredDoor(String id) {
        if (id == null) return null;
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
        return item != null && ZombieDoorShieldBehavior.isWoodenDoor(new ItemStack(item)) ? item : null;
    }

    private static Item defaultDoorFor(Holder<Biome> biome, RandomSource random) {
        boolean modded = biome.unwrapKey().map(key -> !key.identifier().getNamespace().equals("minecraft")).orElse(false);
        if (biome.is(Biomes.BAMBOO_JUNGLE)) return Items.BAMBOO_DOOR;
        if (biome.is(Biomes.CHERRY_GROVE)) return Items.CHERRY_DOOR;
        if (biome.is(Biomes.MANGROVE_SWAMP)) return Items.MANGROVE_DOOR;
        if (biome.is(Biomes.PALE_GARDEN)) return Items.PALE_OAK_DOOR;
        if (biome.is(Biomes.DARK_FOREST) || biome.is(BiomeTags.IS_BADLANDS)
            || modded && (biome.is(ConventionalBiomeTags.IS_DARK_FOREST) || biome.is(ConventionalBiomeTags.IS_BADLANDS))) return Items.DARK_OAK_DOOR;
        if (biome.is(Biomes.BIRCH_FOREST) || biome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
            || modded && biome.is(ConventionalBiomeTags.IS_BIRCH_FOREST)) {
            return Items.BIRCH_DOOR;
        }
        if (biome.is(Biomes.CRIMSON_FOREST)) return Items.CRIMSON_DOOR;
        if (biome.is(Biomes.WARPED_FOREST)) return Items.WARPED_DOOR;
        if (biome.is(BiomeTags.IS_JUNGLE) || biome.is(Biomes.DESERT)
            || modded && (biome.is(ConventionalBiomeTags.IS_JUNGLE) || biome.is(ConventionalBiomeTags.IS_DESERT))) return Items.JUNGLE_DOOR;
        if (biome.is(BiomeTags.IS_SAVANNA) || biome.is(Biomes.STONY_PEAKS)
            || modded && biome.is(ConventionalBiomeTags.IS_SAVANNA)) return Items.ACACIA_DOOR;
        if (biome.is(BiomeTags.IS_TAIGA) || biome.value().getBaseTemperature() <= 0.2F
            || biome.is(Biomes.COLD_OCEAN) || biome.is(Biomes.DEEP_COLD_OCEAN)
            || modded && (biome.is(ConventionalBiomeTags.IS_TAIGA) || biome.is(ConventionalBiomeTags.IS_COLD)
                || biome.is(ConventionalBiomeTags.IS_SNOWY))) {
            return Items.SPRUCE_DOOR;
        }
        if (biome.is(Biomes.FOREST) && random != null && random.nextInt(20) == 0) return Items.BIRCH_DOOR;
        return Items.OAK_DOOR;
    }
}
