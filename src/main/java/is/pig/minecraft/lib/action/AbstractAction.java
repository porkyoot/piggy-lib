package is.pig.minecraft.lib.action;

import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;
import java.util.Optional;

public abstract class AbstractAction implements IAction {
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "AbstractAction");

    private boolean initiated = false;
    private int waitTicks = 0;
    private final int timeoutTicks;
    private final String sourceMod;
    private final ActionPriority priority;
    private boolean ignoreGlobalCps = false;
    private java.util.Optional<ActionCallback> callback = java.util.Optional.empty();

    public AbstractAction(String sourceMod, ActionPriority priority, int timeoutTicks) {
        this.sourceMod = sourceMod;
        this.priority = priority;
        this.timeoutTicks = timeoutTicks;
    }

    public AbstractAction(String sourceMod, ActionPriority priority) {
        this(sourceMod, priority, 40);
    }

    public AbstractAction(String sourceMod) {
        this(sourceMod, ActionPriority.NORMAL, 40);
    }

    protected abstract void onExecute(Minecraft client);

    /**
     * @return Optional.empty() while waiting, Optional.of(true/false) upon definitive result.
     */
    protected abstract Optional<Boolean> verify(Minecraft client);

    @Override
    public java.util.Optional<Boolean> execute(Minecraft client) {
        if (!initiated) {
            if (!checkPreconditions(client)) {
                LOGGER.warn("Preconditions failed for action '{}' - Aborting", getName());
                return java.util.Optional.of(false);
            }
            onExecute(client);
            initiated = true;
            return verify(client); // Check immediately (supports 0-tick actions)
        }

        waitTicks++;
        Optional<Boolean> verificationResult = verify(client);
        
        if (verificationResult.isPresent()) {
            return verificationResult;
        }

        if (waitTicks >= timeoutTicks) {
            LOGGER.warn("Action '{}' from '{}' timed out after {} ticks", getName(), getSourceMod(), waitTicks);
            return Optional.of(false);
        }

        return Optional.empty(); // Keep waiting
    }

    @Override
    public ActionPriority getPriority() { return priority; }
    @Override
    public String getSourceMod() { return sourceMod; }
    @Override
    public boolean isInitiated() { return initiated; }
    public void setIgnoreGlobalCps(boolean ignore) { this.ignoreGlobalCps = ignore; }
    @Override
    public boolean ignoreGlobalCps() { return ignoreGlobalCps; }
    @Override
    public java.util.Optional<ActionCallback> getCallback() { return callback; }
    public void setCallback(ActionCallback callback) { this.callback = java.util.Optional.ofNullable(callback); }
    @Override
    public String getName() { return getClass().getSimpleName(); }
}