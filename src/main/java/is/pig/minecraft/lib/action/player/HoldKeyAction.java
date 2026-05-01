package is.pig.minecraft.lib.action.player;
import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InputAdapter;

import java.util.Optional;

public class HoldKeyAction extends AbstractAction {
    private final String keyId;
    private final boolean state;

    public HoldKeyAction(String keyId, boolean state, String sourceMod) {
        super(sourceMod);
        this.keyId = keyId;
        this.state = state;
    }

    @Override
    protected void onExecute(Object clientObj) {
        InputAdapter input = PiggyServiceRegistry.getInputAdapter();
        input.setKeyDown(this.keyId, this.state);
    }

    @Override
    protected Optional<Boolean> verify(Object clientObj) {
        return Optional.of(true);
    }

    @Override
    public String getName() {
        return "Hold Key: " + this.keyId + " -> " + this.state;
    }

    @Override
    public boolean isClick() {
        return false;
    }
}
