package is.pig.minecraft.lib.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * HUD overlay that renders the blocked action icon below the crosshair.
 */
public class AntiCheatHudOverlay {
    private static final ResourceLocation BLOCKED_ICON = ResourceLocation.fromNamespaceAndPath("piggy-lib",
            "textures/gui/blocked_icon.png");
    private static final int ICON_SIZE = 16; // Render size (will scale down from 32x32)
    private static boolean registered = false;

    /**
     * Registers the HUD overlay. Should be called during client initialization.
     */
    public static void register() {
        if (registered) {
            return;
        }

        HudRenderCallback.EVENT.register(AntiCheatHudOverlay::render);
        registered = true;
    }

    /**
     * Renders the blocked icon if it should be visible.
     */
    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        AntiCheatFeedbackManager manager = AntiCheatFeedbackManager.getInstance();

        if (!manager.shouldShowIcon()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.options.hideGui) {
            return;
        }

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Position: centered horizontally, below crosshair
        // Crosshair is at (screenWidth/2, screenHeight/2)
        // Place icon 20 pixels below crosshair center
        int x = (screenWidth / 2) - (ICON_SIZE / 2);
        int y = (screenHeight / 2) + 20;

        // Get alpha for fade animation
        float alpha = manager.getIconAlpha();

        // Set render color with alpha
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

        // Render the icon
        graphics.blit(BLOCKED_ICON, x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);

        // Reset shader color
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
