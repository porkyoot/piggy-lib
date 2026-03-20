package is.pig.minecraft.lib;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PiggyLib implements ModInitializer {
    public static final String MOD_ID = "piggy-lib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Piggy Lib");
        
        is.pig.minecraft.lib.features.CheatFeatureRegistry.register(
                new is.pig.minecraft.lib.features.CheatFeature(
                        "auto_parkour",
                        "Auto Parkour",
                        "Automatically place blocks below you when running and jumping",
                        false));
    }
}
