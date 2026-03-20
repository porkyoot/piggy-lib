package is.pig.minecraft.lib.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
}
