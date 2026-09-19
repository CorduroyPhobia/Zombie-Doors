package com.greysonloomis.zombiedoors.client;

import com.greysonloomis.zombiedoors.ZombieDoors;
import com.greysonloomis.zombiedoors.network.DoorConfigPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class ZombieDoorsClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DoorConfigPayload.TYPE, (payload, context) ->
            ZombieDoors.setRemoteConfig(payload.config()));
        ClientPlayConnectionEvents.INIT.register((handler, client) -> ZombieDoors.setRemoteConfig(null));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ZombieDoors.setRemoteConfig(null));
    }
}
