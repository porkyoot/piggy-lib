package is.pig.minecraft.lib.action;

import is.pig.minecraft.lib.util.PiggyLog;
import net.minecraft.client.Minecraft;

public abstract class AbstractAction implements IAction {
    private static final PiggyLog LOGGER = new PiggyLog("piggy-lib", "ActionQueue");

    private boolean initiated = false;
    private int waitTicks = 0;
    private final int timeoutTicks;
    private final String sourceMod;
    private final ActionPriority priority;
    private boolean ignoreGlobalCps = false;

    public AbstractAction(String sourceMod) {
        this(sourceMod, ActionPriority.NORMAL, 10);
    }

    public AbstractAction(String sourceMod, ActionPriority priority) {
        this(sourceMod, priority, 10);
    }

    public AbstractAction(String sourceMod, ActionPriority priority, int timeoutTicks) {
        this.sourceMod = sourceMod;
        this.priority = priority;
        this.timeoutTicks = timeoutTicks;
    }

    protected abstract void onExecute(Minecraft client);

    protected abstract boolean verify(Minecraft client);

    @Override
    public boolean execute(Minecraft client) {
        if (!initiated) {
            onExecute(client);
            initiated = true;
            if (verify(client)) {
                return true;
            }
            return false;
        }

        waitTicks++;

        if (verify(client)) {
            return true;
        }

        if (waitTicks >= timeoutTicks) {
            LOGGER.warn("Action '{}' from '{}' timed out", getName(), getSourceMod());
            return true;
        }

        return false;
    }

    @Override
    public ActionPriority getPriority() {
        return priority;
    }

    @Override
    public String getSourceMod() {
        return sourceMod;
    }

    @Override
    public boolean isInitiated() {
        return initiated;
    }

    public void setIgnoreGlobalCps(boolean ignoreGlobalCps) {
        this.ignoreGlobalCps = ignoreGlobalCps;
    }

    @Override
    public boolean ignoreGlobalCps() {
        return ignoreGlobalCps;
    }

    @Override
    public boolean isVerified(Minecraft client) {
        return verify(client);
    }
}
