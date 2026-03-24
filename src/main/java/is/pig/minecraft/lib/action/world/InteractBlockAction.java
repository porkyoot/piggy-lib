package is.pig.minecraft.lib.action.world;

import is.pig.minecraft.lib.action.AbstractAction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
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
    protected void onExecute(Minecraft client) {
        if (client.player != null && client.gameMode != null) {
            InteractionResult result = client.gameMode.useItemOn(client.player, this.hand, this.hitResult);
            if (!result.consumesAction()) {
                InteractionResult useResult = client.gameMode.useItem(client.player, this.hand);
                if (useResult.shouldSwing() || result.shouldSwing()) {
                    client.player.swing(this.hand);
                }
            } else if (result.shouldSwing()) {
                client.player.swing(this.hand);
            }
        }
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
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
