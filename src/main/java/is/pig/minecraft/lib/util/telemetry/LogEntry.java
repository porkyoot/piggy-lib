package is.pig.minecraft.lib.util.telemetry;

import org.slf4j.event.Level;

/**
 * A standard text-based log entry with full verbosity metrics.
 */
public record LogEntry(
    long timestamp,
    long tick,
    Level level,
    double tps,
    double mspt,
    double cps,
    String pos,
    String message
) implements TelemetryEntry {
    @Override
    public String formatted() {
        return String.format("[%d] [Tick:%d] [%s] [TPS:%.1f MSPT:%.1f CPS:%.1f Pos:%s] %s", 
            timestamp, tick, level, tps, mspt, cps, pos, message);
    }
}
