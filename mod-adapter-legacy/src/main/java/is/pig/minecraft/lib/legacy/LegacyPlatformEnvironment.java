package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.IPlatformEnvironment;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public class LegacyPlatformEnvironment implements IPlatformEnvironment {

    @Override
    public Path getConfigDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }

    @Override
    public boolean isDedicatedServer() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER;
    }
}
