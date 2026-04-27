package is.pig.minecraft.lib.util.telemetry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unified event dispatcher for Piggy Mods.
 * Manages event history stores and provides a central pipeline for telemetry.
 */
public class PiggyEventDispatcher {
    private static final PiggyEventDispatcher INSTANCE = new PiggyEventDispatcher();
    
    private final Map<String, AbstractHistoryStore<?>> stores = new ConcurrentHashMap<>();

    private PiggyEventDispatcher() {
        // Bridge with StructuredEventDispatcher to ensure all events flow through stores
        StructuredEventDispatcher.getInstance().registerListener(view -> {
            stores.values().forEach(store -> store.onEvent(view));
        });

    }

    public static PiggyEventDispatcher getInstance() {
        return INSTANCE;
    }

    /**
     * Dispatches an event to the global telemetry pipeline.
     */
    public void dispatch(StructuredEvent event) {
        StructuredEventDispatcher.getInstance().dispatch(event);
    }

    /**
     * Registers a history store with the dispatcher.
     */
    public void registerStore(String key, AbstractHistoryStore<?> store) {
        stores.put(key, store);
    }

    /**
     * Retrieves a registered store by key.
     */
    @SuppressWarnings("unchecked")
    public <T extends AbstractHistoryStore<?>> T getStore(String key) {
        return (T) stores.get(key);
    }
}
