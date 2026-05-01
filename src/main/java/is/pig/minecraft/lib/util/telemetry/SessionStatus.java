package is.pig.minecraft.lib.util.telemetry;
import is.pig.minecraft.api.*;

/**
 * Represents the lifecycle state of a MetaActionSession.
 */
public enum SessionStatus {
    /**
     * Session is currently recording telemetry.
     */
    ACTIVE,
    
    /**
     * Meta-action completed successfully. Session will be discarded.
     */
    SUCCEEDED,
    
    /**
     * Meta-action is holding for a short window to verify no delayed failure (e.g. death).
     */
    MONITORING,
    
    /**
     * Meta-action failed or was interrupted. Session will be dumped to disk.
     */
    FAILED
}
