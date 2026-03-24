package is.pig.minecraft.lib.action.player;

import is.pig.minecraft.lib.action.AbstractAction;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Optional;

public class HoldKeyAction extends AbstractAction {
    private final KeyMapping keyMapping;
    private final boolean state;

    public HoldKeyAction(KeyMapping keyMapping, boolean state, String sourceMod) {
        super(sourceMod);
        this.keyMapping = keyMapping;
        this.state = state;
    }

    @Override
    protected void onExecute(Minecraft client) {
        this.keyMapping.setDown(this.state);
    }

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        return Optional.of(true);
    }

    @Override
    public String getName() {
        return "Hold Key: " + this.state;
    }

    @Override
    public boolean isClick() {
        return false;
    }
}
