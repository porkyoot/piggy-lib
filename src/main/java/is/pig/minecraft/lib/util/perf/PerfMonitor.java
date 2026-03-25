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

    public void recordAction() {
        actionsThisSecond++;
    }

    public double getClientMspt() {
        return clientMspt;
    }

    public double getCps() {
        return currentCps;
    }

    public int getPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null && mc.player != null) {
            net.minecraft.client.multiplayer.PlayerInfo entry = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (entry != null) {
                return entry.getLatency();
            }
        }
        return -1;
    }

    public String getFormattedGlobalState() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- GLOBAL STATE ---\n");
        sb.append(String.format("Client MSPT: %.2f ms (FPS: %.1f)\n", clientMspt, 1000.0 / Math.max(1, clientMspt)));
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
