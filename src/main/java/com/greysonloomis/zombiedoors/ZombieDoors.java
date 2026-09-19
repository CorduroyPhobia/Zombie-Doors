package com.greysonloomis.zombiedoors;

import com.greysonloomis.zombiedoors.config.ZombieDoorsConfig;
import com.greysonloomis.zombiedoors.config.ZombieDoorsConfigManager;
import com.greysonloomis.zombiedoors.generated.ProjectIdentity;
import com.greysonloomis.zombiedoors.network.DoorConfigNetworking;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ZombieDoors implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(ProjectIdentity.MOD_ID);
    private static ZombieDoorsConfigManager configManager;
    private static volatile ZombieDoorsConfig remoteConfig;

    @Override
    public void onInitialize() {
        configManager = new ZombieDoorsConfigManager(
            FabricLoader.getInstance().getConfigDir().resolve("zombiedoors.json"), LOGGER);
        configManager.load();
        DoorConfigNetworking.initialize();
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) configManager.load();
        });
    }

    public static ZombieDoorsConfig configOrDefaults() {
        return configManager == null ? ZombieDoorsConfig.defaults() : configManager.get();
    }

    public static ZombieDoorsConfigManager configManager() { return configManager; }

    public static ZombieDoorsConfig clientConfigOrDefaults() {
        var received = remoteConfig;
        return received == null ? configOrDefaults() : received;
    }

    public static boolean hasRemoteConfig() { return remoteConfig != null; }
    public static void setRemoteConfig(ZombieDoorsConfig config) { remoteConfig = config; }
}
