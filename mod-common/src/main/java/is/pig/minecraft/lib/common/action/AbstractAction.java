package is.pig.minecraft.lib.common.action;

import is.pig.minecraft.lib.api.IPlayerController;
import java.util.Optional;

public abstract class AbstractAction implements IAction {
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

    protected abstract void onExecute(IPlayerController controller);

    @Override
    public java.util.Optional<Boolean> execute(IPlayerController controller) {
        if (!initiated) {
            onExecute(controller);
            initiated = true;
            return Optional.of(true); // Default to true in pure Java logic
        }

        waitTicks++;
        if (waitTicks >= timeoutTicks) {
            return Optional.of(false);
        }

        return Optional.empty();
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
