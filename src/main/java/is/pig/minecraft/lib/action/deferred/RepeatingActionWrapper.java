package is.pig.minecraft.lib.action.deferred;

import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.IAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.function.Supplier;

public class RepeatingActionWrapper implements IDeferredAction {
    private static final is.pig.minecraft.lib.util.PiggyLog LOGGER = new is.pig.minecraft.lib.util.PiggyLog("piggy-lib", "Deferred");
    private final BlockPos targetPos;
    private final int maxTicks;
    private final Runnable preTask;
    private final Supplier<IAction> actionFactory;
    private final Supplier<Boolean> finishCondition;
    private final java.util.function.Predicate<Minecraft> contextValidator;
    private int ageTicks = 0;

    public RepeatingActionWrapper(BlockPos targetPos, int maxTicks, Runnable preTask, Supplier<IAction> actionFactory, Supplier<Boolean> finishCondition, java.util.function.Predicate<Minecraft> contextValidator) {
        this.targetPos = targetPos;
        this.maxTicks = maxTicks;
        this.preTask = preTask;
        this.actionFactory = actionFactory;
        this.finishCondition = finishCondition;
        this.contextValidator = contextValidator;
        LOGGER.info("Registered new Generic Contextual payload! Pos: " + targetPos + " | Timeout: " + maxTicks);
    }

    @Override
    public boolean tick(Minecraft client) {
        if (client.player == null || client.level == null) return true;
        
        ageTicks++;
        if (ageTicks > maxTicks) {
            LOGGER.info("Contextual Tracker TIMEOUT expired for Block: " + targetPos);
            return true; 
        }

        if (ageTicks > 15 && finishCondition.get()) {
            LOGGER.info("Target logically fulfilled! De-registering payload: " + targetPos);
            return true;
        }

        if (this.contextValidator != null && !this.contextValidator.test(client)) {
            return false;
        }

        double distSq = client.player.getEyePosition().distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(targetPos));
        if (distSq >= 16.0) return false;

        if (!PiggyActionQueue.getInstance().hasActions("piggy-build")) {
            LOGGER.info("Tracker Condition Met! DistanceSq: " + distSq + " | Injecting Payload natively.");
            if (preTask != null) {
                preTask.run();
            }
            PiggyActionQueue.getInstance().enqueue(actionFactory.get());
        }

        return false;
    }
}
