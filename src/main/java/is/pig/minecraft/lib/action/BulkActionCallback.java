package is.pig.minecraft.lib.action;
import is.pig.minecraft.api.*;

import java.util.List;

public interface BulkActionCallback {
    /**
     * Triggered when a BulkAction completes its wait phase.
     * 
     * @param fullySuccessful true if all sub-actions independently passed verification
     * @param failedActions   a list of actions that did not pass their verification bounds, for rollback or tracking
     */
    void onComplete(boolean fullySuccessful, List<Action> failedActions);
}
