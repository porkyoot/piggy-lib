package is.pig.minecraft.lib.action;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.ItemDataAdapter;
import net.minecraft.client.Minecraft;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

/**
 * A background service that manages "Expected States" and monitors inventory invariants 
 * without blocking the Minecraft main thread. It performs "lazy" verification by analyzing 
 * snapshots off-thread, providing total thread safety for the UI loop.
 */
public class ActionVerificationService {
    private static final ActionVerificationService INSTANCE = new ActionVerificationService();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "PiggyActionVerificationWatcher");
        t.setDaemon(true);
        return t;
    });

    private final Map<Integer, ExpectedSlotState> expectations = new ConcurrentHashMap<>();
    private final AtomicBoolean isWatcherRunning = new AtomicBoolean(false);

    private record ExpectedSlotState(int slotId, Object expectedItem, long expiryTime, ActionCallback onFail) {}

    private ActionVerificationService() {}

    public static ActionVerificationService getInstance() { return INSTANCE; }

    /**
     * Registers an expectation for a specific slot to be verified lazily.
     * 
     * @param slotId The container slot ID.
     * @param expected The item expected to be in that slot (ItemStack Object).
     * @param timeoutTicks How long to wait before declaring a timeout (fail).
     * @param onFail Callback triggered if verification fails.
     */
    public void expect(int slotId, Object expected, int timeoutTicks, ActionCallback onFail) {
        ItemDataAdapter adapter = PiggyServiceRegistry.getItemDataAdapter();
        long expiryTime = System.currentTimeMillis() + (timeoutTicks * 50); // 50ms per tick
        expectations.put(slotId, new ExpectedSlotState(slotId, adapter.copy(expected), expiryTime, onFail));
        if (isWatcherRunning.compareAndSet(false, true)) {
            startWorker();
        }
    }

    private void startWorker() {
        executor.scheduleWithFixedDelay(this::verifyLoop, 50, 50, TimeUnit.MILLISECONDS);
    }

    private void verifyLoop() {
        if (expectations.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        ItemDataAdapter adapter = PiggyServiceRegistry.getItemDataAdapter();
        long now = System.currentTimeMillis();

        CompletableFuture.runAsync(() -> {
            for (Map.Entry<Integer, ExpectedSlotState> entry : expectations.entrySet()) {
                int slotId = entry.getKey();
                ExpectedSlotState expected = entry.getValue();

                if (now > expected.expiryTime()) {
                    expectations.remove(slotId);
                    expected.onFail().onResult(false);
                    continue;
                }

                if (slotId >= 0 && slotId < client.player.containerMenu.slots.size()) {
                    Object observed = client.player.containerMenu.getSlot(slotId).getItem();
                    if (adapter.areItemsEqual(observed, expected.expectedItem()) &&
                        adapter.getCount(observed) == adapter.getCount(expected.expectedItem())) {
                        expectations.remove(slotId);
                    }
                }
            }
        }, client::execute).join();
    }
}
