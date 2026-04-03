package is.pig.minecraft.lib.inventory.sort;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponentMap;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core mathematical engine for inventory sorting.
 * Operates on InventorySnapshots and generates abstract Move sequences.
 */
public class InventoryOptimizer {

    /**
     * Consolidates partial stacks of identical items to free up empty slots.
     * Prioritizes emptying slots completely.
     */
    public List<Move> consolidate(InventorySnapshot currentState) {
        List<Move> moves = new ArrayList<>();
        InventorySnapshot virtual = currentState;

        // Group items by type (Item + Components)
        Map<ItemKey, List<Integer>> partialStacks = new HashMap<>();
        Map<Integer, ItemStack> slotMap = toMap(virtual.slots());

        for (InventorySnapshot.SlotState slot : virtual.slots()) {
            if (!slot.stack().isEmpty() && slot.stack().getCount() < slot.stack().getMaxStackSize()) {
                ItemKey key = new ItemKey(slot.stack());
                partialStacks.computeIfAbsent(key, k -> new ArrayList<>()).add(slot.index());
            }
        }

        for (Map.Entry<ItemKey, List<Integer>> entry : partialStacks.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() < 2) continue;

            // Sort indices by count (ascending) to empty smaller stacks first
            indices.sort(Comparator.comparingInt(i -> slotMap.get(i).getCount()));

            for (int i = 0; i < indices.size() - 1; i++) {
                int sourceIdx = indices.get(i);
                for (int j = indices.size() - 1; j > i; j--) {
                    int targetIdx = indices.get(j);
                    
                    ItemStack source = slotMap.get(sourceIdx);
                    ItemStack target = slotMap.get(targetIdx);
                    if (source.isEmpty()) break;

                    int max = target.getMaxStackSize();
                    int space = max - target.getCount();
                    if (space <= 0) continue;

                    int toMove = Math.min(space, source.getCount());
                    
                    List<Move> transferSteps = generateTransfer(sourceIdx, targetIdx, toMove, virtual);
                    moves.addAll(transferSteps);
                    virtual = virtual.applyMoves(transferSteps);
                    
                    // Update local state for next iteration
                    slotMap.put(sourceIdx, virtual.slots().stream().filter(s -> s.index() == sourceIdx).findFirst().map(InventorySnapshot.SlotState::stack).orElse(ItemStack.EMPTY));
                    slotMap.put(targetIdx, virtual.slots().stream().filter(s -> s.index() == targetIdx).findFirst().map(InventorySnapshot.SlotState::stack).orElse(ItemStack.EMPTY));
                }
            }
        }

        return moves;
    }

    /**
     * Plans the item transitions between current and target states using Disjoint Cycle Decomposition.
     */
    public List<Move> planCycles(InventorySnapshot current, InventorySnapshot targetSnapshot) {
        List<Move> moves = new ArrayList<>();
        InventorySnapshot virtual = current;
        Map<Integer, ItemStack> targetMap = toMap(targetSnapshot.slots());
        
        Set<Integer> visited = new HashSet<>();
        List<Integer> allIndices = targetSnapshot.slots().stream().map(InventorySnapshot.SlotState::index).toList();

        for (int i : allIndices) {
            if (visited.contains(i)) continue;
            
            ItemStack cur = getStack(virtual, i);
            ItemStack tar = targetMap.getOrDefault(i, ItemStack.EMPTY);

            if (isSame(cur, tar)) {
                visited.add(i);
                continue;
            }

            // Cycle detected or chain starts
            // 1. Pick up item at i
            List<Move> pickup = generatePickup(i, virtual);
            moves.addAll(pickup);
            virtual = virtual.applyMoves(pickup);
            visited.add(i);

            int safety = 0;
            while (!virtual.cursor().isEmpty() && safety++ < 1000) {
                // Where does the cursor item belong?
                int nextSlot = findDestination(virtual.cursor(), targetMap, visited, allIndices);
                if (nextSlot == -1) {
                    // Nowhere in the sorted set? Find an empty slot to dump
                    nextSlot = findEmptySlot(virtual, allIndices);
                    if (nextSlot == -1) break; // Trapped
                }

                // Swap/Deposit at nextSlot
                List<Move> swap = generateSwap(nextSlot, virtual);
                moves.addAll(swap);
                virtual = virtual.applyMoves(swap);
                visited.add(nextSlot);

                // Optimization: If the cursor holds what belongs in the original start slot 'i', we closed a cycle
                if (isSame(virtual.cursor(), targetMap.getOrDefault(i, ItemStack.EMPTY))) {
                     List<Move> close = generateSwap(i, virtual);
                     moves.addAll(close);
                     virtual = virtual.applyMoves(close);
                     break;
                }
            }
        }

        return moves;
    }

    private List<Move> generateTransfer(int from, int to, int amount, InventorySnapshot state) {
        List<Move> moves = new ArrayList<>();
        int remaining = amount;
        int max = state.slots().stream().filter(s -> s.index() == from).findFirst().map(s -> s.stack().getMaxStackSize()).orElse(64);

        while (remaining > 0) {
            int chunk = Math.min(remaining, max);
            // This is a simplification: assuming LeftClick picks up all (up to 64)
            // If the user wants specific counts, they'd use RightClicks.
            // For consolidation of whole piles, LeftClicks are sufficient.
            moves.add(new Move.LeftClick(from));
            moves.add(new Move.LeftClick(to));
            // If cursor has residual, we need to return it (though simulation should handle it)
            remaining -= chunk;
        }
        return moves;
    }

    private List<Move> generatePickup(int slot, InventorySnapshot state) {
        return List.of(new Move.LeftClick(slot));
    }

    private List<Move> generateSwap(int slot, InventorySnapshot state) {
        return List.of(new Move.LeftClick(slot));
    }

    private int findDestination(ItemStack cursor, Map<Integer, ItemStack> targetMap, Set<Integer> visited, List<Integer> allIndices) {
        for (int i : allIndices) {
            if (visited.contains(i)) continue;
            if (isSame(cursor, targetMap.get(i))) return i;
        }
        return -1;
    }

    private int findEmptySlot(InventorySnapshot state, List<Integer> allIndices) {
        Map<Integer, ItemStack> currentMap = toMap(state.slots());
        for (int i : allIndices) {
            if (currentMap.getOrDefault(i, ItemStack.EMPTY).isEmpty()) return i;
        }
        return -1;
    }

    private Map<Integer, ItemStack> toMap(List<InventorySnapshot.SlotState> states) {
        return states.stream().collect(Collectors.toMap(InventorySnapshot.SlotState::index, InventorySnapshot.SlotState::stack));
    }

    private ItemStack getStack(InventorySnapshot state, int index) {
        return state.slots().stream().filter(s -> s.index() == index).findFirst().map(InventorySnapshot.SlotState::stack).orElse(ItemStack.EMPTY);
    }

    private boolean isSame(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
    }

    private record ItemKey(net.minecraft.world.item.Item item, DataComponentMap components) {
        ItemKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponents());
        }
    }
}
