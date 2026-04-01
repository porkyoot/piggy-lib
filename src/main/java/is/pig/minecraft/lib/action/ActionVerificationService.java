package is.pig.minecraft.lib.action;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

/**
 * A background service that manages "Expected States" and monitors inventory invariants 
 * without blocking the Minecraft main thread. It performs "lazy" verification by analyzing 
 * snapshots off-thread, providing total thread safety for the UI loop.
 * 
 * <p>Version 3.0: Off-Thread Robust Verification Pipeline Engaged.
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

    private record ExpectedSlotState(int slotId, ItemStack expectedItem, long expiryTime, ActionCallback onFail) {}

    private ActionVerificationService() {}

    public static ActionVerificationService getInstance() { return INSTANCE; }

    /**
     * Registers an expectation for a specific slot to be verified lazily.
     * 
     * @param slotId The container slot ID.
     * @param expected The item expected to be in that slot.
     * @param timeoutTicks How long to wait before declaring a timeout (fail).
     * @param onFail Callback triggered if verification fails.
     */
    public void expect(int slotId, ItemStack expected, int timeoutTicks, ActionCallback onFail) {
        long expiryTime = System.currentTimeMillis() + (timeoutTicks * 50); // 50ms per tick
        expectations.put(slotId, new ExpectedSlotState(slotId, expected.copy(), expiryTime, onFail));
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

        long now = System.currentTimeMillis();

        // 1. Snapshot the relevant slots from the Main Thread safely
        // Note: we MUST jump to the main thread for the read part!
        CompletableFuture.runAsync(() -> {
            for (Map.Entry<Integer, ExpectedSlotState> entry : expectations.entrySet()) {
                int slotId = entry.getKey();
                ExpectedSlotState expected = entry.getValue();

                if (now > expected.expiryTime()) {
                    expectations.remove(slotId);
                    expected.onFail().onResult(false); // Timeout also equals failure
                    continue;
                }

                // Verify the slot in the main menu
                if (slotId >= 0 && slotId < client.player.containerMenu.slots.size()) {
                    ItemStack observed = client.player.containerMenu.getSlot(slotId).getItem();
                    if (ItemStack.isSameItemSameComponents(observed, expected.expectedItem()) &&
                        observed.getCount() == expected.expectedItem().getCount()) {
                        // Success!
                        expectations.remove(slotId);
                        // No need for a success callback for lazy verification usually, 
                        // as we only trigger recovery on failure.
                    }
                }
            }
        }, client::execute).join(); // We block the background worker, NOT the main thread.
    }
}
