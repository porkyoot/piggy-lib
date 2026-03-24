package is.pig.minecraft.lib.action.inventory;

import is.pig.minecraft.lib.action.AbstractAction;
import net.minecraft.client.Minecraft;
import java.util.Optional;

public class SelectHotbarSlotAction extends AbstractAction {
    private final int slot;

    public SelectHotbarSlotAction(int slot, String sourceMod) {
        super(sourceMod);
        this.slot = slot;
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
