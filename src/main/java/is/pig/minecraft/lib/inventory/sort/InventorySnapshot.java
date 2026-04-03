package is.pig.minecraft.lib.inventory.sort;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * An immutable snapshot of the inventory state, used as a "Shadow Inventory" 
 * for mathematical plan verification.
 */
public record InventorySnapshot(
        int containerId,
        List<SlotState> slots,
        ItemStack cursor
) {
    /**
     * Represents the state of a single slot.
     */
    public record SlotState(int index, ItemStack stack) {
        public SlotState copy() {
            return new SlotState(index, stack.copy());
        }
    }

    /**
     * Captures the current live state from the Minecraft client.
     */
    public static InventorySnapshot capture(Minecraft client) {
        if (client.player == null) {
            return new InventorySnapshot(0, List.of(), ItemStack.EMPTY);
        }

        int containerId = client.player.containerMenu.containerId;
        List<SlotState> slotStates = new ArrayList<>();

        for (Slot slot : client.player.containerMenu.slots) {
            // Include all slots from the container menu
            slotStates.add(new SlotState(slot.index, slot.getItem().copy()));
        }

        ItemStack cursor = client.player.containerMenu.getCarried().copy();
        return new InventorySnapshot(containerId, List.copyOf(slotStates), cursor);
    }

    /**
     * Predicts the state after applying a sequence of abstract mathematical moves.
     * Adheres to Minecraft's client-side item physics (maxStackSize, merging).
     *
     * @param moves The sequence of moves to apply.
     * @return A new InventorySnapshot representing the predicted state.
     */
    public InventorySnapshot applyMoves(List<Move> moves) {
        return applyMoves(moves, ItemStack::getMaxStackSize);
    }
    
    /**
     * Predicts the state after applying abstract moves, using a custom capacity provider.
     */
    public InventorySnapshot applyMoves(List<Move> moves, java.util.function.ToIntFunction<ItemStack> capacityProvider) {
        // Use a map internally for O(1) slot access during simulation
        Map<Integer, ItemStack> workingSlots = new HashMap<>();
        for (SlotState state : slots) {
            workingSlots.put(state.index, state.stack().copy());
        }
        ItemStack workingCursor = cursor.copy();

        for (Move move : moves) {
            int idx = move.slotIndex();
            ItemStack slotStack = workingSlots.getOrDefault(idx, ItemStack.EMPTY).copy();
            int maxSlotCap = capacityProvider.applyAsInt(slotStack);
            // Standard cursor capacity is always 64 in modern Minecraft.
            int maxCursorCap = Math.min(64, slotStack.getMaxStackSize());
            if (maxCursorCap <= 0) maxCursorCap = 64;

            switch (move.type()) {
                case PICKUP_ALL -> {
                    if (workingCursor.isEmpty()) {
                        int amount = Math.min(slotStack.getCount(), maxCursorCap);
                        workingCursor = slotStack.copyWithCount(amount);
                        workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() - amount));
                    } else if (ItemStack.isSameItemSameComponents(workingCursor, slotStack)) {
                        int toTransfer = Math.min(maxCursorCap - workingCursor.getCount(), slotStack.getCount());
                        workingCursor.grow(toTransfer);
                        workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() - toTransfer));
                    } else {
                        // Swap
                        if (slotStack.getCount() > maxCursorCap && workingCursor.getCount() > maxCursorCap) {
                            throw new IllegalStateException("Cannot swap two stacks where both exceed cursor capacity: " + slotStack.getCount() + " vs " + workingCursor.getCount());
                        }
                        workingSlots.put(idx, workingCursor);
                        workingCursor = slotStack;
                    }
                }
                case PICKUP_HALF -> {
                    if (workingCursor.isEmpty() && !slotStack.isEmpty()) {
                        int half = (slotStack.getCount() + 1) / 2;
                        int amount = Math.min(half, maxCursorCap);
                        workingCursor = slotStack.copyWithCount(amount);
                        workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() - amount));
                    }
                }
                case DEPOSIT_ALL -> {
                    if (workingCursor.isEmpty()) break;
                    if (slotStack.isEmpty()) {
                        int amount = Math.min(workingCursor.getCount(), maxSlotCap);
                        workingSlots.put(idx, workingCursor.copyWithCount(amount));
                        workingCursor.shrink(amount);
                    } else if (ItemStack.isSameItemSameComponents(workingCursor, slotStack)) {
                        int toTransfer = Math.min(maxSlotCap - slotStack.getCount(), workingCursor.getCount());
                        workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() + toTransfer));
                        workingCursor.shrink(toTransfer);
                    } else {
                        // Swap logic
                        if (slotStack.getCount() > maxCursorCap && workingCursor.getCount() > maxCursorCap) {
                            throw new IllegalStateException("Cannot swap two mega-stacks: " + slotStack.getCount() + " vs " + workingCursor.getCount());
                        }
                        workingSlots.put(idx, workingCursor);
                        workingCursor = slotStack;
                    }
                }
                case DEPOSIT_ONE -> {
                    if (workingCursor.isEmpty()) break;
                    if (slotStack.isEmpty() || (ItemStack.isSameItemSameComponents(workingCursor, slotStack) && slotStack.getCount() < maxSlotCap)) {
                        if (slotStack.isEmpty()) {
                            workingSlots.put(idx, workingCursor.copyWithCount(1));
                        } else {
                            workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() + 1));
                        }
                        workingCursor.shrink(1);
                    }
                }
                case SWAP -> {
                    if (slotStack.getCount() > maxCursorCap && workingCursor.getCount() > maxCursorCap) {
                        throw new IllegalStateException("Atomic swap prohibited for multiple mega-stacks.");
                    }
                    workingSlots.put(idx, workingCursor);
                    workingCursor = slotStack;
                }
            }
        }

        List<SlotState> finalSlots = workingSlots.entrySet().stream()
                .map(e -> new SlotState(e.getKey(), e.getValue()))
                .sorted(java.util.Comparator.comparingInt(SlotState::index))
                .collect(Collectors.toUnmodifiableList());

        return new InventorySnapshot(containerId, finalSlots, workingCursor);
    }
}
