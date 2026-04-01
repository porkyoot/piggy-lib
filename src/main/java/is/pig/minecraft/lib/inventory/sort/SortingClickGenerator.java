package is.pig.minecraft.lib.inventory.sort;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.inventory.ClickWindowSlotAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates a minimal, slot-aware sequence of inventory clicks to transition a container
 * from its current state to a target sorted state using Permutation Cycle Decomposition.
 *
 * <p>Version 2.5: Fixed Bugs #2/#3/#4 — transfer() now:
 * <ul>
 *   <li>Respects slot capacity from {@link Slot#getMaxStackSize(ItemStack)} (Bug #2)</li>
 *   <li>Emits real container slot IDs via {@code slots[i].index}, not array indices (Bug #3)</li>
 *   <li>Moves exactly {@code desiredState[to].getCount()} items, not the full source stack,
 *       returning any cursor residual back to source with a 3rd click (Bug #4)</li>
 * </ul>
 */
public class SortingClickGenerator {

    public static final int NO_SLOT = -1;
    private static final int MAX_TOTAL_ACTIONS = 5000;

    private final ItemStack[] currentState;
    private final ItemStack[] desiredState;
    /** Real Minecraft slot objects — used for slot capacity and real slot index. */
    private final Slot[] slots;
    private final int externalBufferSlot; // REAL container slot index for evacuation
    private final int containerId;
    private final String sourceMod;
    private final ActionPriority priority;
    private final List<ClickWindowSlotAction> actions;
    private ItemStack cursorState = ItemStack.EMPTY;

    /**
     * @param currentState        Live snapshot of requested items.
     * @param desiredState        Target layout.
     * @param slots               Real Slot objects (mirrors currentState indices).
     * @param externalBufferSlot  A real container slot index that is GUARANTEED empty and NOT in the sort range.
     * @param containerId         For packet routing.
     * @param sourceMod           For attribution.
     * @param priority            For queueing.
     */
    public SortingClickGenerator(ItemStack[] currentState, ItemStack[] desiredState, Slot[] slots,
                                  int externalBufferSlot, int containerId, String sourceMod, ActionPriority priority) {
        if (currentState.length != desiredState.length) {
            throw new IllegalArgumentException("currentState and desiredState must be the same length");
        }
        if (slots != null && slots.length != currentState.length) {
            throw new IllegalArgumentException("slots array must be the same length as currentState");
        }
        this.currentState = cloneArray(currentState);
        this.desiredState = desiredState;
        this.slots = slots;
        this.externalBufferSlot = externalBufferSlot;
        this.containerId = containerId;
        this.sourceMod = sourceMod;
        this.priority = priority;
        this.actions = new ArrayList<>();
    }

    private ItemStack[] cloneArray(ItemStack[] source) {
        ItemStack[] copy = new ItemStack[source.length];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i] != null && !source[i].isEmpty() ? source[i].copy() : ItemStack.EMPTY;
        }
        return copy;
    }

    public List<ClickWindowSlotAction> generate() {
        this.actions.clear();
        resolveChains();
        resolveCycles();
        return new ArrayList<>(this.actions);
    }

    private void resolveChains() {
        boolean progressed;
        int safety = 0;
        do {
            progressed = false;
            progressed = false;
            for (int i = 0; i < currentState.length; i++) {
                if (needsItems(i)) {
                    int source = findSourceSlot(desiredState[i], i);
                    if (source != -1) {
                        transfer(source, i);
                        progressed = true;
                    }
                }
            }
            if (safety++ > currentState.length) break;
        } while (progressed && actions.size() < MAX_TOTAL_ACTIONS);
    }

    private boolean needsItems(int i) {
        if (desiredState[i].isEmpty()) return false;
        if (currentState[i].isEmpty()) return true;
        if (!ItemStack.isSameItemSameComponents(currentState[i], desiredState[i])) return false;
        return currentState[i].getCount() < desiredState[i].getCount();
    }

    private void resolveCycles() {
        int safety = 0;
        while (actions.size() < MAX_TOTAL_ACTIONS && safety++ < currentState.length * 2) {
            int startSlot = -1;
            for (int i = 0; i < currentState.length; i++) {
                if (!currentState[i].isEmpty() && !isSame(currentState[i], desiredState[i])) {
                    startSlot = i;
                    break;
                }
            }
            if (startSlot == -1) break;

            int internalBuffer = findSafeBufferSlot();
            if (internalBuffer != -1) {
                forceTransfer(startSlot, internalBuffer);
            } else if (externalBufferSlot != NO_SLOT) {
                // EXTREME CASE: Container is 100% full. Use external buffer to create a hole.
                evacuateToExternal(startSlot);
            } else {
                // No room at all. This shouldn't happen if the executor found an empty player slot.
                break; 
            }
            resolveChains();
        }
    }

    /**
     * Moves an item from a full container to an external player slot to create a "hole"
     * for cycle resolution.
     */
    private void evacuateToExternal(int arrayIndex) {
        if (currentState[arrayIndex].isEmpty()) return;
        
        ItemStack toEvacuate = currentState[arrayIndex].copy();

        // 1. Pick up from container
        emitClick(arrayIndex, true); // Cursor becomes toEvacuate
        
        // 2. Drop into external buffer
        actions.add(new ClickWindowSlotAction(containerId, externalBufferSlot, 0, ClickType.PICKUP, sourceMod, priority)
                .withExpectedCursorBefore(s -> ItemStack.isSameItemSameComponents(s, toEvacuate))
                .withExpectedCursorAfter(ItemStack::isEmpty));
        
        cursorState = ItemStack.EMPTY;
        currentState[arrayIndex] = ItemStack.EMPTY;
    }

    private void forceTransfer(int from, int to) {
        if (currentState[from].isEmpty()) return;
        
        ItemStack itemType = currentState[from].copy();
        int destSlotMax = (to == externalBufferSlot) ? itemType.getMaxStackSize() : slots[to].getMaxStackSize(itemType);
        int currentInDest = (to == externalBufferSlot) ? 0 : currentState[to].getCount();
        
        int toMove = Math.min(currentState[from].getCount(), destSlotMax - currentInDest);
        if (toMove <= 0) return;

        // Implementation borrows from transfer but uses toMove directly
        int moved = 0;
        int cursorMax = itemType.getMaxStackSize();
        while (moved < toMove && !currentState[from].isEmpty()) {
            int pickupAmount = Math.min(currentState[from].getCount(), cursorMax);
            emitClick(from, true);
            currentState[from].shrink(pickupAmount);
            if (currentState[from].isEmpty()) currentState[from] = ItemStack.EMPTY;

            int depositAmount = Math.min(pickupAmount, toMove - moved);
            emitClick(to, false);
            if (depositAmount > 0) {
                if (to != externalBufferSlot) {
                    if (currentState[to].isEmpty()) {
                        currentState[to] = itemType.copyWithCount(depositAmount);
                    } else {
                        currentState[to].grow(depositAmount);
                    }
                }
            }

            int leftover = pickupAmount - depositAmount;
            if (leftover > 0) {
                emitClick(from, false);
                if (currentState[from].isEmpty()) {
                    currentState[from] = itemType.copyWithCount(leftover);
                } else {
                    currentState[from].grow(leftover);
                }
            }
            moved += depositAmount;
            if (depositAmount == 0) break;
        }
    }

    private void transfer(int from, int to) {
        if (currentState[from].isEmpty()) return;

        ItemStack itemType = currentState[from].copy();
        int cursorMax = itemType.getMaxStackSize();
        int destSlotMax = slots[to].getMaxStackSize(itemType);

        int needed = desiredState[to].getCount() - currentState[to].getCount();
        if (needed <= 0) return;
        int toMove = Math.min(needed, currentState[from].getCount());

        int moved = 0;
        while (moved < toMove && !currentState[from].isEmpty()) {
            int pickupAmount = Math.min(currentState[from].getCount(), cursorMax);

            // 1. Pick up
            emitClick(from, true);
            currentState[from].shrink(pickupAmount);
            if (currentState[from].isEmpty()) currentState[from] = ItemStack.EMPTY;

            int spaceInDest = destSlotMax - currentState[to].getCount();
            int depositAmount = Math.min(pickupAmount, Math.min(spaceInDest, toMove - moved));

            // 2. Deposit
            emitClick(to, false);
            if (depositAmount > 0) {
                if (currentState[to].isEmpty()) {
                    currentState[to] = itemType.copyWithCount(depositAmount);
                } else {
                    currentState[to].grow(depositAmount);
                }
            }

            int leftover = pickupAmount - depositAmount;
            if (leftover > 0) {
                // 3. Return residual
                emitClick(from, false);
                if (currentState[from].isEmpty()) {
                    currentState[from] = itemType.copyWithCount(leftover);
                } else {
                    currentState[from].grow(leftover);
                }
            }

            moved += depositAmount;
            if (depositAmount == 0) break; 
        }
    }

    /**
     * Emits a click with full cursor-awareness.
     * 
     * @param arrayIndex The internal slot index.
     * @param isPickup If true, we expect to pick up items (cursor empty before).
     *                 If false, we expect to drop items (cursor full before).
     */
    private void emitClick(int arrayIndex, boolean isPickup) {
        ItemStack beforeCursor = cursorState.copy();
        
        // Update simulation cursor state
        if (isPickup) {
            cursorState = currentState[arrayIndex].copy();
            // Note: currentState[arrayIndex] should be shrunk BY THE CALLER after this
        } else {
            // Dropping items from cursor to slot
            // Since we use PICKUP mode everywhere, the new cursor will be whatever was 
            // leftover or exchanged.
            // Simplified for this robust model: transfer() handles the count logic.
            cursorState = ItemStack.EMPTY; 
        }

        ItemStack afterCursor = cursorState.copy();

        actions.add(new ClickWindowSlotAction(containerId, slots[arrayIndex].index, 0, ClickType.PICKUP, sourceMod, priority)
                .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursor))
                .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursor)));
    }

    private boolean isSameAndEqual(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
    }

    private int findSourceSlot(ItemStack needed, int targetSlot) {
        for (int i = 0; i < currentState.length; i++) {
            if (i == targetSlot) continue;
            if (currentState[i].isEmpty()) continue;
            if (isSame(currentState[i], desiredState[i])) continue;
            // Use item-only matching (ignoring count) so residuals can satisfy any target of the same type.
            if (ItemStack.isSameItemSameComponents(currentState[i], needed)) return i;
        }
        return -1;
    }

    private int findSafeBufferSlot() {
        for (int i = 0; i < currentState.length; i++) {
            if (currentState[i].isEmpty()) return i;
        }
        return -1;
    }

    private boolean isSame(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
    }
}
