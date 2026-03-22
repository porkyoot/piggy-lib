package is.pig.minecraft.lib.action;

import net.minecraft.client.Minecraft;

public interface IAction {
    /**
     * Executes the action.
     * 
     * @param client The Minecraft client instance.
     * @return true if the action is considered 'completed' and should be
     *         removed from the queue, false otherwise.
     */
    boolean execute(Minecraft client);

    /**
     * Optional post-execution verification to determine if the specific action genuinely succeeded.
     * By default returns true for actions that don't support failure tracking.
     */
    default boolean isVerified(Minecraft client) {
        return true;
    }

    default ActionPriority getPriority() {
        return ActionPriority.NORMAL;
    }

    default boolean isClick() {
        return false;
    }

    default boolean isInitiated() {
        return true;
    }

    default boolean ignoreGlobalCps() {
        return false;
    }

    String getSourceMod();

    String getName();
}
