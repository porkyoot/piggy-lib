package is.pig.minecraft.lib.util;

import is.pig.minecraft.api.registry.PiggyServiceRegistry;

/**
 * Platform-agnostic messenger utility that delegates to the MessagingAdapter SPI.
 */
public class PiggyMessenger {

    public static void send(Object player, String message) {
        PiggyServiceRegistry.getMessagingAdapter().sendMessage(player, message, false);
    }

    public static void sendOverlay(Object player, String message) {
        PiggyServiceRegistry.getMessagingAdapter().sendMessage(player, message, true);
    }

    public static void sendClickable(Object player, String message, String action, String value) {
        PiggyServiceRegistry.getMessagingAdapter().sendClickableMessage(player, message, action, value);
    }
    
    public static void log(String message) {
        PiggyServiceRegistry.getMessagingAdapter().logToConsole(message);
    }
}
