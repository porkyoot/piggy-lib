package is.pig.minecraft.lib.util;
import is.pig.minecraft.api.*;

import is.pig.minecraft.lib.I18n;
import is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry;
import is.pig.minecraft.api.StructuredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;

/**
 * Standardized logging utility for Piggy mods, ensuring a consolidated naming prefix.
 */
public class PiggyLog {

    private static final String DEFAULT_PREFIX = "[PiggyMods]";

    public static Logger create(String modId) {
        return LoggerFactory.getLogger(modId);
    }

    private final Logger logger;
    private final String prefix;

    public PiggyLog(String modId) {
        this.logger = LoggerFactory.getLogger(modId);
        this.prefix = DEFAULT_PREFIX;
    }

    public PiggyLog(String modId, String subPrefix) {
        this.logger = LoggerFactory.getLogger(modId);
        this.prefix = DEFAULT_PREFIX + " [" + subPrefix + "]";
    }

    public void info(String message, Object... args) {
        logger.info(prefix + " " + message, args);
    }

    public void warn(String message, Object... args) {
        logger.warn(prefix + " " + message, args);
    }

    public void error(String message, Object... args) {
        logger.error(prefix + " " + message, args);
    }

    public void error(String message, Throwable t) {
        logger.error(prefix + " " + message, t);
    }

    public void debug(String message, Object... args) {
        logger.debug(prefix + " " + message, args);
    }

    /**
     * Humanizes and logs a structured event, ensuring it passes through the global enrichment pipeline.
     * Retrieving the translator from {@link EventTranslatorRegistry} and applying the current {@link I18n} instance.
     *
     * @param event the structured event to log
     */
    public void logEvent(StructuredEvent event) {
        // 1. Dispatch through global enrichment system (records it in active sessions)
        is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().dispatch(event);
        
        // 2. Perform global enrichment for the immediate log output
        var view = is.pig.minecraft.lib.util.telemetry.StructuredEventDispatcher.getInstance().enrich(event);
        
        // 3. Humanize and log
        BiFunction<StructuredEvent, I18n, String> translator = EventTranslatorRegistry.getInstance().getTranslator(view.parent().getClass());
        if (translator != null) {
            String humanized = translator.apply(view.parent(), I18n.getInstance());
            // Technical | Human dual-view in console/main log
            this.info(humanized + " | Data: " + view.enrichedData());
        } else {
            // Fallback for unregistered events
            this.info("Event: {} | Data: {}", view.parent().getEventKey(), view.enrichedData());
        }
    }
}
