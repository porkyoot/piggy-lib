package is.pig.minecraft.lib.action;

/**
 * Functional interface for tracking the outcome of a stateful action.
 */
@FunctionalInterface
public interface ActionCallback {
    /**
     * Called when an action either succeeds or definitively fails (e.g., verification mismatch or timeout).
     * 
     * @param success True if the action effects were verified, false otherwise.
     */
    void onResult(boolean success);
}
