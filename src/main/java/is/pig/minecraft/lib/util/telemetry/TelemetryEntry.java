package is.pig.minecraft.lib.util.telemetry;

import org.slf4j.event.Level;

/**
 * Base interface for all telemetry entries in the rolling buffer.
 */
public interface TelemetryEntry {
    long timestamp();
    long tick();
    Level level();
    String formatted();
}
