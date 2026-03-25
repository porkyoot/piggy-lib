package is.pig.minecraft.lib.util.telemetry;

import org.slf4j.event.Level;

/**
 * A standard text-based log entry.
 */
public record LogEntry(
    long timestamp,
    long tick,
    Level level,
    String message
) implements TelemetryEntry {
    @Override
    public String formatted() {
        return String.format("[%d] [Tick:%d] [%s] %s", timestamp, tick, level, message);
    }
}
