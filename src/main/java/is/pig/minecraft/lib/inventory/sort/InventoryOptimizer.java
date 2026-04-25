package is.pig.minecraft.lib.inventory.sort;

import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponentMap;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core mathematical engine for inventory sorting.
 * Operates on InventorySnapshots and generates abstract Move sequences.
 */
public class InventoryOptimizer {
    // NOUVELLE SIGNATURE : Prend le Current ET le Target pour analyser la capacité réelle
    private java.util.function.ToIntFunction<ItemStack> getCapacityProvider(InventorySnapshot currentSnapshot, InventorySnapshot targetSnapshot) {
        if (targetSnapshot == null) return ItemStack::getMaxStackSize;
        Map<ItemKey, Integer> capacities = new HashMap<>();
        
        List<InventorySnapshot.SlotState> allSlots = new ArrayList<>(targetSnapshot.slots());
        if (currentSnapshot != null) allSlots.addAll(currentSnapshot.slots());

        for (InventorySnapshot.SlotState slot : allSlots) {
            if (slot.stack().isEmpty()) continue;
            ItemKey key = new ItemKey(slot.stack());
            
            // Si l'item dépasse sa limite Vanilla, c'est un Méga-Stack.
            // On lève la limite (MAX_VALUE) pour que la simulation ne bloque aucun clic.
            if (slot.stack().getCount() > slot.stack().getMaxStackSize()) {
                capacities.put(key, Integer.MAX_VALUE);
            } else {
                capacities.put(key, Math.max(capacities.getOrDefault(key, slot.stack().getMaxStackSize()), slot.stack().getCount()));
            }
        }
        return stack -> capacities.getOrDefault(new ItemKey(stack), stack.getMaxStackSize());
    }

    private boolean isItemMatch(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) return true;
        if (a.isEmpty() || b.isEmpty()) return false;
        return ItemStack.isSameItemSameComponents(a, b);
    }

    public List<Move> consolidate(InventorySnapshot currentState) {
        return consolidate(currentState, null);
    }

    /**
     * Consolidates partial stacks of identical items to free up empty slots.
     * Focuses on 'Clutter Clearing': only merges items that are not in their target positions.
     * Items in their correct 'home' slots are ignored during this pass.
     */
    public List<Move> consolidate(InventorySnapshot currentState, InventorySnapshot targetSnapshot) {
        if (targetSnapshot == null) return new ArrayList<>(); 
        
        java.util.function.ToIntFunction<ItemStack> capacityProvider = getCapacityProvider(currentState, targetSnapshot);
        List<Move> moves = new ArrayList<>();
        InventorySnapshot virtual = currentState;
        Map<Integer, ItemStack> targetMap = toMap(targetSnapshot.slots());
        Map<Integer, ItemStack> slotMap = toMap(virtual.slots());

        // Grouper TOUS les items par type (ItemKey)
        Map<ItemKey, List<Integer>> itemLocations = new HashMap<>();
        for (InventorySnapshot.SlotState slot : virtual.slots()) {
            if (slot.stack().isEmpty()) continue;
            ItemKey key = new ItemKey(slot.stack());
            itemLocations.computeIfAbsent(key, k -> new ArrayList<>()).add(slot.index());
        }

        for (Map.Entry<ItemKey, List<Integer>> entry : itemLocations.entrySet()) {
        }

        for (Map.Entry<ItemKey, List<Integer>> entry : itemLocations.entrySet()) {
            List<Integer> indices = entry.getValue();
            if (indices.size() < 2) continue;


            // Tri intelligent : 
            // 1. Stacks Flottants (Sources) au début. Stacks "Home" (Destinations) à la fin.
            // 2. Flottants : on vide les plus petits en premier.
            // 3. Home : on remplit les plus gros en premier.
            indices.sort((idx1, idx2) -> {
                boolean home1 = isHome(idx1, slotMap.get(idx1), targetMap);
                boolean home2 = isHome(idx2, slotMap.get(idx2), targetMap);
                
                if (home1 != home2) return home1 ? 1 : -1; 
                
                int count1 = slotMap.get(idx1).getCount();
                int count2 = slotMap.get(idx2).getCount();
                
                if (!home1) {
                    return Integer.compare(count1, count2);
                } else {
                    return Integer.compare(count2, count1); 
                }
            });

            for (int i = 0; i < indices.size() - 1; i++) {
                int sourceIdx = indices.get(i);
                
                // Ne pas vider un slot "Home" s'il est déjà correctement placé
                // Sauf si on veut condenser plusieurs slots Home entre eux (mais rare)
                if (isHome(sourceIdx, slotMap.get(sourceIdx), targetMap)) {
                    int cap = capacityProvider.applyAsInt(slotMap.get(sourceIdx));
                    if (slotMap.get(sourceIdx).getCount() >= cap) continue;
                }

                for (int j = indices.size() - 1; j > i; j--) {
                    int targetIdx = indices.get(j);
                    
                    ItemStack source = slotMap.get(sourceIdx);
                    ItemStack target = slotMap.get(targetIdx);
                    if (source.isEmpty()) break;

                    int max = capacityProvider.applyAsInt(target);
                    int space = max - target.getCount();
                    int toMove = Math.min(space, source.getCount());


                    if (space <= 0) continue; 

                    List<Move> transferSteps = generateTransfer(sourceIdx, targetIdx, toMove, virtual, capacityProvider);
                    if (!transferSteps.isEmpty()) {
                        moves.addAll(transferSteps);
                        virtual = virtual.applyMoves(transferSteps, capacityProvider);
                        
                        // Mettre à jour l'état local pour l'itération suivante
                        slotMap.put(sourceIdx, getStack(virtual, sourceIdx));
                        slotMap.put(targetIdx, getStack(virtual, targetIdx));

                    }
                }
            }
        }
        return moves;
    }

    /**
     * Plans the item transitions between current and target states using Disjoint Cycle Decomposition.
     * Switches to 'Drain Mode' for mega-stacks (> 64) and enforces unidirectional flow.
     */
    public List<Move> planCycles(InventorySnapshot current, InventorySnapshot targetSnapshot) {
        java.util.function.ToIntFunction<ItemStack> capacityProvider = getCapacityProvider(current, targetSnapshot);
        List<Move> moves = new ArrayList<>();
        InventorySnapshot virtual = current;
        Map<Integer, ItemStack> targetMap = toMap(targetSnapshot.slots());
        
        Set<Integer> visited = new HashSet<>();
        Set<Long> moveFlows = new HashSet<>(); // Tracks (src << 32) | dest to prevent ping-pong
        List<Integer> allIndices = targetSnapshot.slots().stream().map(InventorySnapshot.SlotState::index).toList();

        for (var entry : targetMap.entrySet()) {
        }

        for (int i : allIndices) {
            boolean isVisited = visited.contains(i);
            ItemStack curAtStart = getStack(virtual, i);

            if (isVisited) continue;
            
            // Simulation Hygiene: If a previous cycle was aborted and couldn't dump,
            // we must stop planning for this burst to prevent "Dirty Hand" crashes.
            if (!virtual.cursor().isEmpty()) break;
            
            ItemStack cur = getStack(virtual, i);
            ItemStack tar = targetMap.getOrDefault(i, ItemStack.EMPTY);

            if (isSame(cur, tar)) {
                visited.add(i);
                continue;
            }

            // If the item type matches but counts differ, we are just reordering stacks of identical items by size.
            // This physically requires an empty slot buffer to swap.
            // If the chest is 100% full, we CANNOT swap them. Accept the slot as "sorted enough" to prevent infinite loops.
            if (isItemMatch(cur, tar) && findEmptySlot(virtual, allIndices) == -1) {
                visited.add(i);
                continue;
            }

            // Mega-Stack Drain Mode Selection
            if (cur.getCount() > 64 || tar.getCount() > 64) {
               // Where does the item currently in 'i' belong?
               int dest = findDestination(cur, targetMap, visited, allIndices, virtual);
               if (dest != -1) {
                   ItemStack destStack = getStack(virtual, dest);
                   // Congestion Check: Wait if the target slot is occupied by ANY different item to prevent cursor swaps during a drain.
                   if (!destStack.isEmpty() && !isItemMatch(cur, destStack)) {
                       continue; 
                   }

                   // Check for bidirectional conflict (Don't move B -> A if we already planned A -> B)
                   if (moveFlows.contains(((long)dest << 32) | i)) {
                       continue;
                   }
                   
                   // Plan a Drain from i to dest
                   int amount = Math.min(cur.getCount(), capacityProvider.applyAsInt(cur) - getStack(virtual, dest).getCount());
                   if (amount > 0) {
                       List<Move> drain = generateTransfer(i, dest, amount, virtual, capacityProvider);
                       moves.addAll(drain);
                       virtual = virtual.applyMoves(drain, capacityProvider);
                       moveFlows.add(((long)i << 32) | dest);
                   } else {
                   }
                   
                   // Only mark as visited if it actually matches target now
                   if (isSame(getStack(virtual, i), targetMap.get(i))) visited.add(i);
                   if (isSame(getStack(virtual, dest), targetMap.get(dest))) visited.add(dest);
                   continue;
               }
            }

            // Standard Disjoint Cycle Decomposition for non-mega stacks
            // Congestion Check Prep: Where does this item actually need to go NEXT?
            int potentialNext = findDestination(cur, targetMap, visited, allIndices, virtual);
            if (potentialNext != -1) {
                ItemStack targetOccupant = getStack(virtual, potentialNext);
                // If its immediate target is clogged by a different mega-stack, skip starting this cycle.
                if (targetOccupant.getCount() > 64 && !isItemMatch(cur, targetOccupant)) {
                    continue;
                }
            }

            // 1. Pick up item at i
            List<Move> pickup = generatePickup(i, virtual);
            moves.addAll(pickup);
            virtual = virtual.applyMoves(pickup, capacityProvider);
            visited.add(i); // Slot i is now 'empty' or holds part of what we picked up

            int safety = 0;
            while (!virtual.cursor().isEmpty() && safety++ < 1000) {
                // Where does the cursor item belong?
                int nextSlot = findDestination(virtual.cursor(), targetMap, visited, allIndices, virtual);
                if (nextSlot == -1) {
                    // Nowhere in the sorted set? Find an empty slot to dump
                    nextSlot = findEmptySlot(virtual, allIndices);
                    if (nextSlot == -1) {
                        break; // Trapped
                    }
                }

                // Safety Check: If the target destination is currently occupied by a mega-stack (> 64)
                // we CANNOT perform a standard swap. Abort the cycle and dump the cursor.
                if (getStack(virtual, nextSlot).getCount() > 64) {
                    int dumpSlot = findEmptySlot(virtual, allIndices);
                    if (dumpSlot != -1) {
                        List<Move> dump = List.of(new Move(dumpSlot, Move.MoveType.DEPOSIT_ALL));
                        moves.addAll(dump);
                        virtual = virtual.applyMoves(dump, capacityProvider);
                    } else {
                    }
                    break; // Abort cycle
                }

                // Swap/Deposit at nextSlot
                List<Move> swap = generateSwap(nextSlot, virtual);
                moves.addAll(swap);
                virtual = virtual.applyMoves(swap, capacityProvider);
                visited.add(nextSlot);

                // Optimization: If the cursor holds what belongs in the original start slot 'i', we closed a cycle
                if (isSame(virtual.cursor(), targetMap.getOrDefault(i, ItemStack.EMPTY))) {
                     List<Move> close = generateSwap(i, virtual);
                     moves.addAll(close);
                     virtual = virtual.applyMoves(close, capacityProvider);
                     // We mark visited above
                     break;
                }
            }
        }

        // Final Cursor Salvage: Guarantee the player's hand is empty at the end of the burst.
        if (!virtual.cursor().isEmpty()) {
            int dumpSlot = findEmptySlot(virtual, allIndices);
            if (dumpSlot == -1) {
                // If the inventory is 100% full, the best we can do is swap with our start slot i.
                // But for now, we'll try to find ANY slot that is not a mega-stack.
                for (int idx : allIndices) {
                    if (getStack(virtual, idx).getCount() <= 64) {
                        dumpSlot = idx;
                        break;
                    }
                }
            }
            if (dumpSlot != -1) {
                List<Move> dump = generateSwap(dumpSlot, virtual);
                moves.addAll(dump);
                virtual = virtual.applyMoves(dump, capacityProvider);
            } else {
            }
        }

        return moves;
    }

    private List<Move> generateTransfer(int from, int to, int amount, InventorySnapshot state, java.util.function.ToIntFunction<ItemStack> capacityProvider) {
        List<Move> moves = new ArrayList<>();
        int remaining = amount;
        
        // Cursors are typically bound to 64 items max for pickup logic in vanilla and most mods
        int cursorLimit = 64; 

        while (remaining > 0) {
            int chunk = Math.min(remaining, cursorLimit);
            moves.add(new Move(from, Move.MoveType.PICKUP_ALL));
            moves.add(new Move(to, Move.MoveType.DEPOSIT_ALL));
            remaining -= chunk;
        }
        return moves;
    }

    private List<Move> generatePickup(int slot, InventorySnapshot state) {
        return List.of(new Move(slot, Move.MoveType.PICKUP_ALL));
    }

    private List<Move> generateSwap(int slot, InventorySnapshot state) {
        return List.of(new Move(slot, Move.MoveType.SWAP));
    }

    private int findDestination(ItemStack cursor, Map<Integer, ItemStack> targetMap, Set<Integer> visited, List<Integer> allIndices, InventorySnapshot virtual) {
        for (int i : allIndices) {
            // Si le slot a déjà parfaitement atteint sa cible finale (quantité incluse), on l'ignore.
            if (visited.contains(i)) {
                continue;
            }
            
            ItemStack targetStack = targetMap.getOrDefault(i, ItemStack.EMPTY);
            
            // On utilise isItemMatch et NON isSame.
            // Le slot veut ce type d'objet, peu importe combien on en a actuellement dans la main.
            if (isItemMatch(cursor, targetStack)) {
                ItemStack curSlotStack = getStack(virtual, i);
                if (!curSlotStack.isEmpty() && isItemMatch(cursor, curSlotStack)) {
                    continue;
                }
                return i;
            } else {
            }
        }
        return -1;
    }

    private int findEmptySlot(InventorySnapshot state, List<Integer> allIndices) {
        Map<Integer, ItemStack> currentMap = toMap(state.slots());
        for (int i : allIndices) {
            ItemStack curStack = currentMap.getOrDefault(i, ItemStack.EMPTY);
            if (curStack.isEmpty()) {
                return i;
            }
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

    private boolean isHome(int slotIdx, ItemStack stack, Map<Integer, ItemStack> targetMap) {
        ItemStack target = targetMap.getOrDefault(slotIdx, ItemStack.EMPTY);
        // Is the item currently in slotIdx of the same type as what's planned for slotIdx?
        return isItemMatch(stack, target);
    }

    private record ItemKey(net.minecraft.world.item.Item item, DataComponentMap components) {
        ItemKey(ItemStack stack) {
            this(stack.getItem(), stack.getComponents());
        }
    }
}
