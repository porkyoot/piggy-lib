package is.pig.minecraft.lib.util.telemetry;
import is.pig.minecraft.api.*;

import org.slf4j.event.Level;

/**
 * A structured entry for micro-actions, defining why, when, how, and the outcome with full verbosity metrics.
 */
public record ActionAnatomyEntry(
    long timestamp,
    long tick,
    Level level,
    double tps,
    double mspt,
    double cps,
    String pos,
    String why,
    String how,
    String outcome
) implements TelemetryEntry {
    @Override
    public String formatted() {
        return String.format("[%d] [Tick:%d] [Action] [TPS:%.1f MSPT:%.1f CPS:%.1f Pos:%s] WHY: %s | HOW: %s | OUTCOME: %s", 
            timestamp, tick, tps, mspt, cps, pos, why, how, outcome);
    }
}
