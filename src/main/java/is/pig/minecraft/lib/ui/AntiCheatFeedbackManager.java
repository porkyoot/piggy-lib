package is.pig.minecraft.lib.ui;

import is.pig.minecraft.api.*;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.MessagingAdapter;
import is.pig.minecraft.api.spi.MessagingAdapter.MessagePart;
import is.pig.minecraft.lib.features.CheatFeatureRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Platform-agnostic manager for centralized anti-cheat feedback.
 * ZERO net.minecraft imports.
 */
public class AntiCheatFeedbackManager {
    private static final AntiCheatFeedbackManager INSTANCE = new AntiCheatFeedbackManager();
    private final Set<String> messagesShown = new HashSet<>();

    private AntiCheatFeedbackManager() {}

    public static AntiCheatFeedbackManager getInstance() {
        return INSTANCE;
    }

    public void onFeatureBlocked(String featureId, BlockReason reason) {
        AntiCheatHudOverlay.triggerBlockedIcon();

        if (!messagesShown.contains(featureId)) {
            showExplanationMessage(featureId, reason);
            messagesShown.add(featureId);
        }
    }

    private void showExplanationMessage(String featureId, BlockReason reason) {
        Object client = PiggyServiceRegistry.getWorldStateAdapter().getClient();
        if (client == null) return;

        var feature = CheatFeatureRegistry.getFeature(featureId);
        String featureName = feature != null ? feature.displayName() : featureId;

        MessagingAdapter messenger = PiggyServiceRegistry.getMessagingAdapter();
        
        if (reason == BlockReason.SERVER_ENFORCEMENT) {
            messenger.sendFormattedMessage(client, false,
                MessagePart.bold("⚠ Feature Blocked: ", "RED"),
                MessagePart.of(featureName, "YELLOW"),
                MessagePart.of("\n", "WHITE"),
                MessagePart.of("This server has disabled this feature. ", "GRAY"),
                MessagePart.of("You cannot enable it while connected.", "GRAY")
            );
        } else {
            messenger.sendFormattedMessage(client, false,
                MessagePart.bold("⚠ Feature Blocked: ", "RED"),
                MessagePart.of(featureName, "YELLOW"),
                MessagePart.of("\n", "WHITE"),
                MessagePart.of("Your 'No Cheating Mode' is enabled. ", "GRAY"),
                MessagePart.of("Disable it in mod settings to use this feature.", "GRAY")
            );
        }
    }

    public void resetMessageTracking() {
        messagesShown.clear();
    }
}
