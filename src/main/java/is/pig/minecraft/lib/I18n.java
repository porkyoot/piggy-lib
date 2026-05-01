package is.pig.minecraft.lib;
import is.pig.minecraft.api.*;

import net.minecraft.network.chat.Component;

/**
 * Centralized UI strings and message helpers for Piggy mods.
 * Add shared strings here to avoid duplication between modules.
 */
public final class I18n {

    private static final I18n INSTANCE = new I18n();

    private I18n() {}

    /**
     * @return the singleton instance of I18n for use as an object reference.
     */
    public static I18n getInstance() {
        return INSTANCE;
    }

    /**
     * Translates a given key into a human-readable string using the current locale mapping.
     * This uses {@link net.minecraft.locale.Language} for underlying lookups.
     *
     * @param key the translation key
     * @param args the formatting arguments to satisfy placeholder requirements
     * @return the translated string, or the key itself if no mapping is found
     */
    public String translate(String key, Object... args) {
        net.minecraft.locale.Language language = net.minecraft.locale.Language.getInstance();
        String msg = (language != null) ? language.getOrDefault(key) : key;
        if (args.length > 0) {
            try {
                return String.format(msg, args);
            } catch (java.util.IllegalFormatException e) {
                return msg;
            }
        }
        return msg;
    }

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

