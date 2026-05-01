package is.pig.minecraft.lib.action.deferred;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
import net.minecraft.client.Minecraft;

public class RepeatingBreakBlockAction implements DeferredAction {
    private static final is.pig.minecraft.lib.util.PiggyLog LOGGER = new is.pig.minecraft.lib.util.PiggyLog("piggy-lib", "Deferred");
    private final BlockPos targetPos;
    private final int maxTicks;
    private final Runnable preBreakTask;
    private int ageTicks = 0;

    public RepeatingBreakBlockAction(BlockPos targetPos, int maxTicks, Runnable preBreakTask) {
        this.targetPos = targetPos;
        this.maxTicks = maxTicks;
        this.preBreakTask = preBreakTask;
        LOGGER.debug("Registered deferred BreakBlock. Pos: {} | Timeout: {}", targetPos, maxTicks);
    }

    public RepeatingBreakBlockAction(BlockPos targetPos, int maxTicks) {
        this(targetPos, maxTicks, null);
    }

    @Override
    public boolean tick(Minecraft client) {
        if (client.player == null || client.level == null) return true;
        
        ageTicks++;
        if (ageTicks > maxTicks) {
            LOGGER.debug("Deferred BreakBlock TIMEOUT for: {}", targetPos);
            return true; 
        }

        WorldStateAdapter adapter = PiggyServiceRegistry.getWorldStateAdapter();
        String worldId = client.level.dimension().location().toString();

        if (adapter.isEmpty(worldId, targetPos)) {
            LOGGER.debug("Target broken successfully. De-registering payload: {}", targetPos);
            return true;
        }

        if (!adapter.isPlayerOnGround(client.player)) return false;
        
        if (adapter.isEntityIntersecting(client.player, targetPos)) {
            return false;
        }
        
        Vec3 eyePos = adapter.getPlayerEyePosition(client.player);
        Vec3 blockCenter = new Vec3(targetPos.x() + 0.5, targetPos.y() + 0.5, targetPos.z() + 0.5);
        
        if (eyePos.distanceToSqr(blockCenter) >= 16.0) return false;

        if (!PiggyActionQueue.getInstance().hasActions("piggy-build")) {
            LOGGER.debug("Deferred BreakBlock condition met. Injecting action.");
            if (preBreakTask != null) {
                preBreakTask.run();
            }
            PiggyActionQueue.getInstance().enqueue(new BreakBlockAction(targetPos, "piggy-build", ActionPriority.LOW));
        }

        return false;
    }
}

