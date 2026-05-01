package is.pig.minecraft.lib.ui;

/**
 * HUD overlay that renders the blocked action icon below the crosshair.
 * ZERO net.minecraft imports.
 */
public class AntiCheatHudOverlay {
    private static final String BLOCKED_ICON = "piggy:textures/gui/icons/cheating_cancel.png";
    private static boolean registered = false;

    public static void register() {
        if (registered) return;
        registered = true;
    }

    public static void triggerBlockedIcon() {
        IconQueueOverlay.queueIcon(BLOCKED_ICON, 1500, true);
    }
}
