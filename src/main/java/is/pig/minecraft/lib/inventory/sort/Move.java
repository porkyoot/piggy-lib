package is.pig.minecraft.lib.inventory.sort;

/**
 * Represents a atomic mathematical interaction with a Minecraft inventory slot.
 * These moves are used to simulate state transitions in the Shadow Inventory
 * without sending actual packets to the server.
 */
public sealed interface Move {
    /**
     * The target slot index for this move.
     */
    int slotIndex();

    /**
     * A standard full click (Pick up all / Place all / Swap).
     */
    record LeftClick(int slotIndex) implements Move {}

    /**
     * A partial click (Pick up half / Place one).
     */
    record RightClick(int slotIndex) implements Move {}
}
