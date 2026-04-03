package is.pig.minecraft.lib.inventory.sort;

/**
 * Represents an explicit mathematical interaction with a Minecraft inventory slot.
 * These moves are used to simulate state transitions in the Shadow Inventory
 * following strict cursor capacity constraints.
 */
public record Move(int slotIndex, MoveType type) {
    public enum MoveType {
        /**
         * Pick up stack from slot into cursor (up to 64 items).
         */
        PICKUP_ALL,
        
        /**
         * Pick up half of the stack from slot into cursor (up to 64 items).
         */
        PICKUP_HALF,
        
        /**
         * Place the entire cursor stack into the slot (respecting slot capacity).
         */
        DEPOSIT_ALL,
        
        /**
         * Place one item from the cursor into the slot.
         */
        DEPOSIT_ONE,
        
        /**
         * Swaps cursor and slot contents (throws if both exceed 64).
         */
        SWAP
    }
}
