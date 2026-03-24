package is.pig.minecraft.lib.features;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import java.util.Optional;

/**
 * Standardized contract for all Piggy Mod features.
 */
public interface IPiggyFeature {
    String getName();
    String getDescription();
    
    /**
     * @return The resource location of the feature's icon for the HUD.
     */
    ResourceLocation getIcon();
    
    /**
     * @return An optional KeyMapping. If present, the centralized HUD/Input manager will handle it.
     */
    Optional<KeyMapping> getKeybinding();
    
    boolean isEnabled();
    void toggle();
    
    /**
     * Integrates with piggy-admin server constraints.
     */
    default boolean isAllowedByServer(java.util.Map<String, Boolean> serverFeatures) {
        return serverFeatures.getOrDefault(getName(), true);
    }
}