package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import is.pig.minecraft.lib.action.ActionPriority;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public class ConsumeItemAction extends AbstractAction {

    private final InteractionHand hand;
    private boolean hasStartedConsuming = false;
    private long startConsumeTime = 0;

    public ConsumeItemAction(InteractionHand hand, String sourceMod, ActionPriority priority) {
        super(sourceMod, priority);
        this.hand = hand;
    }

    @Override
    protected void onExecute(Minecraft client) {
        if (client.player != null && client.gameMode != null) {
            if (!hasStartedConsuming) {
                // Determine our target item counts
                ItemStack currentStack = client.player.getItemInHand(hand);
                if (currentStack.isEmpty()) {
                    return; // Nothing to consume
                }
                
                client.options.keyUse.setDown(true);
                client.gameMode.useItem(client.player, hand);
                
                hasStartedConsuming = true;
                startConsumeTime = System.currentTimeMillis();
            }
        }
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        if (client.player == null || !hasStartedConsuming) return Optional.empty();
        
        // Timeout safeguard
        if (System.currentTimeMillis() - startConsumeTime > 5000) {
            client.options.keyUse.setDown(false);
            return Optional.of(false);
        }

        // Wait until it finishes using
        if (!client.player.isUsingItem()) {
            client.options.keyUse.setDown(false);
            return Optional.of(true);
        }

        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Consume Item";
    }

    @Override
    public boolean isClick() {
        return false;
    }
}
