package is.pig.minecraft.lib;

import net.minecraft.network.chat.Component;

/**
 * Centralized UI strings and message helpers for Piggy mods.
 * Add shared strings here to avoid duplication between modules.
 */
public final class I18n {

    private I18n() {}

    public static Component safetyTooltip() {
        return Component.literal("Configure safety and anti-cheat settings.");
    }

    public static Component antiCheatServerForcedMessage() {
        return Component.literal("Anti-Cheat Active: This server has forced anti-cheat ON.");
    }

    public static Component antiCheatDisableLocalMessage() {
        return Component.literal("Anti-Cheat Active: Disable 'No Cheating Mode' in settings to use.");
    }
}
