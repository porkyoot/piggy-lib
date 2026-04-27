package is.pig.minecraft.lib.common.action.world;

import is.pig.minecraft.lib.common.action.AbstractAction;
import is.pig.minecraft.lib.common.action.ActionPriority;
import is.pig.minecraft.lib.api.IPlayerController;

public class AttackEntityAction extends AbstractAction {
    private final int entityId;

    public AttackEntityAction(int entityId, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.entityId = entityId;
    }

    public AttackEntityAction(int entityId, String sourceMod) {
        this(entityId, sourceMod, ActionPriority.NORMAL);
    }

    @Override
    protected void onExecute(IPlayerController controller) {
        controller.attackEntity(entityId);
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
