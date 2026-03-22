package is.pig.minecraft.lib;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.network.SyncConfigPayload;

public class PiggyLibClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        PiggyLib.LOGGER.info("Initializing Piggy Lib Client");
        SyncConfigPayload.registerPacket();
        is.pig.minecraft.lib.ui.IconQueueOverlay.register();

        // Register Action Queue processing
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PiggyActionQueue.getInstance().tick(client);
        });
    }
}
