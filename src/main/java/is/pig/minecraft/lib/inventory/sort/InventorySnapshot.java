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
        // Use a map internally for O(1) slot access during simulation
        Map<Integer, ItemStack> workingSlots = new HashMap<>();
        for (SlotState state : slots) {
            workingSlots.put(state.index, state.stack().copy());
        }
        ItemStack workingCursor = cursor.copy();

        for (Move move : moves) {
            int idx = move.slotIndex();
            ItemStack slotStack = workingSlots.getOrDefault(idx, ItemStack.EMPTY).copy();
            
            if (move instanceof Move.LeftClick) {
                if (workingCursor.isEmpty()) {
                    workingCursor = slotStack;
                    workingSlots.put(idx, ItemStack.EMPTY);
                } else if (slotStack.isEmpty()) {
                    int max = workingCursor.getMaxStackSize();
                    if (workingCursor.getCount() <= max) {
                        workingSlots.put(idx, workingCursor);
                        workingCursor = ItemStack.EMPTY;
                    } else {
                        workingSlots.put(idx, workingCursor.copyWithCount(max));
                        workingCursor.shrink(max);
                    }
                } else if (ItemStack.isSameItemSameComponents(workingCursor, slotStack)) {
                    int max = slotStack.getMaxStackSize();
                    int space = max - slotStack.getCount();
                    int toTransfer = Math.min(space, workingCursor.getCount());
                    
                    if (toTransfer > 0) {
                        workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() + toTransfer));
                        workingCursor.shrink(toTransfer);
                    } else {
                        workingSlots.put(idx, workingCursor);
                        workingCursor = slotStack;
                    }
                } else {
                    workingSlots.put(idx, workingCursor);
                    workingCursor = slotStack;
                }
            } else if (move instanceof Move.RightClick) {
                if (workingCursor.isEmpty()) {
                    if (!slotStack.isEmpty()) {
                        int half = (slotStack.getCount() + 1) / 2;
                        workingCursor = slotStack.copyWithCount(half);
                        workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() - half));
                    }
                } else {
                    if (slotStack.isEmpty()) {
                        workingSlots.put(idx, workingCursor.copyWithCount(1));
                        workingCursor.shrink(1);
                    } else if (ItemStack.isSameItemSameComponents(workingCursor, slotStack)) {
                        int max = slotStack.getMaxStackSize();
                        if (slotStack.getCount() < max) {
                            workingSlots.put(idx, slotStack.copyWithCount(slotStack.getCount() + 1));
                            workingCursor.shrink(1);
                        } else {
                            workingSlots.put(idx, workingCursor);
                            workingCursor = slotStack;
                        }
                    } else {
                        workingSlots.put(idx, workingCursor);
                        workingCursor = slotStack;
                    }
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
