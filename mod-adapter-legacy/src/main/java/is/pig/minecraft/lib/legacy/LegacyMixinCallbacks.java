package is.pig.minecraft.lib.legacy;

public class LegacyMixinCallbacks {
    private static SlotChangeCallback slotChangeCallback;

    public static void setSlotChangeCallback(SlotChangeCallback callback) {
        slotChangeCallback = callback;
    }

    public static void onSlotChange(String source, int slotId, String itemInfo) {
        if (slotChangeCallback != null) {
            slotChangeCallback.onSlotChange(source, slotId, itemInfo);
        }
    }

    @FunctionalInterface
    public interface SlotChangeCallback {
        void onSlotChange(String source, int slotId, String itemInfo);
    }
}
