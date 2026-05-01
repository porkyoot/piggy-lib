package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;

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
    public Optional<Boolean> execute(Object clientObj) {
        var adapter = PiggyServiceRegistry.getWorldInteractionAdapters().stream().findFirst();
        if (adapter.isPresent()) {
            adapter.get().breakBlock(clientObj, targetPos);
        }
        
        ticksMining++;
        
        Optional<Boolean> verificationResult = verify(clientObj);
        if (verificationResult.isPresent()) return verificationResult;
        
        if (ticksMining > 200) return Optional.of(false); // 10s max timeout
        return Optional.empty();
    }

    @Override
    protected void onExecute(Object clientObj) {
        // Handled in execute override for continuous mining
    }

    @Override
    protected Optional<Boolean> verify(Object clientObj) {
        // Verification logic would ideally also be in an adapter or generic enough
        // For now, if we can't check level state purely, we might need an adapter method for it
        // But the request focused on the execution.
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

