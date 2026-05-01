package is.pig.minecraft.lib.action;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.config.PiggyClientConfig;
import is.pig.minecraft.lib.util.perf.PerfMonitor;

import java.util.List;

/**
 * Manages the "Burst Size" of inventory actions using AIMD (Additive Increase, Multiplicative Decrease).
 * This acts as a congestion control mechanism to prevent server-side desyncs during high-volume sorting.
 */
public class BurstController {
    private int currentWindow;

    public BurstController() {
        var config = PiggyClientConfig.getInstance();
        this.currentWindow = (config != null) ? config.getAimdInitialWindow() : 100;
    }

    /**
     * Increases the window size linearly after a successful verified burst.
     * Adjusts the increase step based on server TPS and network latency.
     */
    public void reportSuccess(int ping) {
        var config = PiggyClientConfig.getInstance();
        double tpsFactor = PerfMonitor.getInstance().getServerTps() / 20.0;
        double latencyPenalty = (ping > 150) ? 0.5 : 1.0;

        int baseIncrease = (config != null) ? config.getAimdIncreaseStep() : 10;
        int adaptiveIncrease = (int) Math.max(1, baseIncrease * tpsFactor * latencyPenalty);

        int maxWindow = (config != null) ? config.getAimdMaxWindow() : 500;
        this.currentWindow = Math.min(currentWindow + adaptiveIncrease, maxWindow);
    }

    /**
     * Halves the window size immediately after a detected state desync.
     */
    public void reportDesync() {
        var config = PiggyClientConfig.getInstance();
        int minWindow = (config != null) ? config.getAimdMinWindow() : 1;
        this.currentWindow = Math.max(currentWindow / 2, minWindow);
    }

    /**
     * Slices the next batch of moves according to the current window size.
     *
     * @param allMoves The full list of planned moves.
     * @return A sublist containing the next burst of moves.
     */
    public <T> List<T> getNextBurst(List<T> allItems) {
        if (allItems.isEmpty()) return List.of();
        int burstSize = Math.min(allItems.size(), currentWindow);
        return allItems.subList(0, burstSize);
    }

    public int getCurrentWindow() {
        return currentWindow;
    }
}
