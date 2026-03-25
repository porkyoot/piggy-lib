package is.pig.minecraft.lib.config;

import java.util.Map;

/**
 * Configuration data model for Piggy Build.
 * Holds the state of user settings.
 */
public abstract class PiggyClientConfig<T extends PiggyClientConfig<T>> {
    // Singleton instance
    private static PiggyClientConfig<?> instance;

    public static PiggyClientConfig<?> getInstance() {
        return instance;
    }

    protected static void setGlobalInstance(PiggyClientConfig<?> inst) {
        instance = inst;
    }

    private final transient java.util.List<java.util.function.BiConsumer<Boolean, Map<String, Boolean>>> syncListeners = new java.util.ArrayList<>();

    public void registerConfigSyncListener(java.util.function.BiConsumer<Boolean, Map<String, Boolean>> listener) {
        this.syncListeners.add(listener);
    }

    // Safety settings
    private boolean noCheatingMode = true;
    public transient boolean serverAllowCheats = true; // Runtime override from server
    public transient java.util.Map<String, Boolean> serverFeatures = new java.util.HashMap<>(); // Runtime feature

    private int tickDelay = 1;
    public int globalActionCps = 20;

    // Logging configurations
    private boolean productionMode = true;
    private int maxLogBufferSize = 100;

    public abstract void save();
                                                                                                // overrides

    public PiggyClientConfig() {
        // Automatically register with the registry upon creation?
        // Or let the subclass do it explicitely?
        // Explicit is better for control, but auto ensures we don't forget.
        // Let's rely on explicit registration in Subclass static instantiation or
        // constructor if singleton.
    }

    /**
     * Called when the server synchronizes config settings (anti-cheat).
     * Subclasses can override to perform additional logic, but should call super.
     */
    public void onServerSync(boolean allowCheats, Map<String, Boolean> features) {
        this.serverAllowCheats = allowCheats;
        this.serverFeatures = features;

        for (java.util.function.BiConsumer<Boolean, Map<String, Boolean>> listener : syncListeners) {
            listener.accept(allowCheats, features);
        }
    }

    public boolean isNoCheatingMode() {
        return noCheatingMode;
    }

    public void setNoCheatingMode(boolean noCheatingMode) {
        this.noCheatingMode = noCheatingMode;
        PiggyConfigRegistry.getInstance().syncSharedSettings(this);
    }

    public void setNoCheatingModeInternal(boolean noCheatingMode) {
        this.noCheatingMode = noCheatingMode;
    }

    public int getTickDelay() {
        return tickDelay;
    }

    public void setTickDelay(int tickDelay) {
        this.tickDelay = tickDelay;
        PiggyConfigRegistry.getInstance().syncSharedSettings(this);
    }
    
    public void setTickDelayInternal(int tickDelay) {
        this.tickDelay = tickDelay;
    }

    public boolean isProductionMode() {
        return productionMode;
    }

    public void setProductionMode(boolean productionMode) {
        this.productionMode = productionMode;
        PiggyConfigRegistry.getInstance().syncSharedSettings(this);
    }

    public int getMaxLogBufferSize() {
        return maxLogBufferSize;
    }

    public void setMaxLogBufferSize(int maxLogBufferSize) {
        this.maxLogBufferSize = maxLogBufferSize;
        PiggyConfigRegistry.getInstance().syncSharedSettings(this);
    }

    /**
     * Determines if the global "No Cheating Mode" toggle can be edited.
     * Returns false if the server is enforcing anti-cheat (allowCheats = false).
     */
    public boolean isGlobalCheatsEditable() {
        return this.serverAllowCheats;
    }

}