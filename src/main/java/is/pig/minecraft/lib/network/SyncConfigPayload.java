package is.pig.minecraft.lib.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

import is.pig.minecraft.lib.PiggyLib;

public record SyncConfigPayload(boolean allowCheats, Map<String, Boolean> features) implements CustomPacketPayload {
    public static final Type<SyncConfigPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("piggy-lib", "sync_config"));

    public static final StreamCodec<FriendlyByteBuf, SyncConfigPayload> CODEC = CustomPacketPayload.codec(
            SyncConfigPayload::write,
            SyncConfigPayload::new);

    public SyncConfigPayload(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf.readMap(FriendlyByteBuf::readUtf, FriendlyByteBuf::readBoolean));
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(allowCheats);
        buf.writeMap(features, FriendlyByteBuf::writeUtf, FriendlyByteBuf::writeBoolean);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static boolean registered = false;

    public static void registerPacket() {
        if (registered)
            return;
        registered = true;

        // Register the payload type first
        // ResourceLocation PACKET_ID =
        // ResourceLocation.fromNamespaceAndPath("piggy-lib", "sync_config");
        try {
            PayloadTypeRegistry.playS2C().register(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC);
        } catch (IllegalArgumentException e) {
            PiggyLib.LOGGER.warn("Packet type already registered, skipping: {}", e.getMessage());
        }

        // Register the SINGLE global receiver (removed duplicate)
        ClientPlayNetworking.registerGlobalReceiver(
                SyncConfigPayload.TYPE,
                (payload, context) -> {
                    // Execute on the main thread to ensure thread-safety
                    // Execute on the main thread to ensure thread-safety
                    context.client().execute(() -> {
                        // Use the shared Registry to notify all registered config instances
                        is.pig.minecraft.lib.config.PiggyConfigRegistry.getInstance()
                                .notifyConfigSynced(payload.allowCheats(), payload.features());

                        PiggyLib.LOGGER.info("[ANTI-CHEAT DEBUG] Received server config: allowCheats={}, features={}",
                                payload.allowCheats(), payload.features());
                    });
                });
    }
}
