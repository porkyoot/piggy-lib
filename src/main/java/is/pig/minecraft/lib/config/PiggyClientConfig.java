package is.pig.minecraft.lib.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Configuration data model for Piggy Build.
 * Holds the state of user settings.
 */
public class PiggyClientConfig {

    private static PiggyClientConfig INSTANCE = new PiggyClientConfig();

    // Safety settings
    private boolean noCheatingMode = true;
    public transient boolean serverAllowCheats = true; // Runtime override from server
    public transient java.util.Map<String, Boolean> serverFeatures = new java.util.HashMap<>(); // Runtime feature
                                                                                                // overrides

    // --- Listener support ---
    // Marked transient to prevent Gson from trying to serialize listeners to the config file
    private final transient List<ConfigSyncListener> syncListeners = new CopyOnWriteArrayList<>();

    public void registerConfigSyncListener(ConfigSyncListener listener) {
        if (listener != null) syncListeners.add(listener);
    }

    public void unregisterConfigSyncListener(ConfigSyncListener listener) {
        if (listener != null) syncListeners.remove(listener);
    }

    public void notifyConfigSyncListeners(boolean allowCheats, Map<String, Boolean> features) {
        for (ConfigSyncListener l : syncListeners) {
            try {
                l.onServerConfigSynced(allowCheats, features);
            } catch (Throwable t) {
                // Swallow exceptions from listeners to avoid breaking the notification loop
            }
        }
    }

    // --- SINGLETON ACCESS ---

    public static PiggyClientConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Updates the singleton instance. Should only be called by ConfigPersistence.
     * * @param instance The new instance loaded from disk.
     */
    public static void setInstance(PiggyClientConfig instance) {
        INSTANCE = instance;
    }


    public boolean isNoCheatingMode() {
        return noCheatingMode;
    }

    public void setNoCheatingMode(boolean noCheatingMode) {
        this.noCheatingMode = noCheatingMode;
    }

    /**
     * Determines if the global "No Cheating Mode" toggle can be edited.
     * Returns false if the server is enforcing anti-cheat (allowCheats = false).
     */
    public boolean isGlobalCheatsEditable() {
        return this.serverAllowCheats;
    }

}