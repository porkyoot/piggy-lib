package is.pig.minecraft.lib.util.perf;

import net.minecraft.client.Minecraft;
import is.pig.minecraft.lib.config.PiggyClientConfig;

/**
 * Tracks global performance metrics for telemetry.
 */
public class PerfMonitor {
    private static final PerfMonitor INSTANCE = new PerfMonitor();
    
    private long lastTickTime = System.nanoTime();
    private double clientMspt = 50.0;
    private long lastCpsReset = System.currentTimeMillis();
    private int actionsThisSecond = 0;
    private double currentCps = 0;

    private double serverTps = 20.0;
    private long lastWorldTickTime = System.currentTimeMillis();

    private PerfMonitor() {}

    public static PerfMonitor getInstance() {
        return INSTANCE;
    }

    /**
     * Should be called every client tick.
     */
    public void tick(Minecraft client) {
        long now = System.nanoTime();
        // Simple smoothing
        double instantMspt = (now - lastTickTime) / 1_000_000.0;
        clientMspt = (clientMspt * 0.9) + (instantMspt * 0.1);
        lastTickTime = now;

        // Update CPS
        long nowMs = System.currentTimeMillis();
        if (nowMs - lastCpsReset >= 1000) {
            currentCps = actionsThisSecond;
            actionsThisSecond = 0;
            lastCpsReset = nowMs;
        }
    }

    /**
     * Estimates server TPS using delta between world ticks.
     */
    public void onWorldTickEnd() {
        long now = System.currentTimeMillis();
        long delta = Math.max(1, now - lastWorldTickTime);
        lastWorldTickTime = now;

        // Apply smoothing factor to the TPS estimate
        double instantTps = Math.min(20.0, 1000.0 / Math.max(50, delta));
        serverTps = (serverTps * 0.9) + (instantTps * 0.1);
    }

    public void recordAction() {
        actionsThisSecond++;
    }

    public double getClientMspt() {
        return clientMspt;
    }

    public double getCps() {
        return currentCps;
    }

    public double getServerTps() {
        return serverTps;
    }

    public int getPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.player != null) {
            var entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            return entry != null ? entry.getLatency() : 0;
        }
        return 0;
    }

    public String getFormattedGlobalState() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- GLOBAL STATE ---\n");
        sb.append(String.format("Client MSPT: %.2f ms (FPS: %.1f)\n", clientMspt, 1000.0 / Math.max(1, clientMspt)));
        sb.append(String.format("Server TPS: %.1f\n", serverTps));
        sb.append(String.format("Current CPS: %.1f\n", currentCps));
        sb.append(String.format("Player Ping: %d ms\n", getPing()));
        
        // Configs
        sb.append("Enabled Features: ");
        var config = PiggyClientConfig.getInstance();
        if (config != null && config.serverFeatures != null) {
            config.serverFeatures.forEach((k, v) -> {
                if (v) sb.append(k).append(", ");
            });
        }
        sb.append("\n--------------------\n");
        return sb.toString();
    }
}
