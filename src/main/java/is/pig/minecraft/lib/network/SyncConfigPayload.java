package is.pig.minecraft.lib.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

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
}
