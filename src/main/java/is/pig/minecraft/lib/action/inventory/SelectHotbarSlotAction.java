package is.pig.minecraft.lib.action.inventory;

import is.pig.minecraft.lib.action.AbstractAction;
import net.minecraft.client.Minecraft;
import java.util.Optional;

public class SelectHotbarSlotAction extends AbstractAction {
    private final int slot;

    public SelectHotbarSlotAction(int slot, String sourceMod, is.pig.minecraft.lib.action.ActionPriority priority, int timeoutTicks) {
        super(sourceMod, priority, timeoutTicks);
        this.slot = slot;
    }

    public SelectHotbarSlotAction(int slot, String sourceMod, is.pig.minecraft.lib.action.ActionPriority priority) {
        this(slot, sourceMod, priority, 40);
    }

    public SelectHotbarSlotAction(int slot, String sourceMod) {
        this(slot, sourceMod, is.pig.minecraft.lib.action.ActionPriority.NORMAL, 40);
    }

    @Override
    protected void onExecute(Minecraft client) {
        if (client.player != null) {
            client.player.getInventory().selected = this.slot;
            
            if (client.getConnection() != null) {
                client.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(this.slot));
            }
        }
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        if (client.player != null && client.player.getInventory().selected == this.slot) {
            return Optional.of(true);
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Select Hotbar Slot";
    }
}
