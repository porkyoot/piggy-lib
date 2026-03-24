package is.pig.minecraft.lib.action.inventory;

import is.pig.minecraft.lib.action.BulkAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.world.inventory.ClickType;

import java.util.Arrays;

public class MoveItemAction extends BulkAction {
    public MoveItemAction(int containerId, int sourceSlot, int targetSlot, String sourceMod, ActionPriority priority) {
        super(sourceMod, "Move Item", Arrays.asList(
                new ClickWindowSlotAction(containerId, sourceSlot, 0, ClickType.PICKUP, sourceMod, priority),
                new ClickWindowSlotAction(containerId, targetSlot, 0, ClickType.PICKUP, sourceMod, priority)
        ), () -> true);
        if (priority == ActionPriority.HIGHEST || priority == ActionPriority.HIGH) {
            this.setIgnoreGlobalCps(true);
        }
    }

    public MoveItemAction(int containerId, int sourceSlot, int targetSlot, String sourceMod) {
        this(containerId, sourceSlot, targetSlot, sourceMod, ActionPriority.NORMAL);
    }
}
