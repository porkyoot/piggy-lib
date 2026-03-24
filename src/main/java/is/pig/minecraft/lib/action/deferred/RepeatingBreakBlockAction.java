package is.pig.minecraft.lib.action.deferred;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.world.BreakBlockAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class RepeatingBreakBlockAction implements IDeferredAction {
    private static final is.pig.minecraft.lib.util.PiggyLog LOGGER = new is.pig.minecraft.lib.util.PiggyLog("piggy-lib", "Deferred");
    private final BlockPos targetPos;
    private final int maxTicks;
    private final Runnable preBreakTask;
    private int ageTicks = 0;

    public RepeatingBreakBlockAction(BlockPos targetPos, int maxTicks, Runnable preBreakTask) {
        this.targetPos = targetPos;
        this.maxTicks = maxTicks;
        this.preBreakTask = preBreakTask;
        LOGGER.info("Registered new Contextual BreakBlock payload! Pos: " + targetPos + " | Timeout: " + maxTicks);
    }

    public RepeatingBreakBlockAction(BlockPos targetPos, int maxTicks) {
        this(targetPos, maxTicks, null);
    }

    @Override
    public boolean tick(Minecraft client) {
        if (client.player == null || client.level == null) return true; // Abort natively on logout
        
        ageTicks++;
        if (ageTicks > maxTicks) {
            LOGGER.info("Contextual Tracker TIMEOUT expired for Block: " + targetPos);
            return true; 
        }

        // Success check!
        if (client.level.isEmptyBlock(targetPos)) {
            LOGGER.info("Target organically broken! De-registering payload: " + targetPos);
            return true; // Successfully broken natively!
        }

        // Context Trigger Validation
        if (!client.player.onGround()) return false;
        
        // Ensure player is not actively standing on the target block organically (e.g. at the bottom apex of a slime bounce)
        net.minecraft.world.phys.AABB extendedBlockBox = new net.minecraft.world.phys.AABB(targetPos).expandTowards(0, 0.1, 0);
        if (client.player.getBoundingBox().intersects(extendedBlockBox)) {
            return false; // Still standing on or intersecting the MLG block!
        }
        
        double distSq = client.player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(targetPos));
        if (distSq >= 16.0) return false;

        // Ensure the main queue isn't already busy handling our exact request organically
        if (!PiggyActionQueue.getInstance().hasActions("piggy-build")) {
            LOGGER.info("Tracker Condition Met! DistanceSq: " + distSq + " | Injecting Block Break Action natively.");
            if (preBreakTask != null) {
                preBreakTask.run();
            }
            // Queue is free, context is perfect: retry the manual payload!
            PiggyActionQueue.getInstance().enqueue(new BreakBlockAction(targetPos, "piggy-build", ActionPriority.LOW));
        }

        return false; // Remain alive waiting for block to dissolve
    }
}
