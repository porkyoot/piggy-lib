package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.IModAdapter;
import is.pig.minecraft.lib.api.IInventoryManager;
import is.pig.minecraft.lib.api.IPlayerTracker;
import is.pig.minecraft.lib.api.INetworkDispatcher;
import is.pig.minecraft.lib.api.I2DRenderer;
import is.pig.minecraft.lib.api.I3DRenderer;
import is.pig.minecraft.lib.api.IScreenManager;
import is.pig.minecraft.lib.api.IPlayerController;

public class LegacyModAdapter implements IModAdapter {
    private final IInventoryManager inventoryManager = new LegacyInventoryManager();
    private final INetworkDispatcher networkDispatcher = new LegacyNetworkDispatcher();
    private final I2DRenderer renderer2D = new Legacy2DRenderer();
    private final I3DRenderer renderer3D = new Legacy3DRenderer();
    private final IScreenManager screenManager = new LegacyScreenManager();
    private final IPlayerController playerController = new LegacyPlayerController();

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
        return networkDispatcher;
    }

    @Override
    public I2DRenderer get2DRenderer() {
        return renderer2D;
    }

    @Override
    public I3DRenderer get3DRenderer() {
        return renderer3D;
    }

    @Override
    public IScreenManager getScreenManager() {
        return screenManager;
    }

    @Override
    public IPlayerController getPlayerController() {
        return playerController;
    }
}
