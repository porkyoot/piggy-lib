package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.IAction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

public class AttackEntityAction implements IAction {
    private final Entity target;

    public AttackEntityAction(Entity target) {
        this.target = target;
    }

    @Override
    public boolean execute(Minecraft client) {
        if (client.player != null && client.gameMode != null) {
            client.gameMode.attack(client.player, this.target);
            client.player.swing(InteractionHand.MAIN_HAND);
        }
        return true;
    }

    @Override
    public String getSourceMod() {
        return "piggy-lib";
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
