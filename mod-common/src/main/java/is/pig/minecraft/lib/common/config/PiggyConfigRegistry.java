package is.pig.minecraft.lib.common.config;

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

    public void notifyConfigSynced(boolean allowCheats, Map<String, Boolean> features) {
        for (PiggyClientConfig<?> config : configs) {
            try {
                config.onServerSync(allowCheats, features);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public void syncSharedSettings(PiggyClientConfig<?> source) {
        for (PiggyClientConfig<?> config : configs) {
            if (config != source) {
                config.setNoCheatingModeInternal(source.isNoCheatingMode());
                config.setTickDelayInternal(source.getTickDelay());
                config.setFullActionDebugInternal(source.isFullActionDebug());
                config.save();
            }
        }
    }
}
