package is.pig.minecraft.lib.common.action;

import is.pig.minecraft.lib.api.IPlayerController;
import java.util.Optional;

public interface IAction {
    Optional<Boolean> execute(IPlayerController controller);
    
    default ActionPriority getPriority() { return ActionPriority.NORMAL; }
    default boolean isClick() { return false; }
    default boolean isInitiated() { return true; }
    default boolean ignoreGlobalCps() { return false; }
    default java.util.Optional<ActionCallback> getCallback() { return java.util.Optional.empty(); }
    
    String getSourceMod();
    String getName();
}
