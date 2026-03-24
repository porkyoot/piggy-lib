package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

import java.util.Optional;

public class ThrowItemAction extends AbstractAction {

    private final InteractionHand hand;
    private int initialCount = -1;

    public ThrowItemAction(InteractionHand hand, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.hand = hand;
    }

    @Override
    protected void onExecute(Minecraft client) {
        if (client.player != null && client.gameMode != null) {
            initialCount = client.player.getItemInHand(hand).getCount();
            client.gameMode.useItem(client.player, hand);
            client.player.swing(hand);
        }
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        if (client.player == null) return Optional.empty();
        
        int currentCount = client.player.getItemInHand(hand).getCount();
        if (currentCount < initialCount || currentCount == 0) {
            return Optional.of(true);
        }
        
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Throw Item";
    }

    @Override
    public boolean isClick() {
        return true;
    }
}
