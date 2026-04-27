package is.pig.minecraft.lib.common.action.player;

import is.pig.minecraft.lib.common.action.AbstractAction;
import is.pig.minecraft.lib.common.action.ActionPriority;
import is.pig.minecraft.lib.api.IPlayerController;

public class SetRotationAction extends AbstractAction {
    private final float targetPitch;
    private final float targetYaw;

    public SetRotationAction(float targetPitch, float targetYaw, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.targetPitch = targetPitch;
        this.targetYaw = targetYaw;
    }

    public SetRotationAction(float targetPitch, float targetYaw, String sourceMod) {
        this(targetPitch, targetYaw, sourceMod, ActionPriority.NORMAL);
    }

    @Override
    protected void onExecute(IPlayerController controller) {
        controller.setCameraRotation(targetYaw, targetPitch);
    }
}
