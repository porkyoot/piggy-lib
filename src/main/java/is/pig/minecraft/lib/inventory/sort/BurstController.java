package is.pig.minecraft.lib.inventory.sort;

import java.util.List;

/**
 * Manages the "Burst Size" of inventory actions using AIMD (Additive Increase, Multiplicative Decrease).
 * This acts as a congestion control mechanism to prevent server-side desyncs during high-volume sorting.
 */
public class BurstController {
    private int currentWindow = 50;
    private final int minWindow = 1;
    private final int maxWindow = 200;

    /**
     * Increases the window size linearly after a successful verified burst.
     */
    public void reportSuccess() {
        this.currentWindow = Math.min(currentWindow + 5, maxWindow);
    }

    /**
     * Halves the window size immediately after a detected state desync.
     */
    public void reportDesync() {
        this.currentWindow = Math.max(currentWindow / 2, minWindow);
    }

    /**
     * Slices the next batch of moves according to the current window size.
     *
     * @param allMoves The full list of planned moves.
     * @return A sublist containing the next burst of moves.
     */
    public List<Move> getNextBurst(List<Move> allMoves) {
        if (allMoves.isEmpty()) return List.of();
        int burstSize = Math.min(allMoves.size(), currentWindow);
        return allMoves.subList(0, burstSize);
    }

    public int getCurrentWindow() {
        return currentWindow;
    }
}
