package is.pig.minecraft.lib.features;

/**
 * Represents a cheat feature that can be registered and controlled.
 * Features can be enabled/disabled both on the server and client side.
 * 
 * @param id             Unique identifier for the feature (e.g., "fast_place")
 * @param displayName    Human-readable name for the feature
 * @param description    Brief description of what the feature does
 * @param defaultEnabled Whether the feature is enabled by default
 */
public record CheatFeature(
        String id,
        String displayName,
        String description,
        boolean defaultEnabled) {
    /**
     * Creates a new cheat feature.
     * 
     * @throws IllegalArgumentException if id is null or empty
     */
    public CheatFeature {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Feature ID cannot be null or empty");
        }
        if (displayName == null || displayName.isEmpty()) {
            throw new IllegalArgumentException("Feature display name cannot be null or empty");
        }
    }
}
