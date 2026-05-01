package is.pig.minecraft.lib.action.deferred;
import is.pig.minecraft.api.*;

import net.minecraft.client.Minecraft;

public interface DeferredAction {
    /**
     * Called every tick by the DeferredActionTracker.
     * @param client The Minecraft client context.
     * @return true if the action has completed its lifecycle (or perfectly timed out) and should be dequeued natively.
     */
    boolean tick(Minecraft client);
}
