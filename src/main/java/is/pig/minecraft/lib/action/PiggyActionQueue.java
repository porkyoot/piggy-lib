package is.pig.minecraft.lib.action;

import is.pig.minecraft.lib.config.PiggyClientConfig;
import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PiggyActionQueue {
    private static final PiggyActionQueue INSTANCE = new PiggyActionQueue();
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "ActionQueue");

    private final PriorityBlockingQueue<PrioritizedAction> queue = new PriorityBlockingQueue<>();
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private int ticksSinceLastClick = 100;
    private boolean suppressAutoRefill = false;

    public void setSuppressAutoRefill(boolean suppressAutoRefill) {
        this.suppressAutoRefill = suppressAutoRefill;
    }

    public boolean isSuppressAutoRefill() {
        return suppressAutoRefill;
    }

    private PiggyActionQueue() {}

    public static PiggyActionQueue getInstance() { return INSTANCE; }

    public void enqueue(IAction action) {
        queue.put(new PrioritizedAction(action, sequenceNumber.getAndIncrement()));
    }

    public void clear(String sourceMod) {
        queue.removeIf(pa -> pa.action().getSourceMod().equals(sourceMod));
    }

    public boolean hasActions(String sourceMod) {
        return queue.stream().anyMatch(pa -> pa.action().getSourceMod().equals(sourceMod));
    }

    public void tick(Minecraft client) {
        is.pig.minecraft.lib.util.perf.PerfMonitor.getInstance().tick(client);
        is.pig.minecraft.lib.util.telemetry.MetaActionSessionManager.getInstance().tick(client);
        ticksSinceLastClick++;
        
        while (!queue.isEmpty()) {
            PrioritizedAction top = queue.peek();
            IAction action = top.action();

            // 1. CPS Rate Limiter Gate
            if (isCpsRateLimited(action)) {
                break; // Stop processing this tick, wait for next
            }

            // 2. Execution
            boolean justInitiated = !action.isInitiated();
            Optional<Boolean> result = action.execute(client);
            
            if (justInitiated && action.isClick() && action.isInitiated()) {
                ticksSinceLastClick = 0;
                is.pig.minecraft.lib.util.perf.PerfMonitor.getInstance().recordAction();
            }

            // 3. Result Evaluation
            if (result.isPresent()) {
                queue.poll(); // Remove finished/failed action
                if (result.get()) {
                    LOGGER.debug("Completed action '{}'", action.getName());
                }
            } else {
                break; // Action is waiting for verification. Block queue.
            }
        }
    }

    private boolean isCpsRateLimited(IAction action) {
        if (!action.isClick() || action.isInitiated() || 
            action.getPriority() == ActionPriority.HIGHEST || action.ignoreGlobalCps()) {
            return false; // Fast-pass
        }
        
        int cps = PiggyClientConfig.getInstance().globalActionCps;
        if (cps <= 0) return false; // 0 = Unlimited
        
        int requiredDelayTicks = Math.max(1, 20 / cps);
        return ticksSinceLastClick < requiredDelayTicks;
    }

    private record PrioritizedAction(IAction action, long sequence) implements Comparable<PrioritizedAction> {
        @Override
        public int compareTo(PrioritizedAction o) {
            int priorityComparison = this.action.getPriority().compareTo(o.action.getPriority());
            if (priorityComparison != 0) {
                return priorityComparison;
            }
            return Long.compare(this.sequence, o.sequence);
        }
    }
}