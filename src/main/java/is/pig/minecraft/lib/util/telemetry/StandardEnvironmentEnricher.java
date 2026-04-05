package is.pig.minecraft.lib.util.telemetry;

import is.pig.minecraft.lib.util.perf.PerfMonitor;
import net.minecraft.client.Minecraft;

import java.util.Map;

/**
 * Captures standard environment metrics for every telemetry entry.
 * Includes TPS, MSPT, CPS, and the player's world position.
 */
public class StandardEnvironmentEnricher implements LogEnricher {

    @Override
    public void enrich(Map<String, Object> data) {
        PerfMonitor perf = PerfMonitor.getInstance();
        
        data.put("tps", perf.getServerTps());
        data.put("mspt", perf.getClientMspt());
        data.put("cps", perf.getCps());

        String posStr = "n/a";
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            var p = mc.player;
            posStr = String.format("(%.1f,%.1f,%.1f)", p.getX(), p.getY(), p.getZ());
        }
        data.put("pos", posStr);
    }
}
