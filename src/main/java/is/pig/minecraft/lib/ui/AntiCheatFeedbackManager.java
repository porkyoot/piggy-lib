package is.pig.minecraft.lib.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Singleton manager for centralized anti-cheat feedback.
 * Tracks blocked actions and coordinates UI feedback (icon + chat messages).
 */
public class AntiCheatFeedbackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("piggy-lib");
    private static final AntiCheatFeedbackManager INSTANCE = new AntiCheatFeedbackManager();

    // Icon display duration in milliseconds
    private static final long ICON_DISPLAY_DURATION = 2500;

    // Track last blocked action for icon display
    private long lastBlockedTime = 0;
    @SuppressWarnings("unused")
    private String lastBlockedFeatureId = null;
    @SuppressWarnings("unused")
    private BlockReason lastBlockedReason = null;

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
        long currentTime = System.currentTimeMillis();

        // Update icon display state
        lastBlockedTime = currentTime;
        lastBlockedFeatureId = featureId;
        lastBlockedReason = reason;

        // Show chat message if this is the first time for this feature
        if (!messagesShown.contains(featureId)) {
            showExplanationMessage(featureId, reason);
            messagesShown.add(featureId);
        }

        LOGGER.debug("Feature '{}' blocked: {}", featureId, reason);
    }

    /**
     * Gets the current icon visibility (0.0 = invisible, 1.0 = fully visible).
     * Uses fade in/out animation.
     */
    public float getIconAlpha() {
        if (lastBlockedTime == 0) {
            return 0.0f;
        }

        long elapsed = System.currentTimeMillis() - lastBlockedTime;

        if (elapsed > ICON_DISPLAY_DURATION) {
            return 0.0f;
        }

        // Fade in for first 200ms, stay solid, fade out for last 300ms
        long fadeInDuration = 200;
        long fadeOutStart = ICON_DISPLAY_DURATION - 300;

        if (elapsed < fadeInDuration) {
            // Fade in
            return (float) elapsed / fadeInDuration;
        } else if (elapsed > fadeOutStart) {
            // Fade out
            long fadeOutElapsed = elapsed - fadeOutStart;
            return 1.0f - ((float) fadeOutElapsed / 300);
        } else {
            // Fully visible
            return 1.0f;
        }
    }

    /**
     * Checks if the icon should be visible right now.
     */
    public boolean shouldShowIcon() {
        return getIconAlpha() > 0.0f;
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
        LOGGER.debug("Reset anti-cheat message tracking");
    }
}
