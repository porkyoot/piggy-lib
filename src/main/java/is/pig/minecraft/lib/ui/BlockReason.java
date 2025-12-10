package is.pig.minecraft.lib.ui;

/**
 * Enum defining why a feature was blocked by anti-cheat enforcement.
 */
public enum BlockReason {
    /**
     * Server has disabled cheats globally or for this specific feature.
     */
    SERVER_ENFORCEMENT,

    /**
     * User's local "No Cheating Mode" configuration is enabled.
     */
    LOCAL_CONFIG
}
