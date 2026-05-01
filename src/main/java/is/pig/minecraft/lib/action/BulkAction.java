package is.pig.minecraft.lib.action;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class BulkAction implements Action {
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "ActionQueue");

    private final String sourceMod;
    private final ActionPriority priority;
    private final List<Action> subActions;
    private final BooleanSupplier verifyCondition;
    private final int timeoutTicks;
    private final String name;
    private final BulkActionCallback callback;

    private boolean initiated = false;
    private boolean allExecuted = false;
    private int currentIndex = 0;
    private int ticksSinceLastSubAction = 0;
    private int waitTicks = 0;
    private boolean ignoreGlobalCps = false;

    public BulkAction(String sourceMod, ActionPriority priority, List<Action> subActions, BooleanSupplier verifyCondition, int timeoutTicks, String name, BulkActionCallback callback) {
        this.sourceMod = sourceMod;
        this.priority = priority;
        this.subActions = subActions;
        this.verifyCondition = verifyCondition;
        this.timeoutTicks = timeoutTicks;
        this.name = name;
        this.callback = callback;
    }

    public BulkAction(String sourceMod, ActionPriority priority, List<Action> subActions, BooleanSupplier verifyCondition, int timeoutTicks, String name) {
        this(sourceMod, priority, subActions, verifyCondition, timeoutTicks, name, null);
    }

    public BulkAction(String sourceMod, String name, List<Action> subActions) {
        this(sourceMod, ActionPriority.NORMAL, subActions, () -> true, 40, name, null);
    }
    
    public BulkAction(String sourceMod, String name, List<Action> subActions, BulkActionCallback callback) {
        this(sourceMod, ActionPriority.NORMAL, subActions, () -> true, 40, name, callback);
    }

    public BulkAction(String sourceMod, String name, List<Action> subActions, BooleanSupplier verifyCondition) {
        this(sourceMod, ActionPriority.NORMAL, subActions, verifyCondition, 40, name, null);
    }

    private void finalizeAction(Minecraft client, boolean isTimeout) {
        if (callback != null) {
            java.util.List<Action> failed = new java.util.ArrayList<>();
            for (Action action : subActions) {
                if (!action.isVerified(client)) {
                    failed.add(action);
                }
            }
            callback.onComplete(failed.isEmpty(), failed);
        }
    }

    @Override
    public Optional<Boolean> execute(Object clientObj) {
        Minecraft client = (Minecraft) clientObj;
        initiated = true;

        if (!allExecuted) {
            while (currentIndex < subActions.size()) {
                Action currentSub = subActions.get(currentIndex);

                boolean shouldThrottle = currentSub.isClick() && !this.ignoreGlobalCps() && !currentSub.ignoreGlobalCps() && this.getPriority() != ActionPriority.HIGHEST;
                int cps = is.pig.minecraft.lib.config.PiggyClientConfig.getInstance().globalActionCps;

                if (shouldThrottle && cps > 0) {
                    int requiredDelayTicks = Math.max(1, 20 / cps);
                    if (ticksSinceLastSubAction < requiredDelayTicks) {
                        ticksSinceLastSubAction++;
                        return Optional.empty(); 
                    }
                }

                // Fire the sub action once to initiate its effects
                currentSub.execute(client);
                ticksSinceLastSubAction = 0;
                currentIndex++;

                if (shouldThrottle && cps > 0) {
                    return Optional.empty(); // Yield tick to enforce CPS delay
                }
            }
            
            allExecuted = true;
            if (verifyCondition.getAsBoolean()) {
                finalizeAction(client, false);
                return Optional.of(true);
            }
            return Optional.empty();
        }

        // Verification Phase
        waitTicks++;
        if (verifyCondition.getAsBoolean()) {
            finalizeAction(client, false);
            return Optional.of(true);
        }

        if (waitTicks >= timeoutTicks) {
            // Check if any failed or if it's purely a timeout
            finalizeAction(client, true);
            
            // If the timeout expires but there's no callback evaluating success, still trace it if not suppressed
            if (callback == null) {
                LOGGER.warn("BulkAction '{}' from '{}' timed out waiting for composite verification", getName(), getSourceMod());
            }
            return Optional.of(false);
        }

        return Optional.empty();
    }

    public void setIgnoreGlobalCps(boolean ignoreGlobalCps) {
        this.ignoreGlobalCps = ignoreGlobalCps;
    }

    @Override
    public boolean ignoreGlobalCps() {
        return ignoreGlobalCps;
    }

    @Override
    public ActionPriority getPriority() {
        return priority;
    }

    @Override
    public String getSourceMod() {
        return sourceMod;
    }

    @Override
    public boolean isInitiated() {
        return initiated;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isClick() {
        // Bulk actions generally compose clicks or block places that need rate limiting.
        return true; 
    }

    @Override
    public String getTelemetry(Object clientObj) {
        return String.format("Progress=%d/%d, WaitTicks=%d, Timeout=%d", currentIndex, subActions.size(), waitTicks, timeoutTicks);
    }
}
