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

    default ActionPriority getPriority() {
        return ActionPriority.NORMAL;
    }

    default boolean isClick() {
        return false;
    }

    default boolean isInitiated() {
        return true;
    }

    String getSourceMod();

    String getName();
}
