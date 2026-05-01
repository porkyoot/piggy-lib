package is.pig.minecraft.lib.action.deferred;
import is.pig.minecraft.api.*;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DeferredActionTracker {
    private static final DeferredActionTracker INSTANCE = new DeferredActionTracker();
    public static DeferredActionTracker getInstance() { return INSTANCE; }

    private final List<DeferredAction> queue = new ArrayList<>();
    private boolean isRegistered = false;

    private DeferredActionTracker() {}

    public boolean hasPayloads() {
        return !queue.isEmpty();
    }

    public void enqueue(DeferredAction action) {
        if (!isRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
            isRegistered = true;
        }
        queue.add(action);
    }

    private void onTick(Minecraft client) {
        Iterator<DeferredAction> it = queue.iterator();
        while (it.hasNext()) {
            DeferredAction action = it.next();
            if (action.tick(client)) {
                it.remove();
            }
        }
    }
}
