package is.pig.minecraft.lib.action;

import net.minecraft.client.Minecraft;
import java.util.Optional;

/**
 * Represents a stateful, verifiable action that can be enqueued and executed by
 * the centralized {@code PiggyActionQueue}. 
 * 
 * Actions are processed sequentially and can bypass CPS rate limits if
 * assigned high priority or specific flags.
 */
public interface IAction {
    /**
     * Executes the primary action or its verification phase.
     * 
     * @param client The Minecraft client instance.
     * @return {@code Optional.of(true)} if the action succeeded and is complete.
     *         {@code Optional.of(false)} if the action failed or timed out.
     *         {@code Optional.empty()} if still waiting for server/world verification.
     */
    Optional<Boolean> execute(Minecraft client);

    /**
     * @return The priority level for queue sequencing.
     */
    default ActionPriority getPriority() { return ActionPriority.NORMAL; }

    /**
     * @return True if this action represents a simulated mouse click, subject to CPS limits.
     */
    default boolean isClick() { return false; }

    /**
     * @return True if the action has already sent its initial packet/interaction.
     */
    default boolean isInitiated() { return true; }

    /**
     * @return True if this action should ignore global CPS rate limits regardless of priority.
     */
    default boolean ignoreGlobalCps() { return false; }
    default boolean isVerified(Minecraft client) { return execute(client).orElse(false); }
    
    String getSourceMod();
    String getName();
}