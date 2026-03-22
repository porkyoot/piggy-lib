package is.pig.minecraft.lib.action;

import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class PiggyActionQueue {
    private static final PiggyActionQueue INSTANCE = new PiggyActionQueue();
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "ActionQueue");

    private final PriorityBlockingQueue<PrioritizedAction> queue = new PriorityBlockingQueue<>();
    private final AtomicLong sequenceNumber = new AtomicLong(0);

    private PiggyActionQueue() {}

    public static PiggyActionQueue getInstance() {
        return INSTANCE;
    }

    public void enqueue(IAction action) {
        queue.put(new PrioritizedAction(action, sequenceNumber.getAndIncrement()));
        LOGGER.info("Enqueued [{}] action '{}' from '{}'", action.getPriority().name(), action.getName(), action.getSourceMod());
    }

    public void clear(String sourceMod) {
        queue.removeIf(pa -> pa.action.getSourceMod().equals(sourceMod));
    }

    private int ticksSinceLastClick = 100;

    public void tick(Minecraft client) {
        ticksSinceLastClick++;
        PrioritizedAction top = queue.peek();
        if (top != null) {
            IAction action = top.action();
            
            if (action.isClick() && !action.isInitiated()) {
                if (action.getPriority() != ActionPriority.HIGHEST) {
                    int requiredDelayTicks = 20 / Math.max(1, is.pig.minecraft.lib.config.PiggyClientConfig.getInstance().globalActionCps);
                    if (ticksSinceLastClick < requiredDelayTicks) {
                        return;
                    }
                }
            }

            boolean wasInitiated = action.isInitiated();
            boolean done = action.execute(client);
            
            if (action.isClick() && !wasInitiated && action.isInitiated()) {
                ticksSinceLastClick = 0;
            }

            if (done) {
                queue.poll();
                LOGGER.info("Completed action '{}'", action.getName());
            }
        }
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
