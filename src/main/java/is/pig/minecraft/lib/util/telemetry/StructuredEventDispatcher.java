package is.pig.minecraft.lib.util.telemetry;
import is.pig.minecraft.api.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central hub for dispatching and enriching structured telemetry events.
 */
public class StructuredEventDispatcher {
    private static final StructuredEventDispatcher INSTANCE = new StructuredEventDispatcher();
    
    private final List<LogEnricher> globalEnrichers = new CopyOnWriteArrayList<>();
    private final List<StructuredEventListener> eventListeners = new CopyOnWriteArrayList<>();

    private StructuredEventDispatcher() {}

    public static StructuredEventDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Registers an enricher that will be applied to every dispatched structured event.
     */
    public void registerEnricher(LogEnricher enricher) {
        if (!globalEnrichers.contains(enricher)) {
            globalEnrichers.add(enricher);
        }
    }

    /**
     * Registers a listener that will be notified of every enriched structured event.
     */
    public void registerListener(StructuredEventListener listener) {
        if (!eventListeners.contains(listener)) {
            eventListeners.add(listener);
        }
    }

    /**
     * Enriches an event through the global enrichment pipeline.
     * @param event The structured event to enrich.
     * @return An EnrichedEventView containing the original event and its global context.
     */
    public EnrichedEventView enrich(StructuredEvent event) {
        // 1. Create a mutable version of the event data
        Map<String, Object> enrichedData = new HashMap<>(event.getEventData());
        
        // 2. Run all global enrichers
        globalEnrichers.forEach(enricher -> enricher.enrich(enrichedData));
        
        return new EnrichedEventView(event, enrichedData);
    }

    /**
     * Dispatches an event through the global enrichment pipeline and forwards it to active loggers and listeners.
     */
    public void dispatch(StructuredEvent event) {
        EnrichedEventView view = enrich(event);
        
        // Forward to MetaActionSession (rolling buffer)
        MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(session -> {
            session.logEnrichedEvent(view);
        });

        // Notify all registered listeners
        eventListeners.forEach(listener -> listener.onEvent(view));
    }

    /**
     * Internal view of an event after enrichment.
     * @param parent       The original structured event.
     * @param enrichedData The mutable data map after global enrichment.
     */

}
