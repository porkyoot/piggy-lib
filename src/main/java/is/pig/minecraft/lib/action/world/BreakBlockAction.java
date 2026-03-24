package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

public class BreakBlockAction extends AbstractAction {
    private final BlockPos targetPos;
    private int ticksMining = 0;

    public BreakBlockAction(BlockPos targetPos, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.targetPos = targetPos;
    }

    public BreakBlockAction(BlockPos targetPos, String sourceMod) {
        this(targetPos, sourceMod, ActionPriority.NORMAL);
    }

    @Override
    public Optional<Boolean> execute(Minecraft client) {
        if (client.gameMode != null && client.player != null) {
            Vec3 targetVec = Vec3.atCenterOf(this.targetPos);
            Vec3 diff = targetVec.subtract(client.player.getEyePosition());
            double distance = diff.horizontalDistance();
            
            client.player.setYRot((float) (Math.atan2(diff.z, diff.x) * (180.0 / Math.PI)) - 90.0f);
            client.player.setXRot((float) -(Math.atan2(diff.y, distance) * (180.0 / Math.PI)));

            if (ticksMining == 0) {
                client.gameMode.startDestroyBlock(this.targetPos, Direction.UP);
            } else {
                client.gameMode.continueDestroyBlock(this.targetPos, Direction.UP);
            }
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        ticksMining++;
        
        Optional<Boolean> verificationResult = verify(client);
        if (verificationResult.isPresent()) return verificationResult;
        
        if (ticksMining > 200) return Optional.of(false); // 10s max timeout
        return Optional.empty();
    }

    @Override
    protected void onExecute(Minecraft client) {}

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        if (client.level != null && client.level.getBlockState(this.targetPos).isAir()) {
            return Optional.of(true);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Break Block";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
