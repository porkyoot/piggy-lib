package is.pig.minecraft.lib.common.action;

@FunctionalInterface
public interface ActionCallback {
    void onResult(boolean success);
}
