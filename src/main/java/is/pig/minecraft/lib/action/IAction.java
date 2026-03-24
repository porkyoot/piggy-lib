package is.pig.minecraft.lib.action;

import net.minecraft.client.Minecraft;
import java.util.Optional;

public interface IAction {
    /**
     * @return Optional.of(true) if succeeded, Optional.of(false) if failed/timed out, 
     * Optional.empty() if still executing/waiting for verification.
     */
    Optional<Boolean> execute(Minecraft client);

    default ActionPriority getPriority() { return ActionPriority.NORMAL; }
    default boolean isClick() { return false; }
    default boolean isInitiated() { return true; }
    default boolean ignoreGlobalCps() { return false; }
    default boolean isVerified(Minecraft client) { return execute(client).orElse(false); }
    
    String getSourceMod();
    String getName();
}