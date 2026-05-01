package is.pig.minecraft.lib.action.deferred;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.WorldStateAdapter;
import is.pig.minecraft.lib.action.PiggyActionQueue;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class RepeatingActionWrapper implements DeferredAction {
    private static final is.pig.minecraft.lib.util.PiggyLog LOGGER = new is.pig.minecraft.lib.util.PiggyLog("piggy-lib", "Deferred");
    private final BlockPos targetPos;
    private final int maxTicks;
    private final Runnable preTask;
    private final Supplier<Action> actionFactory;
    private final Supplier<Boolean> finishCondition;
    private final Predicate<Object> contextValidator;
    private int ageTicks = 0;

    public RepeatingActionWrapper(BlockPos targetPos, int maxTicks, Runnable preTask, Supplier<Action> actionFactory, Supplier<Boolean> finishCondition, Predicate<Object> contextValidator) {
        this.targetPos = targetPos;
        this.maxTicks = maxTicks;
        this.preTask = preTask;
        this.actionFactory = actionFactory;
        this.finishCondition = finishCondition;
        this.contextValidator = contextValidator;
        LOGGER.debug("Registered deferred generic action. Pos: {} | Timeout: {}", targetPos, maxTicks);
    }

    @Override
    public boolean tick(Object client) {
        WorldStateAdapter worldState = PiggyServiceRegistry.getWorldStateAdapter();
        
        ageTicks++;
        if (ageTicks > maxTicks) {
            LOGGER.debug("Deferred generic action TIMEOUT for: {}", targetPos);
            return true; 
        }

        if (ageTicks > 15 && finishCondition.get()) {
            LOGGER.debug("Deferred action fulfilled. De-registering: {}", targetPos);
            return true;
        }

        if (this.contextValidator != null && !this.contextValidator.test(client)) {
            return false;
        }

        Vec3 eyePos = worldState.getPlayerEyePosition(client);
        Vec3 targetCenter = new Vec3(targetPos.x() + 0.5, targetPos.y() + 0.5, targetPos.z() + 0.5);
        double distSq = eyePos.distanceToSqr(targetCenter);
        
        if (distSq >= 16.0) return false;

        if (!PiggyActionQueue.getInstance().hasActions("piggy-build")) {
            LOGGER.debug("Deferred generic action condition met. Injecting payload.");
            if (preTask != null) {
                preTask.run();
            }
            PiggyActionQueue.getInstance().enqueue(actionFactory.get());
        }

        return false;
    }
}
