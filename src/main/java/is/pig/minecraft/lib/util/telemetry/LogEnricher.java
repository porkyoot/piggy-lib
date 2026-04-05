package is.pig.minecraft.lib.util.telemetry;

import java.util.Map;

/**
 * Functional interface for enriching telemetry data maps with additional context.
 * Used to decouple environmental monitoring from the core telemetry logging logic.
 */
@FunctionalInterface
public interface LogEnricher {

    /**
     * Enriches the provided map with supplemental data.
     * 
     * @param data the mutable map to which telemetry context should be added.
     */
    void enrich(Map<String, Object> data);
}
