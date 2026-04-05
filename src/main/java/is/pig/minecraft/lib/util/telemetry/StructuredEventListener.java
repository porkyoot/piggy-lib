package is.pig.minecraft.lib.util.telemetry;

/**
 * Interface for components that wish to listen to and process structured events globally.
 */
@FunctionalInterface
public interface StructuredEventListener {
    /**
     * Called when a structured event is dispatched and enriched.
     * @param view the enriched event view containing original event and global context
     */
    void onEvent(StructuredEventDispatcher.EnrichedEventView view);
}
