package is.pig.minecraft.lib.api;

public interface IModAdapter {
    IInventoryManager getInventoryManager();
    IPlayerTracker getPlayerTracker();
    INetworkDispatcher getNetworkDispatcher();
}
