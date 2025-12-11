package is.pig.minecraft.lib.config;

import java.util.Map;

/**
 * Listener for server config sync events. Optional modules can implement
 * this and register with `PiggyClientConfig` to be notified when the
 * server sends new runtime configuration values.
 */
public interface ConfigSyncListener {
    void onServerConfigSynced(boolean allowCheats, Map<String, Boolean> features);
}
