package is.pig.minecraft.lib.action.network;

import is.pig.minecraft.lib.action.ActionPriority;
import is.pig.minecraft.lib.action.IAction;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;

public class SendPacketAction implements IAction {
    private final Packet<?> packet;
    private final String sourceMod;
    private final ActionPriority priority;

    public SendPacketAction(Packet<?> packet, String sourceMod, ActionPriority priority) {
        this.packet = packet;
        this.sourceMod = sourceMod;
        this.priority = priority;
    }

    @Override
    public boolean execute(Minecraft client) {
        if (client.getConnection() != null) {
            client.getConnection().send(packet);
        }
        return true;
    }

    @Override
    public ActionPriority getPriority() {
        return priority;
    }

    @Override
    public String getSourceMod() {
        return sourceMod;
    }

    @Override
    public String getName() {
        return "Send Packet";
    }
}
