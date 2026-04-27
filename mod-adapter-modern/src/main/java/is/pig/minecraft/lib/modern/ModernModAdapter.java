package is.pig.minecraft.lib.modern;

import is.pig.minecraft.lib.api.IModAdapter;
import is.pig.minecraft.lib.api.IInventoryManager;
import is.pig.minecraft.lib.api.IPlayerTracker;
import is.pig.minecraft.lib.api.INetworkDispatcher;

public class ModernModAdapter implements IModAdapter {
    private final IInventoryManager inventoryManager = new ModernInventoryManager();

    @Override
    public IInventoryManager getInventoryManager() {
        return inventoryManager;
    }

    @Override
    public IPlayerTracker getPlayerTracker() {
        return null;
    }

    @Override
    public INetworkDispatcher getNetworkDispatcher() {
        return null;
    }
}
