package is.pig.minecraft.lib.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton manager for centralized anti-cheat feedback.
 * Tracks blocked actions and coordinates UI feedback (icon + chat messages).
 */
public class AntiCheatFeedbackManager {
    private static final AntiCheatFeedbackManager INSTANCE = new AntiCheatFeedbackManager();

    // Track which features have shown explanation messages this session
    private final Set<String> messagesShown = new HashSet<>();

    private AntiCheatFeedbackManager() {
    }

    public static AntiCheatFeedbackManager getInstance() {
        return INSTANCE;
    }

    /**
     * Called when a feature is blocked by anti-cheat enforcement.
     * Triggers icon display and chat message (first time only per feature).
     * 
     * @param featureId The ID of the blocked feature
     * @param reason    Why the feature was blocked
     */
    public void onFeatureBlocked(String featureId, BlockReason reason) {
        // Trigger icon display in the centralized queue
        AntiCheatHudOverlay.triggerBlockedIcon();

        // Show chat message if this is the first time for this feature
        if (!messagesShown.contains(featureId)) {
            showExplanationMessage(featureId, reason);
            messagesShown.add(featureId);
        }
    }

    /**
     * Shows an explanation message in chat about why the feature is blocked.
     */
    private void showExplanationMessage(String featureId, BlockReason reason) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }

        // Get feature display name
        var feature = is.pig.minecraft.lib.features.CheatFeatureRegistry.getFeature(featureId);
        String featureName = feature != null ? feature.displayName() : featureId;

        Component message;
        if (reason == BlockReason.SERVER_ENFORCEMENT) {
            message = Component.literal("⚠ Feature Blocked: ")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(Component.literal(featureName)
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("\n")
                            .withStyle(ChatFormatting.RESET))
                    .append(Component.literal("This server has disabled this feature. ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("You cannot enable it while connected.")
                            .withStyle(ChatFormatting.GRAY));
        } else {
            message = Component.literal("⚠ Feature Blocked: ")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
                    .append(Component.literal(featureName)
                            .withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal("\n")
                            .withStyle(ChatFormatting.RESET))
                    .append(Component.literal("Your 'No Cheating Mode' is enabled. ")
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("Disable it in mod settings to use this feature.")
                            .withStyle(ChatFormatting.GRAY));
        }

        client.player.sendSystemMessage(message);
    }

    /**
     * Resets the message tracking (e.g., on disconnect/reconnect).
     */
    public void resetMessageTracking() {
        messagesShown.clear();
    }
}
