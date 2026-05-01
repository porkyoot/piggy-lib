package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;

import java.util.Optional;
import java.util.function.BooleanSupplier;

public class InteractBlockAction extends AbstractAction {
    private final BlockHitResult hitResult;
    private final InteractionHand hand;
    private final BooleanSupplier verifyCondition;

    public InteractBlockAction(BlockHitResult hitResult, InteractionHand hand, String sourceMod, BooleanSupplier verifyCondition) {
        super(sourceMod);
        this.hitResult = hitResult;
        this.hand = hand;
        this.verifyCondition = verifyCondition;
    }

    public InteractBlockAction(BlockHitResult hitResult, InteractionHand hand, String sourceMod) {
        this(hitResult, hand, sourceMod, () -> true);
    }

    @Override
    protected void onExecute(Object clientObj) {
        var adapter = PiggyServiceRegistry.getWorldInteractionAdapters().stream().findFirst();
        if (adapter.isPresent()) {
            adapter.get().useItemOn(clientObj, this.hand, this.hitResult);
        }
    }

    @Override
    protected Optional<Boolean> verify(Object clientObj) {
        return verifyCondition.getAsBoolean() ? Optional.of(true) : Optional.empty();
    }

    @Override
    public String getName() {
        return "Interact Block";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}

