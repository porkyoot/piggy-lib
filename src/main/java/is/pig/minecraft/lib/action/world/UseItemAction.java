package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import java.util.function.BooleanSupplier;

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
    protected void onExecute(Minecraft client) {
        if (client.player != null && client.gameMode != null) {
            client.gameMode.useItem(client.player, this.hand);
            client.player.swing(this.hand);
        }
    }

    @Override
    protected boolean verify(Minecraft client) {
        return verifyCondition.getAsBoolean();
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
