package is.pig.minecraft.lib.api;

public interface IModAdapter {
    IInventoryManager getInventoryManager();
    IPlayerTracker getPlayerTracker();
    INetworkDispatcher getNetworkDispatcher();
    I2DRenderer get2DRenderer();
    I3DRenderer get3DRenderer();
    IScreenManager getScreenManager();
    IPlayerController getPlayerController();
}
