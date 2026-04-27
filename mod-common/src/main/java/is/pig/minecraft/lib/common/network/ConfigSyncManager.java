package is.pig.minecraft.lib.common.network;

import com.google.gson.Gson;
import is.pig.minecraft.lib.api.IModAdapter;
import is.pig.minecraft.lib.api.INetworkDispatcher;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;

public class ConfigSyncManager {
    private static final Gson GSON = new Gson();

    public static void syncToClient(UUID playerUuid, boolean allowCheats, Map<String, Boolean> features) {
        IModAdapter adapter = ServiceLoader.load(IModAdapter.class).findFirst()
            .orElseThrow(() -> new IllegalStateException("No IModAdapter found"));
        INetworkDispatcher dispatcher = adapter.getNetworkDispatcher();
        
        if (dispatcher != null) {
            SyncConfigData data = new SyncConfigData(allowCheats, features);
            String json = GSON.toJson(data);
            byte[] payload = json.getBytes(StandardCharsets.UTF_8);
            dispatcher.sendToClient(playerUuid, "piggy:sync_config", payload);
        }
    }

    private record SyncConfigData(boolean allowCheats, Map<String, Boolean> features) {}
}
