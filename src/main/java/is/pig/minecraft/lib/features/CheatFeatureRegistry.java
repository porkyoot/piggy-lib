package is.pig.minecraft.lib.features;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for all cheat features across Piggy mods.
 * Mods should register their features during initialization.
 */
public class CheatFeatureRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger("piggy-lib");
    private static final Map<String, CheatFeature> FEATURES = new LinkedHashMap<>();

    /**
     * Registers a new cheat feature.
     * 
     * @param feature The feature to register
     * @throws IllegalArgumentException if a feature with the same ID is already
     *                                  registered
     */
    public static void register(CheatFeature feature) {
        if (FEATURES.containsKey(feature.id())) {
            throw new IllegalArgumentException("Feature with ID '" + feature.id() + "' is already registered");
        }
        FEATURES.put(feature.id(), feature);
        LOGGER.info("Registered cheat feature: {} ({})", feature.displayName(), feature.id());
    }

    /**
     * Gets all registered features.
     * 
     * @return Unmodifiable collection of all registered features
     */
    public static Collection<CheatFeature> getAllFeatures() {
        return Collections.unmodifiableCollection(FEATURES.values());
    }

    /**
     * Gets a feature by its ID.
     * 
     * @param featureId The feature ID
     * @return The feature, or null if not found
     */
    public static CheatFeature getFeature(String featureId) {
        return FEATURES.get(featureId);
    }

    /**
     * Checks if a feature exists.
     * 
     * @param featureId The feature ID
     * @return true if the feature is registered
     */
    public static boolean hasFeature(String featureId) {
        return FEATURES.containsKey(featureId);
    }

    /**
     * Determines if a feature is enabled based on server and client settings.
     * Server settings ALWAYS override client settings.
     * 
     * @param featureId            The feature ID to check
     * @param serverAllowCheats    Whether the server allows cheats (master switch)
     * @param serverFeatures       Map of server-side feature settings (can be null)
     * @param clientNoCheatingMode Whether client has "no cheating mode" enabled
     * @param clientFeatureEnabled Whether the client has this specific feature
     *                             enabled
     * @return true if the feature should be enabled
     */
    public static boolean isFeatureEnabled(
            String featureId,
            boolean serverAllowCheats,
            Map<String, Boolean> serverFeatures,
            boolean clientNoCheatingMode,
            boolean clientFeatureEnabled) {

        // 1. Check if feature exists
        if (!hasFeature(featureId)) {
            LOGGER.warn("Attempted to check unknown feature: {}", featureId);
            return false;
        }

        // 2. Server master switch takes precedence
        if (!serverAllowCheats) {
            return false;
        }

        // 3. Check server-specific feature setting (if provided)
        if (serverFeatures != null && serverFeatures.containsKey(featureId)) {
            return serverFeatures.get(featureId);
        }

        // 4. Fall back to client settings
        // If client has "no cheating mode" enabled, disable all features
        if (clientNoCheatingMode) {
            return false;
        }

        // 5. Use client's feature-specific setting
        return clientFeatureEnabled;
    }

    /**
     * Clears all registered features. Should only be used for testing.
     */
    static void clearForTesting() {
        FEATURES.clear();
    }
}
