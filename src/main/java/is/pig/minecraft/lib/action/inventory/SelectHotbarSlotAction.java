package is.pig.minecraft.lib.action.inventory;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import java.util.Optional;

/**
 * Platform-agnostic action to select a specific hotbar slot.
 * ZERO net.minecraft imports.
 */
public class SelectHotbarSlotAction extends AbstractAction {
    private final int slot;

    public SelectHotbarSlotAction(int slot, String sourceMod, ActionPriority priority, int timeoutTicks) {
        super(sourceMod, priority, timeoutTicks);
        this.slot = slot;
    }

    public SelectHotbarSlotAction(int slot, String sourceMod, ActionPriority priority) {
        this(slot, sourceMod, priority, 40);
    }

    public SelectHotbarSlotAction(int slot, String sourceMod) {
        this(slot, sourceMod, ActionPriority.NORMAL, 40);
    }

    @Override
    protected void onExecute(Object client) {
        PiggyServiceRegistry.getInventoryInteractionAdapter().swapToSlot(client, this.slot);
    }

    @Override
    protected Optional<Boolean> verify(Object client) {
        if (PiggyServiceRegistry.getWorldStateAdapter().getSelectedSlot(client) == this.slot) {
            return Optional.of(true);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Select Hotbar Slot";
    }
}
