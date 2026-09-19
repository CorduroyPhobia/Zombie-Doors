package com.greysonloomis.zombiedoors.client;

import com.greysonloomis.zombiedoors.ZombieDoors;
import com.greysonloomis.zombiedoors.config.ZombieDoorsConfig;
import com.greysonloomis.zombiedoors.generated.ProjectIdentity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class ZombieDoorsConfigScreen {
    private ZombieDoorsConfigScreen() {}

    public static Screen create(Screen parent) {
        boolean serverControlled = Minecraft.getInstance().getConnection() != null
            && !Minecraft.getInstance().hasSingleplayerServer();
        ZombieDoorsConfig current = serverControlled ? ZombieDoors.clientConfigOrDefaults() : ZombieDoors.configOrDefaults();
        ZombieDoorsConfig defaults = ZombieDoorsConfig.defaults();
        boolean[] flags = {current.enableReliableZombieDoorBreaking(), current.enableZombieDoorShields()};
        int[] durability = {current.zombieDoorShieldDurability()};
        double[] values = {current.zombieDoorShieldSpawnChance(), current.zombieDoorShieldAxeDisableSeconds(),
            current.zombieDoorWhackCooldownSeconds(), current.zombieDoorWhackReachBonus(),
            current.zombieDoorPoseTransitionSeconds(), current.zombieDoorPoseCooldownSeconds()};
        ConfigBuilder builder = ConfigBuilder.create().setParentScreen(parent)
            .setTitle(Component.literal(ProjectIdentity.MOD_NAME)).setEditable(!serverControlled);
        ConfigCategory category = builder.getOrCreateCategory(Component.literal("Zombie Doors"));
        ConfigEntryBuilder entries = builder.entryBuilder();
        if (serverControlled) category.addEntry(entries.startTextDescription(
            label(ZombieDoors.hasRemoteConfig() ? "server_controlled" : "server_unavailable")).build());
        toggle(entries, category, "reliable_zombie_door_breaking", flags[0],
            defaults.enableReliableZombieDoorBreaking(), value -> flags[0] = value);
        toggle(entries, category, "zombie_door_shields", flags[1],
            defaults.enableZombieDoorShields(), value -> flags[1] = value);
        decimal(entries, category, "zombie_door_shield_spawn_chance", values[0], 0.05, 0, 0.5, value -> values[0] = value);
        category.addEntry(entries.startIntField(label("zombie_door_shield_durability"), durability[0])
            .setDefaultValue(48).setMin(1).setMax(512)
            .setTooltip(label("zombie_door_shield_durability.tooltip"))
            .setSaveConsumer(value -> durability[0] = value).build());
        decimal(entries, category, "zombie_door_shield_axe_disable_seconds", values[1], 6, 0, 20, value -> values[1] = value);
        decimal(entries, category, "zombie_door_whack_cooldown_seconds", values[2], 1.6, 1, 5, value -> values[2] = value);
        decimal(entries, category, "zombie_door_whack_reach_bonus", values[3], 0.75, 0, 2, value -> values[3] = value);
        decimal(entries, category, "zombie_door_pose_transition_seconds", values[4], 0.35, 0.05, 1, value -> values[4] = value);
        decimal(entries, category, "zombie_door_pose_cooldown_seconds", values[5], 0.5, 0, 2, value -> values[5] = value);
        builder.setSavingRunnable(() -> {
            if (serverControlled) return;
            try {
                ZombieDoors.configManager().updateAndSave(new ZombieDoorsConfig(flags[0], flags[1],
                    values[0], durability[0], values[1], values[2], values[3], values[4], values[5], current.biomeDoorOverrides()));
            } catch (IOException exception) {
                throw new UncheckedIOException("Could not save Zombie Doors settings", exception);
            }
        });
        return builder.build();
    }

    private static Component label(String key) {
        return Component.translatable("text." + ProjectIdentity.MOD_ID + ".config." + key);
    }

    private static void toggle(ConfigEntryBuilder entries, ConfigCategory category, String key,
                               boolean current, boolean fallback, Consumer<Boolean> save) {
        category.addEntry(entries.startBooleanToggle(label(key), current).setDefaultValue(fallback)
            .setTooltip(label(key + ".tooltip")).setSaveConsumer(save).build());
    }

    private static void decimal(ConfigEntryBuilder entries, ConfigCategory category, String key,
                                double current, double fallback, double min, double max, Consumer<Double> save) {
        category.addEntry(entries.startDoubleField(label(key), current).setDefaultValue(fallback)
            .setMin(min).setMax(max).setTooltip(label(key + ".tooltip")).setSaveConsumer(save).build());
    }
}
