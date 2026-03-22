package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;

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
    public boolean execute(Minecraft client) {
        if (client.gameMode != null && client.player != null) {
            if (ticksMining == 0) {
                client.gameMode.startDestroyBlock(this.targetPos, Direction.UP);
            } else {
                client.gameMode.continueDestroyBlock(this.targetPos, Direction.UP);
            }
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        ticksMining++;
        
        if (verify(client)) return true;
        if (ticksMining > 200) return true; // 10s max timeout
        return false;
    }

    @Override
    protected void onExecute(Minecraft client) {}

    @Override
    protected boolean verify(Minecraft client) {
        return client.level != null && client.level.getBlockState(this.targetPos).isAir();
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
