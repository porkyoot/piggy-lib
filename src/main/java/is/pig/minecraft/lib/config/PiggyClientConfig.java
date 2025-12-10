package is.pig.minecraft.lib.config;

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

    // --- SINGLETON ACCESS ---

    public static PiggyClientConfig getInstance() {
        return INSTANCE;
    }

    /**
     * Updates the singleton instance. Should only be called by ConfigPersistence.
     * 
     * @param instance The new instance loaded from disk.
     */
    static void setInstance(PiggyClientConfig instance) {
        INSTANCE = instance;
    }


    public boolean isNoCheatingMode() {
        return noCheatingMode;
    }

    public void setNoCheatingMode(boolean noCheatingMode) {
        this.noCheatingMode = noCheatingMode;
    }

}