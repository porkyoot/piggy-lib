package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Platform-agnostic action to attack an entity.
 * ZERO net.minecraft imports.
 */
public class AttackEntityAction extends AbstractAction {
    private final Supplier<Object> entityLocator;

    public AttackEntityAction(Supplier<Object> entityLocator, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.entityLocator = entityLocator;
    }

    public AttackEntityAction(Object target) {
        super("piggy-lib", ActionPriority.NORMAL);
        this.entityLocator = () -> target;
    }

    @Override
    protected void onExecute(Object client) {
        Object target = entityLocator.get();
        if (target != null) {
            PiggyServiceRegistry.getWorldInteractionAdapter().attackEntity(client, target);
        }
    }

    @Override
    protected Optional<Boolean> verify(Object client) {
        Object target = entityLocator.get();
        boolean dead = target == null || !PiggyServiceRegistry.getWorldStateAdapter().isAlive(target);
        return dead ? Optional.of(true) : Optional.empty();
    }

    @Override
    public String getName() {
        return "Attack Entity";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
