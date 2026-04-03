package is.pig.minecraft.lib.inventory.sort;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.inventory.ClickWindowSlotAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

/**
 * @deprecated Legacy imperative click generator. Migrated to state-machine based reconciliation.
 * This class is preserved for API compatibility during the transition but all imperative loops have been removed.
 */
@Deprecated
public class SortingClickGenerator {

    public static final int NO_SLOT = -1;

    public SortingClickGenerator(ItemStack[] currentState, ItemStack[] desiredState, Slot[] slots,
                                  int externalBufferSlot, int containerId, String sourceMod, 
                                  ActionPriority priority, ItemStack initialCursor) {
        // Legacy constructor - no-op
    }

    /**
     * @return An empty list. Imperative generation is disabled.
     */
    public List<ClickWindowSlotAction> generate() {
        return Collections.emptyList();
    }
}
