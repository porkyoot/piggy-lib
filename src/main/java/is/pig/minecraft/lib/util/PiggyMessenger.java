package is.pig.minecraft.lib.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Standardized utility for sending chat and admin messages across all Piggy mods.
 */
public class PiggyMessenger {

    private static final String DEFAULT_PREFIX = "[PiggyMods]";
    private static final Style PREFIX_STYLE = Style.EMPTY.withColor(ChatFormatting.LIGHT_PURPLE).withBold(true);
    private static final Style ERROR_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
    private static final Style SUCCESS_STYLE = Style.EMPTY.withColor(ChatFormatting.GREEN);
    private static final Style INFO_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
    private static final Style WHISPER_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY).withItalic(true);

    public static MutableComponent getPrefix() {
        return Component.literal(DEFAULT_PREFIX + " ").withStyle(PREFIX_STYLE);
    }

    public static MutableComponent getAdminPrefix() {
        return getPrefix();
    }

    public static void sendSuccess(Player player, String translationKey, Object... args) {
        CompatibilityHelper.sendSystemMessage(player, getPrefix().append(Component.translatable(translationKey, args).withStyle(SUCCESS_STYLE)));
    }

    public static void sendError(Player player, String translationKey, Object... args) {
        CompatibilityHelper.sendSystemMessage(player, getPrefix().append(Component.translatable(translationKey, args).withStyle(ERROR_STYLE)));
    }

    public static void sendInfo(Player player, String translationKey, Object... args) {
        CompatibilityHelper.sendSystemMessage(player, getPrefix().append(Component.translatable(translationKey, args).withStyle(INFO_STYLE)));
    }

    /**
     * Dedicated method for the local client player to replace action bar/toast messages.
     * Always sends to chat (overlay = false) to ensure users can read it fully.
     */
    public static void sendClientMessage(LocalPlayer player, String translationKey, Object... args) {
        player.displayClientMessage(getPrefix().append(Component.translatable(translationKey, args).withStyle(INFO_STYLE)), false);
    }

    /**
     * Dedicated method for the local client player to send an error message to the chat.
     */
    public static void sendClientError(LocalPlayer player, String translationKey, Object... args) {
        player.displayClientMessage(getPrefix().append(Component.translatable(translationKey, args).withStyle(ERROR_STYLE)), false);
    }

    /**
     * Dedicated method for the local client player to send an error message to the chat
     * with an interactive suffix to open an associated log file.
     */
    public static void sendClientErrorWithLog(LocalPlayer player, String translationKey, String logPath, Object... args) {
        ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_FILE, logPath);
        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Open log file"));
        MutableComponent clickableText = Component.literal(" [Open Log]")
                .withStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW).withUnderlined(true)
                .withClickEvent(clickEvent).withHoverEvent(hoverEvent));
        
        player.displayClientMessage(getPrefix()
            .append(Component.translatable(translationKey, args).withStyle(ERROR_STYLE))
            .append(clickableText), false);
    }

    /**
     * Standardizes whispered message format for server administrators with an interactive tag.
     */
    public static void sendAdminMessage(ServerPlayer op, String playerName, String tag, Component content, ClickEvent clickEvent, HoverEvent hoverEvent) {
        MutableComponent message = Component.literal("")
                .append(getAdminPrefix())
                .append(Component.literal("<" + playerName + "> ").withStyle(WHISPER_STYLE))
                .append(Component.literal("[" + tag + "]")
                        .withStyle(WHISPER_STYLE)
                        .withStyle(style -> style
                                .withClickEvent(clickEvent)
                                .withHoverEvent(hoverEvent)
                        ))
                .append(Component.literal(" ").withStyle(WHISPER_STYLE))
                .append(content.copy().withStyle(WHISPER_STYLE));

        CompatibilityHelper.sendSystemMessage(op, message);
    }
}
