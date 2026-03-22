package is.pig.minecraft.lib.action;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;

public class ActionQueue {
    private final Queue<IAction> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(IAction action) {
        if (action != null) {
            queue.add(action);
        }
    }

    public void clear() {
        queue.clear();
    }

    public void tick(Minecraft client) {
        IAction currentAction = queue.peek();
        if (currentAction != null) {
            if (currentAction.execute(client)) {
                queue.poll();
            }
        }
    }
}
