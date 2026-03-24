package is.pig.minecraft.lib.action;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import java.util.Optional;

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
            Optional<Boolean> result = currentAction.execute(client);
            if (result.isPresent()) {
                queue.poll();
            }
        }
    }
}
