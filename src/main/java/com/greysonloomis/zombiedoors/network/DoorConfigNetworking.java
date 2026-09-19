package com.greysonloomis.zombiedoors.network;

import com.greysonloomis.zombiedoors.ZombieDoors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class DoorConfigNetworking {
    private static volatile MinecraftServer activeServer;
    private DoorConfigNetworking() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(DoorConfigPayload.TYPE, DoorConfigPayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTED.register(server -> activeServer = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { if (activeServer == server) activeServer = null; });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> send(handler.getPlayer()));
        ZombieDoors.configManager().setChangeListener(config -> {
            var server = activeServer;
            if (server != null) server.execute(() -> server.getPlayerList().getPlayers().forEach(DoorConfigNetworking::send));
        });
    }

    private static void send(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, DoorConfigPayload.TYPE)) {
            ServerPlayNetworking.send(player, new DoorConfigPayload(ZombieDoors.configOrDefaults()));
        }
    }
}
