package is.pig.minecraft.lib.ui;

import net.minecraft.resources.ResourceLocation;

/**
 * HUD overlay that renders the blocked action icon below the crosshair.
 */
public class AntiCheatHudOverlay {
    private static final ResourceLocation BLOCKED_ICON = ResourceLocation.fromNamespaceAndPath("piggy",
            "textures/gui/icons/cheating_cancel.png");
    private static boolean registered = false;

    /**
     * Replaced by the centralized IconQueueOverlay, preserving the interface.
     */
    public static void register() {
        // No-op or we can leave it to avoid breaking other things that call register()
        if (registered) {
            return;
        }
        registered = true;
    }

    public static void triggerBlockedIcon() {
        IconQueueOverlay.queueIcon(BLOCKED_ICON, 1500, true);
    }
}
