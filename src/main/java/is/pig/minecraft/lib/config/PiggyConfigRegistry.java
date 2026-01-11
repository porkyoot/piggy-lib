package is.pig.minecraft.lib.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry to manage all PiggyClientConfig instances.
 * Handles distribution of server sync events to all registered configs.
 */
public class PiggyConfigRegistry {

    private static final PiggyConfigRegistry INSTANCE = new PiggyConfigRegistry();
    private final List<PiggyClientConfig<?>> configs = new CopyOnWriteArrayList<>();

    // Private constructor for singleton
    private PiggyConfigRegistry() {
    }

    public static PiggyConfigRegistry getInstance() {
        return INSTANCE;
    }

    public void register(PiggyClientConfig<?> config) {
        if (config != null && !configs.contains(config)) {
            configs.add(config);
        }
    }

    public void unregister(PiggyClientConfig<?> config) {
        if (config != null) {
            configs.remove(config);
        }
    }

    /**
     * Notifies all registered configs of server settings.
     * 
     * @param allowCheats Whether cheats are allowed globally.
     * @param features    Map of specific feature toggles.
     */
    public void notifyConfigSynced(boolean allowCheats, Map<String, Boolean> features) {
        for (PiggyClientConfig<?> config : configs) {
            try {
                config.onServerSync(allowCheats, features);
            } catch (Throwable t) {
                // Prevent one config failure from stopping others
                t.printStackTrace();
            }
        }
    }
}
