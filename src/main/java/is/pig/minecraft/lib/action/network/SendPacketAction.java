package is.pig.minecraft.lib.action.network;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.ActionPriority;
import is.pig.minecraft.api.Action;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import java.util.Optional;

public class SendPacketAction implements Action {
    private final Packet<?> packet;
    private final String sourceMod;
    private final ActionPriority priority;

    public SendPacketAction(Packet<?> packet, String sourceMod, ActionPriority priority) {
        this.packet = packet;
        this.sourceMod = sourceMod;
        this.priority = priority;
    }

    @Override
    public Optional<Boolean> execute(Object clientObj) {
        Minecraft client = (Minecraft) clientObj;
        if (client.getConnection() != null) {
            client.getConnection().send(packet);
        }
        return Optional.of(true);
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
