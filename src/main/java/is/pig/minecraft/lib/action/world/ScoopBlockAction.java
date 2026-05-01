package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;

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
    protected void onExecute(Object clientObj) {
        var adapter = PiggyServiceRegistry.getWorldInteractionAdapters().stream().findFirst();
        if (adapter.isPresent()) {
            BlockHitResult hitResult = new BlockHitResult(Vec3.atCenterOf(targetPos), Direction.UP, targetPos, false);
            adapter.get().useItemOn(clientObj, this.hand, hitResult);
        }
    }

    @Override
    protected Optional<Boolean> verify(Object clientObj) {
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

