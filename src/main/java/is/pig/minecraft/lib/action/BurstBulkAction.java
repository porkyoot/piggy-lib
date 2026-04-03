package is.pig.minecraft.lib.action;

import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * A specialized bulk action that streams logic in bursts using AIMD.
 * This generalizes the "Wait for Sync" loop used in robust sorting.
 */
public class BurstBulkAction implements IAction {
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "ActionQueue");

    private final String sourceMod;
    private final ActionPriority priority;
    private final String name;
    private final Supplier<List<IAction>> planProvider;
    private final BooleanSupplier verifyCondition;
    private final IntSupplier latencySupplier;
    private final int timeoutTicks;
    private final ActionCallback callback;
    
    private final BurstController burstController = new BurstController();
    private boolean initiated = false;
    
    private List<IAction> currentBurst = null;
    private int burstIndex = 0;
    private int waitTicks = 0;
    private int consecutiveDesyncs = 0;
    private static final int MAX_CONSECUTIVE_DESYNCS = 10;
    private State state = State.PLANNING;

    private enum State {
        PLANNING,      // Calculating the next burst
        EXECUTING,     // Sending the current batch of packets
        VERIFYING      // Waiting for server sync after a burst
    }

    public BurstBulkAction(String sourceMod, ActionPriority priority, String name, 
                           Supplier<List<IAction>> planProvider, 
                           BooleanSupplier verifyCondition, 
                           IntSupplier latencySupplier,
                           int timeoutTicks,
                           ActionCallback callback) {
        this.sourceMod = sourceMod;
        this.priority = priority;
        this.name = name;
        this.planProvider = planProvider;
        this.verifyCondition = verifyCondition;
        this.latencySupplier = latencySupplier;
        this.timeoutTicks = timeoutTicks;
        this.callback = callback;
    }

    @Override
    public Optional<Boolean> execute(Minecraft client) {
        initiated = true;

        switch (state) {
            case PLANNING -> {
                List<IAction> fullPlan = planProvider.get();
                if (fullPlan == null || fullPlan.isEmpty()) {
                    if (callback != null) callback.onResult(true);
                    return Optional.of(true);
                }

                int window = burstController.getCurrentWindow();
                int burstSize = Math.min(fullPlan.size(), window);
                this.currentBurst = new java.util.ArrayList<>(fullPlan.subList(0, burstSize));
                this.burstIndex = 0;
                
                state = State.EXECUTING;
                return Optional.empty(); // Wait for next tick to start burst
            }
            case EXECUTING -> {
                if (currentBurst == null || burstIndex >= currentBurst.size()) {
                    state = State.VERIFYING;
                    waitTicks = 0;
                    return Optional.empty(); 
                }

                // Fire the entire burst in this tick
                while (burstIndex < currentBurst.size()) {
                    IAction sub = currentBurst.get(burstIndex++);
                    sub.execute(client);
                }
                
                state = State.VERIFYING;
                waitTicks = 0;
                return Optional.empty(); // Yield for server sync
            }
            case VERIFYING -> {
                waitTicks++;
                
                if (verifyCondition.getAsBoolean()) {
                    LOGGER.debug("BurstBulkAction burst verified. Proceeding.");
                    burstController.reportSuccess(latencySupplier.getAsInt());
                    this.consecutiveDesyncs = 0; // Reset on progress
                    state = State.PLANNING;
                    return Optional.empty(); // Wait for next tick to re-plan
                }

                if (waitTicks >= timeoutTicks) {
                    this.consecutiveDesyncs++;
                    if (this.consecutiveDesyncs >= MAX_CONSECUTIVE_DESYNCS) {
                        LOGGER.error("BurstBulkAction '{}' aborted: Too many consecutive desyncs ({}). Possible stuck logic.", name, consecutiveDesyncs);
                        if (callback != null) callback.onResult(false);
                        return Optional.of(false);
                    }
                    
                    LOGGER.warn("BurstBulkAction '{}' timed out ({}). Decreasing window.", name, consecutiveDesyncs);
                    burstController.reportDesync();
                    state = State.PLANNING; // Re-plan from the drift
                    return Optional.empty();
                }

                return Optional.empty(); // Still waiting for verifyCondition
            }
        }
        
        return Optional.empty();
    }

    @Override
    public ActionPriority getPriority() { return priority; }

    @Override
    public String getSourceMod() { return sourceMod; }

    @Override
    public String getName() { return name; }

    @Override
    public boolean isInitiated() { return initiated; }

    @Override
    public boolean isClick() { return true; }

    @Override
    public Optional<ActionCallback> getCallback() { return Optional.ofNullable(callback); }
}
