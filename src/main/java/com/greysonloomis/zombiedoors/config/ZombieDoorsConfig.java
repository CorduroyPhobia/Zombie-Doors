package com.greysonloomis.zombiedoors.config;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.resources.Identifier;

public record ZombieDoorsConfig(
    boolean enableReliableZombieDoorBreaking,
    boolean enableZombieDoorShields,
    double zombieDoorShieldSpawnChance,
    int zombieDoorShieldDurability,
    double zombieDoorShieldAxeDisableSeconds,
    double zombieDoorWhackCooldownSeconds,
    double zombieDoorWhackReachBonus,
    double zombieDoorPoseTransitionSeconds,
    double zombieDoorPoseCooldownSeconds,
    Map<String, String> biomeDoorOverrides
) {
    public ZombieDoorsConfig(boolean breaking, boolean shields, double spawnChance, int durability,
                             double axeDisable, double attackCooldown, double reach,
                             double transition, double dwell) {
        this(breaking, shields, spawnChance, durability, axeDisable, attackCooldown, reach,
            transition, dwell, Map.of());
    }

    public ZombieDoorsConfig {
        zombieDoorShieldSpawnChance = clamp(zombieDoorShieldSpawnChance, 0, 0.5, 0.05);
        zombieDoorShieldDurability = Math.clamp(zombieDoorShieldDurability, 1, 512);
        zombieDoorShieldAxeDisableSeconds = clamp(zombieDoorShieldAxeDisableSeconds, 0, 20, 6);
        zombieDoorWhackCooldownSeconds = clamp(zombieDoorWhackCooldownSeconds, 1, 5, 1.6);
        zombieDoorWhackReachBonus = clamp(zombieDoorWhackReachBonus, 0, 2, 0.75);
        zombieDoorPoseTransitionSeconds = clamp(zombieDoorPoseTransitionSeconds, 0.05, 1, 0.35);
        zombieDoorPoseCooldownSeconds = clamp(zombieDoorPoseCooldownSeconds, 0, 2, 0.5);
        var overrides = new TreeMap<String, String>();
        if (biomeDoorOverrides != null) {
            if (biomeDoorOverrides.size() > 128) throw new IllegalArgumentException("At most 128 biome door overrides are supported");
            biomeDoorOverrides.forEach((biome, door) -> {
                if (biome == null || door == null || biome.length() > 256 || door.length() > 256
                    || Identifier.tryParse(biome.startsWith("#") ? biome.substring(1) : biome) == null
                    || Identifier.tryParse(door) == null) {
                    throw new IllegalArgumentException("Invalid biome door override: " + biome + " -> " + door);
                }
                String biomeId = (biome.startsWith("#") ? "#" : "")
                    + Identifier.parse(biome.startsWith("#") ? biome.substring(1) : biome);
                String doorId = Identifier.parse(door).toString();
                if (biomeId.length() > 256 || doorId.length() > 256) {
                    throw new IllegalArgumentException("Biome door override IDs must fit within 256 characters");
                }
                overrides.put(biomeId, doorId);
            });
        }
        biomeDoorOverrides = Collections.unmodifiableMap(overrides);
    }

    public static ZombieDoorsConfig defaults() {
        return new ZombieDoorsConfig(false, true, 0.05, 48, 6, 1.6, 0.75, 0.35, 0.5);
    }

    private static double clamp(double value, double min, double max, double fallback) {
        return Double.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
