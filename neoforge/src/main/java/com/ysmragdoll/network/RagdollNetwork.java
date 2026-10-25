package com.ysmragdoll.network;

import com.ysmragdoll.client.ClientRagdollManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class RagdollNetwork {
    private RagdollNetwork() {}
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("ysmragdoll", path); }

    public record Death(PlayerDeathSnapshot value) implements CustomPacketPayload {
        public static final Type<Death> TYPE = new Type<>(id("death_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Death> CODEC = StreamCodec.of(
                (buffer, message) -> PlayerDeathSnapshot.encode(message.value, buffer),
                buffer -> new Death(PlayerDeathSnapshot.decode(buffer)));
        public Type<Death> type() { return TYPE; }
    }

    public record Blast(ExplosionImpulseSnapshot value) implements CustomPacketPayload {
        public static final Type<Blast> TYPE = new Type<>(id("explosion_v2"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Blast> CODEC = StreamCodec.of(
                (buffer, message) -> ExplosionImpulseSnapshot.encode(message.value, buffer),
                buffer -> new Blast(ExplosionImpulseSnapshot.decode(buffer)));
        public Type<Blast> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2").optional();
        registrar.playToClient(Death.TYPE, Death.CODEC, (message, context) ->
                ClientRagdollManager.onPlayerDeath(message.value));
        registrar.playToClient(Blast.TYPE, Blast.CODEC, (message, context) -> {
            if (message.value.valid()) ClientRagdollManager.onExplosion(message.value);
        });
    }

    private static void send(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection.hasChannel(payload)) PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendDeathSnapshot(ServerPlayer player, PlayerDeathSnapshot message) {
        var payload = new Death(message);
        send(player, payload);
        for (var observer : player.serverLevel().getChunkSource().chunkMap.getPlayers(player.chunkPosition(), false)) {
            if (observer != player) send(observer, payload);
        }
    }

    public static void sendExplosion(ServerLevel level, ExplosionImpulseSnapshot message) {
        var payload = new Blast(message);
        for (var player : level.players()) send(player, payload);
    }
}
