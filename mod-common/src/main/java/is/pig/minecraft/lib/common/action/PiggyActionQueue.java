package is.pig.minecraft.lib.common.action;

import is.pig.minecraft.lib.api.IPlayerController;
import java.util.Optional;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PiggyActionQueue {
    private static final PiggyActionQueue INSTANCE = new PiggyActionQueue();

    private final PriorityBlockingQueue<PrioritizedAction> queue = new PriorityBlockingQueue<>();
    private final AtomicLong sequenceNumber = new AtomicLong(0);
    private int ticksSinceLastClick = 100;

    private PiggyActionQueue() {}

    public static PiggyActionQueue getInstance() { return INSTANCE; }

    public void enqueue(IAction action) {
        queue.put(new PrioritizedAction(action, sequenceNumber.getAndIncrement()));
    }

    public void clear(String sourceMod) {
        queue.removeIf(pa -> pa.action().getSourceMod().equals(sourceMod));
    }

    public void tick(IPlayerController controller) {
        ticksSinceLastClick++;
        
        while (!queue.isEmpty()) {
            PrioritizedAction top = queue.peek();
            IAction action = top.action();

            if (isCpsRateLimited(action)) {
                break;
            }

            boolean justInitiated = !action.isInitiated();
            Optional<Boolean> result = action.execute(controller);
            
            if (justInitiated && action.isClick() && action.isInitiated()) {
                ticksSinceLastClick = 0;
            }

            if (result.isPresent()) {
                queue.poll();
                boolean success = result.get();
                action.getCallback().ifPresent(cb -> cb.onResult(success));
            } else {
                break;
            }
        }
    }

    private boolean isCpsRateLimited(IAction action) {
        if (!action.isClick() || action.isInitiated() || action.getPriority() == ActionPriority.HIGHEST || action.ignoreGlobalCps()) {
            return false;
        }
        int requiredDelayTicks = 1; // Simplified for pure Java
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
