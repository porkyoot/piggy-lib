package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.INetworkDispatcher;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class LegacyNetworkDispatcher implements INetworkDispatcher {

    public static final CustomPacketPayload.Type<GenericPayload> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("piggy", "generic"));

    public static final StreamCodec<FriendlyByteBuf, GenericPayload> CODEC = CustomPacketPayload.codec(
        GenericPayload::write,
        GenericPayload::new
    );

    @Override
    public void sendToClient(UUID playerUuid, String channel, byte[] payload) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            // On client, we can't send to other clients directly.
            return;
        }
        // Server logic (simplified, assumes we can find the server instance)
        // In a real mod, you'd get the server from an event or state.
    }

    @Override
    public void broadcastToClients(String channel, byte[] payload) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return;
        }
    }

    @Override
    public void sendToServer(String channel, byte[] payload) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            ClientPlayNetworking.send(new GenericPayload(channel, payload));
        }
    }

    public record GenericPayload(String channel, byte[] data) implements CustomPacketPayload {
        public GenericPayload(FriendlyByteBuf buf) {
            this(buf.readUtf(), buf.readByteArray());
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeUtf(channel);
            buf.writeByteArray(data);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
