package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

import java.util.function.Supplier;

public class AttackEntityAction extends AbstractAction {
    private final Supplier<Entity> entityLocator;

    public AttackEntityAction(Supplier<Entity> entityLocator, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.entityLocator = entityLocator;
    }

    public AttackEntityAction(Entity target) {
        super("piggy-lib", ActionPriority.NORMAL);
        this.entityLocator = () -> target;
    }

    @Override
    public boolean execute(Minecraft client) {
        onExecute(client);
        boolean isDone = verify(client);
        if (isDone) return true;
        return super.execute(client);
    }

    @Override
    protected void onExecute(Minecraft client) {
        Entity target = entityLocator.get();
        if (target != null && client.player != null && client.gameMode != null) {
            client.gameMode.attack(client.player, target);
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    @Override
    protected boolean verify(Minecraft client) {
        Entity target = entityLocator.get();
        return target == null || !target.isAlive();
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
