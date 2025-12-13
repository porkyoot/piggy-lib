package is.pig.minecraft.lib;

import net.fabricmc.api.ClientModInitializer;
import is.pig.minecraft.lib.network.SyncConfigPayload;

public class PiggyLibClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PiggyLib.LOGGER.info("Initializing Piggy Lib Client");
        SyncConfigPayload.registerPacket();
    }
}
