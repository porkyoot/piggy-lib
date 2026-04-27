package is.pig.minecraft.lib.common.action.world;

import is.pig.minecraft.lib.common.action.AbstractAction;
import is.pig.minecraft.lib.common.action.ActionPriority;
import is.pig.minecraft.lib.api.IPlayerController;

public class BreakBlockAction extends AbstractAction {
    private final int x;
    private final int y;
    private final int z;
    private final String face;
    private int ticksMining = 0;

    public BreakBlockAction(int x, int y, int z, String face, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.x = x;
        this.y = y;
        this.z = z;
        this.face = face;
    }

    public BreakBlockAction(int x, int y, int z, String face, String sourceMod) {
        this(x, y, z, face, sourceMod, ActionPriority.NORMAL);
    }

    @Override
    protected void onExecute(IPlayerController controller) {
        controller.startBreakingBlock(x, y, z, face);
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
