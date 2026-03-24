package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ScoopBlockAction extends AbstractAction {
    private final BlockPos targetPos;
    private final InteractionHand hand;

    public ScoopBlockAction(BlockPos targetPos, InteractionHand hand, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.targetPos = targetPos;
        this.hand = hand;
    }

    public ScoopBlockAction(BlockPos targetPos, InteractionHand hand, String sourceMod) {
        this(targetPos, hand, sourceMod, ActionPriority.NORMAL);
    }

    @Override
    protected void onExecute(Minecraft client) {
        if (client.player != null && client.gameMode != null && client.level != null) {
            if (client.player.getItemInHand(hand).getItem() != net.minecraft.world.item.Items.BUCKET) {
                return; // Silence the interaction if we already successfully pulled the liquid natively!
            }
            
            // First we must look at the block to ensure server raycast succeeds
            Vec3 target = Vec3.atCenterOf(targetPos);
            Vec3 eyePos = client.player.getEyePosition();
            double dX = target.x - eyePos.x;
            double dY = target.y - eyePos.y;
            double dZ = target.z - eyePos.z;
            double distance = Math.sqrt(dX * dX + dZ * dZ);
            float targetYaw = (float) (Math.atan2(dZ, dX) * (180.0 / Math.PI)) - 90.0f;
            float targetPitch = (float) -(Math.atan2(dY, distance) * (180.0 / Math.PI));
            
            client.player.setXRot(targetPitch);
            client.player.setYRot(targetYaw);
            
            BlockHitResult hitResult = new BlockHitResult(target, net.minecraft.core.Direction.UP, targetPos, false);
            client.gameMode.useItemOn(client.player, hand, hitResult);
            client.gameMode.useItem(client.player, hand);
        }
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        return Optional.of(true);
    }

    @Override
    public String getName() {
        return "Scoop Block";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
