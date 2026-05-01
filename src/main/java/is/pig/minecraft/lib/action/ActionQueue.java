package is.pig.minecraft.lib.action;
import is.pig.minecraft.api.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import java.util.Optional;

public class ActionQueue {
    private final Queue<Action> queue = new ConcurrentLinkedQueue<>();

    public void enqueue(Action action) {
        if (action != null) {
            queue.add(action);
        }
    }

    public void clear() {
        queue.clear();
    }

    public void tick(Minecraft client) {
        Action currentAction = queue.peek();
        if (currentAction != null) {
            Optional<Boolean> result = currentAction.execute(client);
            if (result.isPresent()) {
                queue.poll();
            }
        }
    }
}
