package is.pig.minecraft.lib.action.player;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class SetRotationAction extends AbstractAction {
    private final float targetPitch;
    private final float targetYaw;
    private int ticksToInterpolate;

    public SetRotationAction(float targetPitch, float targetYaw, String sourceMod) {
        this(targetPitch, targetYaw, 0, sourceMod);
    }

    public SetRotationAction(float targetPitch, float targetYaw, int ticksToInterpolate, String sourceMod) {
        super(sourceMod, ActionPriority.NORMAL, Math.max(10, ticksToInterpolate + 10));
        this.targetPitch = targetPitch;
        this.targetYaw = targetYaw;
        this.ticksToInterpolate = Math.max(0, ticksToInterpolate);
    }

    @Override
    public boolean execute(Minecraft client) {
        if (this.ticksToInterpolate <= 0) {
            Player player = client.player;
            if (player != null) {
                player.setXRot(this.targetPitch);
                player.setYRot(this.targetYaw);
            }
            return true;
        }
        return super.execute(client);
    }

    @Override
    protected void onExecute(Minecraft client) {
        // Initialization handled lazily/continuously in verify()
    }

    @Override
    protected boolean verify(Minecraft client) {
        Player player = client.player;
        if (player == null) return true;

        if (this.ticksToInterpolate > 0) {
            float currentPitch = player.getXRot();
            float currentYaw = player.getYRot();
            
            float yawDelta = ((this.targetYaw - currentYaw) % 360.0f + 540.0f) % 360.0f - 180.0f;
            float pitchDelta = this.targetPitch - currentPitch;

            float stepYaw = yawDelta / this.ticksToInterpolate;
            float stepPitch = pitchDelta / this.ticksToInterpolate;

            player.setXRot(currentPitch + stepPitch);
            player.setYRot(currentYaw + stepYaw);

            this.ticksToInterpolate--;
        }

        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();
        
        boolean pitchMatches = Math.abs(currentPitch - this.targetPitch) <= 0.1f;
        float yawDiff = Math.abs(((currentYaw - this.targetYaw) % 360.0f + 540.0f) % 360.0f - 180.0f);
        boolean yawMatches = yawDiff <= 0.1f;

        return (pitchMatches && yawMatches) || this.ticksToInterpolate <= 0;
    }

    @Override
    public String getName() {
        return "Set Rotation";
    }
}
