package is.pig.minecraft.lib.ui;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class IconQueueOverlay {

    // Icon render size
    private static final int ICON_SIZE = 8;
    private static final int VERTICAL_SPACING = 10;
    private static final int FADE_IN_TIME_MS = 300;
    private static final int FADE_OUT_TIME_MS = 500;

    private static class QueuedIcon {
        ResourceLocation icon;
        long startTime;
        int durationMs;
        boolean flashing;

        public QueuedIcon(ResourceLocation icon, int durationMs, boolean flashing) {
            this.icon = icon;
            this.startTime = System.currentTimeMillis();
            this.durationMs = durationMs;
            this.flashing = flashing;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime >= durationMs;
        }

        public float getAlpha() {
            long elapsed = System.currentTimeMillis() - startTime;
            if (elapsed < FADE_IN_TIME_MS) {
                return (float) elapsed / FADE_IN_TIME_MS;
            }
            long remaining = durationMs - elapsed;
            if (remaining < FADE_OUT_TIME_MS) {
                return Math.max(0f, (float) remaining / FADE_OUT_TIME_MS);
            }
            if (flashing) {
                // pulsing effect completely solid to semi-transparent
                float sine = (float) Math.sin((elapsed / 150.0) * Math.PI);
                return 0.6f + (0.4f * sine);
            }
            return 1.0f;
        }
    }

    private static final List<QueuedIcon> activeIcons = new LinkedList<>();

    public static void register() {
        HudRenderCallback.EVENT.register(IconQueueOverlay::render);
    }

    /**
     * Add an icon to the queue, or refresh it if already in queue to prevent duplicates.
     */
    public static void queueIcon(ResourceLocation icon, int durationMs, boolean flashing) {
        synchronized (activeIcons) {
            for (QueuedIcon qi : activeIcons) {
                if (qi.icon.equals(icon)) {
                    // Update instead of adding duplicate
                    // Extend expiration so it doesn't flicker by resetting fade-in
                    long targetExpiration = System.currentTimeMillis() + durationMs;
                    long currentExpiration = qi.startTime + qi.durationMs;
                    if (targetExpiration > currentExpiration) {
                        qi.durationMs = (int)(targetExpiration - qi.startTime);
                    }
                    qi.flashing = flashing;
                    return;
                }
            }
            activeIcons.add(new QueuedIcon(icon, durationMs, flashing));
        }
    }

    public static void render(GuiGraphics graphics, DeltaTracker tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        int screenWidth = graphics.guiWidth();
        int screenHeight = graphics.guiHeight();

        // Base coordinates: Center horizontal, below crosshair
        int baseX = (screenWidth / 2) - (ICON_SIZE / 2);
        int baseY = (screenHeight / 2) + 10;

        // Preallocate RenderType map or generate on the fly
        synchronized (activeIcons) {
            Iterator<QueuedIcon> it = activeIcons.iterator();
            int index = 0;
            while (it.hasNext()) {
                QueuedIcon qi = it.next();
                if (qi.isExpired()) {
                    it.remove();
                    continue;
                }

                int iconY = baseY + (index * VERTICAL_SPACING);
                float alpha = qi.getAlpha();

                // Build custom render type for inverted colors
                RenderType invertedType = RenderType.create(
                        "piggy_gui_inverted_" + qi.icon.getPath().replace("/", "_"),
                        DefaultVertexFormat.POSITION_TEX_COLOR,
                        VertexFormat.Mode.QUADS,
                        1536,
                        false,
                        false,
                        RenderType.CompositeState.builder()
                                .setShaderState(new RenderStateShard.ShaderStateShard(GameRenderer::getPositionTexColorShader))
                                .setTextureState(new RenderStateShard.TextureStateShard(qi.icon, false, false))
                                .setTransparencyState(new RenderStateShard.TransparencyStateShard("gui_inverted_transparency", () -> {
                                    RenderSystem.enableBlend();
                                    RenderSystem.blendFuncSeparate(
                                            GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                                            GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                                            GlStateManager.SourceFactor.ONE,
                                            GlStateManager.DestFactor.ZERO);
                                }, () -> {
                                    RenderSystem.disableBlend();
                                    RenderSystem.defaultBlendFunc();
                                }))
                                .createCompositeState(false)
                );

                VertexConsumer buffer = graphics.bufferSource().getBuffer(invertedType);
                Matrix4f matrix = graphics.pose().last().pose();

                float x1 = (float) baseX;
                float y1 = (float) iconY;
                float x2 = (float) (baseX + ICON_SIZE);
                float y2 = (float) (iconY + ICON_SIZE);

                // Add vertices (scaling RGB by alpha enables fading with inverted blend mode)
                buffer.addVertex(matrix, x1, y1, 0).setUv(0, 0).setColor(alpha, alpha, alpha, alpha);
                buffer.addVertex(matrix, x1, y2, 0).setUv(0, 1).setColor(alpha, alpha, alpha, alpha);
                buffer.addVertex(matrix, x2, y2, 0).setUv(1, 1).setColor(alpha, alpha, alpha, alpha);
                buffer.addVertex(matrix, x2, y1, 0).setUv(1, 0).setColor(alpha, alpha, alpha, alpha);

                graphics.flush();
                index++;
            }
        }
    }
}
