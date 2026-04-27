package is.pig.minecraft.lib.common.action.inventory;

import is.pig.minecraft.lib.common.action.AbstractAction;
import is.pig.minecraft.lib.common.action.ActionPriority;
import is.pig.minecraft.lib.api.IPlayerController;

public class ClickWindowSlotAction extends AbstractAction {
    private final int syncId;
    private final int slotId;
    private final int button;
    private final String clickType;

    public ClickWindowSlotAction(int syncId, int slotId, int button, String clickType, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.syncId = syncId;
        this.slotId = slotId;
        this.button = button;
        this.clickType = clickType;
    }

    public ClickWindowSlotAction(int syncId, int slotId, int button, String clickType, String sourceMod) {
        this(syncId, slotId, button, clickType, sourceMod, ActionPriority.NORMAL);
    }

    @Override
    protected void onExecute(IPlayerController controller) {
        controller.clickInventorySlot(syncId, slotId, button, clickType);
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
