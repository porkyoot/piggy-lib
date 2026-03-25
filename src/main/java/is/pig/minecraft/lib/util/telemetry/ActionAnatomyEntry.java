package is.pig.minecraft.lib.util.telemetry;

import org.slf4j.event.Level;

/**
 * A structured entry for micro-actions, defining why, when, how, and the outcome.
 */
public record ActionAnatomyEntry(
    long timestamp,
    long tick,
    Level level,
    String why,
    String how,
    String outcome
) implements TelemetryEntry {
    @Override
    public String formatted() {
        return String.format("[%d] [Tick:%d] [Action] WHY: %s | HOW: %s | OUTCOME: %s", 
            timestamp, tick, why, how, outcome);
    }
}
