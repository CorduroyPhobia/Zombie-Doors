package com.greysonloomis.zombiedoors.network;

import com.greysonloomis.zombiedoors.config.ZombieDoorsConfig;
import com.greysonloomis.zombiedoors.generated.ProjectIdentity;
import java.util.LinkedHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-owned gameplay settings. Receiving this packet never writes a client's config file. */
public record DoorConfigPayload(ZombieDoorsConfig config) implements CustomPacketPayload {
    public static final Type<DoorConfigPayload> TYPE = new Type<>(
        Identifier.fromNamespaceAndPath(ProjectIdentity.MOD_ID, "config_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DoorConfigPayload> CODEC = new StreamCodec<>() {
        @Override public DoorConfigPayload decode(RegistryFriendlyByteBuf buf) {
            boolean breaking = buf.readBoolean(), shields = buf.readBoolean();
            double spawn = buf.readDouble();
            int durability = buf.readVarInt();
            double axe = buf.readDouble(), attack = buf.readDouble(), reach = buf.readDouble();
            double transition = buf.readDouble(), dwell = buf.readDouble();
            int count = buf.readVarInt();
            if (count < 0 || count > 128) throw new IllegalArgumentException("Invalid biome override count");
            var overrides = new LinkedHashMap<String, String>();
            for (int i = 0; i < count; i++) overrides.put(buf.readUtf(256), buf.readUtf(256));
            return new DoorConfigPayload(new ZombieDoorsConfig(breaking, shields, spawn, durability,
                axe, attack, reach, transition, dwell, overrides));
        }
        @Override public void encode(RegistryFriendlyByteBuf buf, DoorConfigPayload payload) {
            var c = payload.config();
            buf.writeBoolean(c.enableReliableZombieDoorBreaking());
            buf.writeBoolean(c.enableZombieDoorShields());
            buf.writeDouble(c.zombieDoorShieldSpawnChance());
            buf.writeVarInt(c.zombieDoorShieldDurability());
            buf.writeDouble(c.zombieDoorShieldAxeDisableSeconds());
            buf.writeDouble(c.zombieDoorWhackCooldownSeconds());
            buf.writeDouble(c.zombieDoorWhackReachBonus());
            buf.writeDouble(c.zombieDoorPoseTransitionSeconds());
            buf.writeDouble(c.zombieDoorPoseCooldownSeconds());
            buf.writeVarInt(c.biomeDoorOverrides().size());
            c.biomeDoorOverrides().forEach((biome, door) -> { buf.writeUtf(biome, 256); buf.writeUtf(door, 256); });
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
