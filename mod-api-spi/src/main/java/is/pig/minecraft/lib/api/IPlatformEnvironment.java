package is.pig.minecraft.lib.api;

import java.nio.file.Path;

public interface IPlatformEnvironment {
    Path getConfigDirectory();
    boolean isClient();
    boolean isDedicatedServer();
}
