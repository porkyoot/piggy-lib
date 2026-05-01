package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * Platform-agnostic action to use an item.
 * ZERO net.minecraft imports.
 */
public class UseItemAction extends AbstractAction {
    private final InteractionHand hand;
    private final BooleanSupplier verifyCondition;

    public UseItemAction(InteractionHand hand, String sourceMod, BooleanSupplier verifyCondition) {
        super(sourceMod);
        this.hand = hand;
        this.verifyCondition = verifyCondition;
    }

    public UseItemAction(InteractionHand hand, String sourceMod) {
        this(hand, sourceMod, () -> true);
    }

    @Override
    protected void onExecute(Object client) {
        PiggyServiceRegistry.getWorldInteractionAdapter().useItem(client, this.hand);
    }

    @Override
    protected Optional<Boolean> verify(Object client) {
        return verifyCondition.getAsBoolean() ? Optional.of(true) : Optional.empty();
    }

    @Override
    public String getName() {
        return "Use Item";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
