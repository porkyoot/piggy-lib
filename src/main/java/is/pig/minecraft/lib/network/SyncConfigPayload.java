package is.pig.minecraft.lib.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

import is.pig.minecraft.lib.PiggyLib;
import is.pig.minecraft.lib.config.PiggyClientConfig;

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

    public static void registerPacket(){
        // Register the payload type first
        //ResourceLocation PACKET_ID = ResourceLocation.fromNamespaceAndPath("piggy-lib", "sync_config");
        PayloadTypeRegistry.playS2C().register(SyncConfigPayload.TYPE, SyncConfigPayload.CODEC);

        // Register the SINGLE global receiver (removed duplicate)
        ClientPlayNetworking.registerGlobalReceiver(
            SyncConfigPayload.TYPE, 
            (payload, context) -> {
                // Execute on the main thread to ensure thread-safety
                context.client().execute(() -> {
                    PiggyClientConfig config = PiggyClientConfig.getInstance();
                    config.serverAllowCheats = payload.allowCheats();
                    config.serverFeatures = payload.features();
                    
                    PiggyLib.LOGGER.info("[ANTI-CHEAT DEBUG] Received server config: allowCheats={}, features={}", payload.allowCheats(), payload.features());
                    PiggyLib.LOGGER.info("[ANTI-CHEAT DEBUG] Updated PiggyClientConfig - serverAllowCheats: {}, serverFeatures: {}", config.serverAllowCheats, config.serverFeatures);
                    
                    // IMPORTANT: If PiggyBuild is loaded, also update its config instance
                    // This ensures anti-cheat enforcement works in PiggyBuild
                    try {
                        Class<?> piggyBuildConfigClass = Class.forName("is.pig.minecraft.build.config.PiggyBuildConfig");
                        Object piggyBuildConfig = piggyBuildConfigClass.getMethod("getInstance").invoke(null);
                        if (piggyBuildConfig instanceof PiggyClientConfig) {
                            PiggyClientConfig buildConfig = (PiggyClientConfig) piggyBuildConfig;
                            buildConfig.serverAllowCheats = payload.allowCheats();
                            buildConfig.serverFeatures = payload.features();
                            PiggyLib.LOGGER.info("[ANTI-CHEAT DEBUG] Also updated PiggyBuildConfig instance");
                        }
                    } catch (Exception e) {
                        // PiggyBuild not loaded or error accessing it - that's fine
                    }
                    
                    // Also update PiggyInventoryConfig if loaded
                    try {
                        Class<?> piggyInvConfigClass = Class.forName("is.pig.minecraft.inventory.config.PiggyConfig");
                        Object piggyInvConfig = piggyInvConfigClass.getMethod("getInstance").invoke(null);
                        if (piggyInvConfig instanceof PiggyClientConfig) {
                            PiggyClientConfig invConfig = (PiggyClientConfig) piggyInvConfig;
                            invConfig.serverAllowCheats = payload.allowCheats();
                            invConfig.serverFeatures = payload.features();
                            PiggyLib.LOGGER.info("[ANTI-CHEAT DEBUG] Also updated PiggyInventoryConfig instance");
                        }
                    } catch (Exception e) {
                        // PiggyInventory not loaded or error accessing it - that's fine
                    }
                });
            }
        );
    }
}
