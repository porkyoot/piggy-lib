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
    private final ItemStack initialCursor;

    public SortingClickGenerator(ItemStack[] currentState, ItemStack[] desiredState, Slot[] slots,
                                  int externalBufferSlot, int containerId, String sourceMod, ActionPriority priority, ItemStack initialCursor) {
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
        this.initialCursor = initialCursor != null ? initialCursor.copy() : ItemStack.EMPTY;
        this.cursorState = this.initialCursor.copy();
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
        resolveCursorChain(); // Doit impérativement être en premier !
        resolveChains();
        resolveCycles();
        // --- SANITY CHECK INTERNE ---
        for (int i = 0; i < currentState.length; i++) {
            if (!isSameAndEqual(currentState[i], desiredState[i])) {
                throw new RuntimeException("ALGORITHM FAILURE: Virtual state desynced at Slot " + i + 
                        ". Expected: " + desiredState[i] + " but got: " + currentState[i]);
            }
        }
        if (!cursorState.isEmpty()) {
            throw new RuntimeException("ALGORITHM FAILURE: Cursor is not empty at the end of the sort!");
        }
        // -----------------------------
        return new ArrayList<>(this.actions);
    }

    private void resolveCursorChain() {
        if (cursorState.isEmpty()) return;
        
        int safety = 0;
        while (!cursorState.isEmpty() && safety++ < currentState.length) {
            int target = findDestinationForCursor();
            if (target == -1) {
                // Vidage d'urgence pour libérer le curseur avant resolveChains
                int buffer = findSafeBufferSlot();
                if (buffer != -1) {
                    emitClickSwap(buffer);
                } else if (externalBufferSlot != NO_SLOT) {
                    evacuateCursorToExternal();
                }
                break;
            }

            ItemStack cursorStack = cursorState.copy();
            ItemStack targetSlotStack = currentState[target].copy();

            if (!targetSlotStack.isEmpty() && ItemStack.isSameItem(cursorStack, targetSlotStack)) {
                if (!handleIdenticalItemCollision(target, cursorStack)) {
                    if (externalBufferSlot != NO_SLOT) {
                        evacuateCursorToExternal();
                        break;
                    } else {
                        break; 
                    }
                }
            } else {
                emitClickSwap(target);
            }
        }
    }

    private void resolveChains() {
        boolean progressed;
        int safety = 0;
        do {
            progressed = false;
            for (int i = 0; i < currentState.length; i++) {
                if (needsItems(i)) {
                    int source = findSourceSlot(desiredState[i], i);
                    if (source != -1) {
                        int needed = desiredState[i].getCount() - currentState[i].getCount();
                        int max = currentState[source].getMaxStackSize();
                        
                        if (currentState[i].isEmpty() && currentState[source].getCount() > max) {
                            generateChunkedTransfer(source, i, needed, max);
                        } else {
                            transfer(source, i);
                        }
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
                if (currentState[i].isEmpty()) continue;
                
                // Ignorer les surplus
                boolean isWrongType = !ItemStack.isSameItemSameComponents(currentState[i], desiredState[i]);
                boolean isSurplus = !isWrongType && currentState[i].getCount() > desiredState[i].getCount();
                
                if (isWrongType || isSurplus) {
                    startSlot = i;
                    break;
                }
            }
            if (startSlot == -1) break;

            if (!cursorState.isEmpty() && ItemStack.isSameItem(cursorState, currentState[startSlot])) {
                if (!handleIdenticalItemCollision(startSlot, cursorState)) {
                    if (externalBufferSlot != NO_SLOT) {
                        evacuateCursorToExternal();
                        break;
                    } else {
                        break; 
                    }
                }
            } else {
                // CORRECTION 1 : PROTECTION SUPER STACK POUR LE DÉPART DU CYCLE
                int count = currentState[startSlot].getCount();
                int max = currentState[startSlot].getMaxStackSize();
                if (count > max) {
                    int buffer = findSafeBufferSlot();
                    if (buffer == -1) buffer = externalBufferSlot;
                    
                    if (buffer != NO_SLOT) {
                        if (buffer < currentState.length && buffer >= 0) {
                            generateChunkedTransfer(startSlot, buffer, count, max);
                        } else {
                            evacuateSlotToExternal(startSlot);
                        }
                        // Lancer la chaîne créée par le vidage IMMÉDIATEMENT avant de continuer
                        resolveChains(); 
                        continue; 
                    } else {
                        break; // Impasse
                    }
                } else {
                    emitClickSwap(startSlot);
                }
            }

            int cycleSafety = 0;
            while (!cursorState.isEmpty() && cycleSafety++ < currentState.length) {
                int targetSlot = findDestinationForCursor();
                if (targetSlot == -1 || targetSlot == startSlot) {
                    int dumpSlot = (targetSlot != -1) ? targetSlot : findSafeBufferSlot();
                    if (dumpSlot != -1) {
                        emitClickSwap(dumpSlot);
                    } else if (externalBufferSlot != NO_SLOT) {
                        evacuateCursorToExternal();
                    }
                    break;
                }

                ItemStack cursorStack = cursorState.copy();
                ItemStack targetSlotStack = currentState[targetSlot].copy();

                int count = targetSlotStack.getCount();
                int max = targetSlotStack.getMaxStackSize();
                
                // CORRECTION 2 : PROTECTION SUPER STACK POUR LA CIBLE
                if (!targetSlotStack.isEmpty() && count > max) {
                    int buffer = findSafeBufferSlot();
                    if (buffer == -1) buffer = externalBufferSlot;
                    
                    if (buffer != NO_SLOT) {
                        if (buffer < currentState.length && buffer >= 0) {
                            generateChunkedTransfer(targetSlot, buffer, count, max);
                        } else {
                            evacuateSlotToExternal(targetSlot);
                        }
                        
                        // Déposer l'item courant dans la case qu'on vient de vider
                        emitClickSwap(targetSlot); 
                        
                        // ON S'ARRÊTE LÀ ! Ne surtout PAS récupérer le super stack sur le curseur.
                        // Le curseur est désormais vide. Le `break` laisse resolveChains agir à la fin.
                        break; 
                    } else {
                        break;
                    }
                } else if (!targetSlotStack.isEmpty() && ItemStack.isSameItem(cursorStack, targetSlotStack)) {
                    if (!handleIdenticalItemCollision(targetSlot, cursorStack)) {
                        if (externalBufferSlot != NO_SLOT) {
                            evacuateCursorToExternal();
                            break;
                        } else {
                            break; 
                        }
                    }
                } else {
                    emitClickSwap(targetSlot);
                }
            }
            
            if (cursorState.isEmpty()) {
                resolveChains();
            }
        }
    }


    private void evacuateCursorToExternal() {
        if (cursorState.isEmpty()) return;
        ItemStack toEvacuate = cursorState.copy();
        
        actions.add(new ClickWindowSlotAction(containerId, externalBufferSlot, 0, ClickType.PICKUP, sourceMod, priority)
                .withExpectedCursorBefore(s -> isSameAndEqual(s, toEvacuate))
                .withExpectedCursorAfter(ItemStack::isEmpty));
        
        cursorState = ItemStack.EMPTY;
    }

    /** Evacuates an oversized stack from a slot to the external buffer in chunks. */
    private void evacuateSlotToExternal(int arrayIndex) {
        if (currentState[arrayIndex].isEmpty()) return;
        ItemStack itemType = currentState[arrayIndex].copy();
        int totalToMove = itemType.getCount();
        int max = itemType.getMaxStackSize();
        
        int moved = 0;
        while (moved < totalToMove && !currentState[arrayIndex].isEmpty()) {
            int chunk = Math.min(totalToMove - moved, Math.min(currentState[arrayIndex].getCount(), max));
            
            // Pick up chunk from source
            ItemStack beforeCursorPickup = cursorState.copy();
            if (chunk < currentState[arrayIndex].getCount()) {
                currentState[arrayIndex] = itemType.copyWithCount(currentState[arrayIndex].getCount() - chunk);
                cursorState = itemType.copyWithCount(chunk);
            } else {
                currentState[arrayIndex] = ItemStack.EMPTY; // FIX : fantômes
                cursorState = itemType.copyWithCount(chunk);
            }
            ItemStack afterCursorPickup = cursorState.copy();
            actions.add(new ClickWindowSlotAction(containerId, slots[arrayIndex].index, 0, ClickType.PICKUP, sourceMod, priority)
                    .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursorPickup))
                    .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursorPickup)));
            
            // Drop in external buffer
            ItemStack toEvacuate = cursorState.copy();
            actions.add(new ClickWindowSlotAction(containerId, externalBufferSlot, 0, ClickType.PICKUP, sourceMod, priority)
                    .withExpectedCursorBefore(s -> isSameAndEqual(s, toEvacuate))
                    .withExpectedCursorAfter(ItemStack::isEmpty));
            cursorState = ItemStack.EMPTY;
            
            moved += chunk;
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
            ItemStack type = currentState[from].copy();
            ItemStack beforeCursorPickup = cursorState.copy();
            
            if (pickupAmount < type.getCount()) {
                currentState[from] = type.copyWithCount(type.getCount() - pickupAmount);
                cursorState = type.copyWithCount(pickupAmount);
            } else {
                currentState[from] = ItemStack.EMPTY;
                cursorState = type.copyWithCount(pickupAmount);
            }
            
            ItemStack afterCursorPickup = cursorState.copy();
            actions.add(new ClickWindowSlotAction(containerId, slots[from].index, 0, ClickType.PICKUP, sourceMod, priority)
                    .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursorPickup))
                    .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursorPickup)));

            int spaceInDest = destSlotMax - currentState[to].getCount();
            int depositAmount = Math.min(pickupAmount, Math.min(spaceInDest, toMove - moved));

            // 2. Deposit (Correction : Gestion de la physique des clics Minecraft)
            ItemStack beforeCursorDrop = cursorState.copy();
            int leftover = pickupAmount - depositAmount;
            
            if (depositAmount > 0) {
                if (currentState[to].isEmpty()) {
                    currentState[to] = itemType.copyWithCount(depositAmount);
                } else {
                    currentState[to].grow(depositAmount);
                }
            }
            
            cursorState = leftover > 0 ? itemType.copyWithCount(leftover) : ItemStack.EMPTY;
            ItemStack afterCursorDrop = cursorState.copy();

            if (depositAmount > 0) {
                // Si on pose tout, OU si on remplit la case exactement jusqu'à sa limite -> Clic Gauche (0)
                boolean canUseLeftClick = (leftover == 0) || (spaceInDest == depositAmount && pickupAmount >= spaceInDest);

                if (canUseLeftClick) {
                    actions.add(new ClickWindowSlotAction(containerId, slots[to].index, 0, ClickType.PICKUP, sourceMod, priority)
                            .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursorDrop))
                            .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursorDrop)));
                } else {
                    // Sinon, on doit distribuer exactement 'depositAmount' items un par un -> Clics Droits (1)
                    for (int k = 0; k < depositAmount; k++) {
                        int currentHold = pickupAmount - k;
                        int nextHold = currentHold - 1;
                        ItemStack cBefore = itemType.copyWithCount(currentHold);
                        ItemStack cAfter = nextHold > 0 ? itemType.copyWithCount(nextHold) : ItemStack.EMPTY;

                        actions.add(new ClickWindowSlotAction(containerId, slots[to].index, 1, ClickType.PICKUP, sourceMod, priority)
                                .withExpectedCursorBefore(s -> isSameAndEqual(s, cBefore))
                                .withExpectedCursorAfter(s -> isSameAndEqual(s, cAfter)));
                    }
                }
            }

            // 3. Return residual
            if (leftover > 0) {
                ItemStack beforeCursorReturn = cursorState.copy();
                if (currentState[from].isEmpty()) {
                    currentState[from] = itemType.copyWithCount(leftover);
                } else {
                    currentState[from].grow(leftover);
                }
                cursorState = ItemStack.EMPTY;
                ItemStack afterCursorReturn = cursorState.copy();
                
                actions.add(new ClickWindowSlotAction(containerId, slots[from].index, 0, ClickType.PICKUP, sourceMod, priority)
                        .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursorReturn))
                        .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursorReturn)));
            }

            moved += depositAmount;
            if (depositAmount == 0) break; 
        }
    }

    private void emitClickSwap(int slotIndex) {
        // 1. On prend une "photo" de ce qu'on a en main et dans la case AVANT le clic
        ItemStack beforeCursor = cursorState.isEmpty() ? ItemStack.EMPTY : cursorState.copy();
        ItemStack beforeSlot = currentState[slotIndex].isEmpty() ? ItemStack.EMPTY : currentState[slotIndex].copy();

        // 2. On simule l'échange dans la mémoire virtuelle de l'algorithme
        currentState[slotIndex] = beforeCursor;
        cursorState = beforeSlot;

        // 3. On prend une "photo" de la main APRÈS le clic
        ItemStack afterCursor = cursorState.isEmpty() ? ItemStack.EMPTY : cursorState.copy();

        // 4. On génère l'action avec une sécurité absolue sur les prédictions
        actions.add(new ClickWindowSlotAction(containerId, slots[slotIndex].index, 0, ClickType.PICKUP, sourceMod, priority)
                .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursor))
                .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursor)));
    }

    private int findDestinationForCursor() {
        if (cursorState.isEmpty()) return -1;
        
        // Priorité 1 : Match PARFAIT (Type + Composants + Quantité exacte)
        for (int i = 0; i < currentState.length; i++) {
            if (isSameAndEqual(desiredState[i], cursorState)) {
                if (!isSameAndEqual(currentState[i], desiredState[i])) {
                    return i;
                }
            }
        }
        
        // Priorité 2 : Match de Type (Déblocage d'inventaire plein)
        // La case veut cet objet, mais elle est actuellement squattée par un objet DIFFÉRENT.
        // On renvoie cette case pour forcer un Swap et libérer l'espace pour resolveChains().
        for (int i = 0; i < currentState.length; i++) {
            if (ItemStack.isSameItemSameComponents(desiredState[i], cursorState)) {
                if (!isSame(currentState[i], desiredState[i])) {
                    // Sécurité : On ne swap QUE si la case est vide ou contient un objet différent,
                    // pour éviter de provoquer une fusion Minecraft non anticipée.
                    if (currentState[i].isEmpty() || !ItemStack.isSameItem(currentState[i], cursorState)) {
                        return i;
                    }
                }
            }
        }
        
        return -1;
    }

    private void generateChunkedTransfer(int sourceSlot, int targetSlot, int quantityToMove, int maxCursorSize) {
        ItemStack itemType = currentState[sourceSlot].copy();
        int movedTotal = 0;
        while (movedTotal < quantityToMove && !currentState[sourceSlot].isEmpty()) {
            int chunk = Math.min(quantityToMove - movedTotal, Math.min(currentState[sourceSlot].getCount(), maxCursorSize));
            
            // Pickup
            ItemStack beforeCursorPickup = cursorState.copy();
            if (chunk < currentState[sourceSlot].getCount()) {
                currentState[sourceSlot] = itemType.copyWithCount(currentState[sourceSlot].getCount() - chunk);
                cursorState = itemType.copyWithCount(chunk);
            } else {
                currentState[sourceSlot] = ItemStack.EMPTY; // FIX : fantômes
                cursorState = itemType.copyWithCount(chunk);
            }
            ItemStack afterCursorPickup = cursorState.copy();
            actions.add(new ClickWindowSlotAction(containerId, slots[sourceSlot].index, 0, ClickType.PICKUP, sourceMod, priority)
                    .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursorPickup))
                    .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursorPickup)));

            // Drop
            ItemStack beforeCursorDrop = cursorState.copy();
            if (currentState[targetSlot].isEmpty()) {
                currentState[targetSlot] = itemType.copyWithCount(chunk);
            } else {
                currentState[targetSlot].grow(chunk);
            }
            cursorState = ItemStack.EMPTY;
            ItemStack afterCursorDrop = cursorState.copy();
            actions.add(new ClickWindowSlotAction(containerId, slots[targetSlot].index, 0, ClickType.PICKUP, sourceMod, priority)
                    .withExpectedCursorBefore(s -> isSameAndEqual(s, beforeCursorDrop))
                    .withExpectedCursorAfter(s -> isSameAndEqual(s, afterCursorDrop)));
            
            movedTotal += chunk;
        }
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
        if (a.isEmpty() && b.isEmpty())
            return true;
        if (a.isEmpty() || b.isEmpty())
            return false;
        return ItemStack.isSameItemSameComponents(a, b) && a.getCount() == b.getCount();
    }
    
    private boolean handleIdenticalItemCollision(int targetSlot, ItemStack cursorStack) {
        int pivot1 = -1;
        int pivot2 = -1;

        for (int i = 0; i < currentState.length; i++) {
            if (i == targetSlot) continue;
            
            boolean isEmpty = currentState[i].isEmpty();
            boolean isDifferentFromCursor = !ItemStack.isSameItem(currentState[i], cursorStack);
            
            if (isEmpty || isDifferentFromCursor) {
                if (currentState[i].getCount() <= currentState[i].getMaxStackSize()) {
                    if (pivot1 == -1) {
                        pivot1 = i;
                    } else if (pivot2 == -1) {
                        // SÉCURITÉ ANTI-MERGE : Pivot 2 doit être strictement différent de Pivot 1 (sauf si les 2 sont vides)
                        boolean p1Empty = currentState[pivot1].isEmpty();
                        boolean isDifferentFromP1 = !ItemStack.isSameItem(currentState[i], currentState[pivot1]);
                        
                        if ((p1Empty && isEmpty) || isDifferentFromP1) {
                            pivot2 = i;
                            break;
                        }
                    }
                }
            }
        }

        if (pivot1 != -1 && pivot2 != -1) {
            emitClickSwap(pivot1);      // 1
            emitClickSwap(targetSlot);  // 2
            emitClickSwap(pivot2);      // 3
            emitClickSwap(pivot1);      // 4
            emitClickSwap(targetSlot);  // 5
            emitClickSwap(pivot1);      // 6
            emitClickSwap(pivot2);      // 7
            return true;
        }

        return false;
    }
}
