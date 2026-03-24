package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;
import java.util.function.Supplier;

public class MountEntityAction extends AbstractAction {

    private final Supplier<Entity> entitySupplier;
    private final InteractionHand hand;
    private long startTime = 0;

    public MountEntityAction(Supplier<Entity> entitySupplier, InteractionHand hand, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.entitySupplier = entitySupplier;
        this.hand = hand;
    }

    @Override
    protected void onExecute(Minecraft client) {
        if (startTime == 0) startTime = System.currentTimeMillis();
        if (client.player != null && client.gameMode != null) {
            Entity target = entitySupplier.get();
            if (target != null) {
                client.gameMode.interactAt(client.player, target, new EntityHitResult(target), hand);
                client.gameMode.interact(client.player, target, hand);
                client.player.swing(hand);
            }
        }
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        if (client.player == null) return Optional.empty();
        Entity target = entitySupplier.get();
        if (target == null) return Optional.of(false); // Disappeared

        if (client.player.getVehicle() == target || target.hasPassenger(client.player)) {
            return Optional.of(true);
        }
        
        // Timeout if unable to mount
        if (System.currentTimeMillis() - startTime > 1000) {
            return Optional.of(false);
        }
        
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Mount Entity";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
