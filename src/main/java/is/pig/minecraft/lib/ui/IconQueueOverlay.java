package is.pig.minecraft.lib.ui;

import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.RenderPipelineAdapter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Platform-agnostic overlay for displaying queued icons with transitions and inverted blend modes.
 * ZERO net.minecraft imports.
 */
public class IconQueueOverlay {

    private static final int ICON_SIZE = 8;
    private static final int VERTICAL_SPACING = 10;
    private static final int FADE_IN_TIME_MS = 300;
    private static final int FADE_OUT_TIME_MS = 500;

    private static class QueuedIcon {
        String iconId;
        long startTime;
        int durationMs;
        boolean flashing;

        public QueuedIcon(String iconId, int durationMs, boolean flashing) {
            this.iconId = iconId;
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
                float sine = (float) Math.sin((elapsed / 150.0) * Math.PI);
                return 0.6f + (0.4f * sine);
            }
            return 1.0f;
        }
    }

    private static final List<QueuedIcon> activeIcons = new LinkedList<>();

    /**
     * Add an icon to the queue, or refresh it if already in queue to prevent duplicates.
     */
    public static void queueIcon(String iconId, int durationMs, boolean flashing) {
        synchronized (activeIcons) {
            for (QueuedIcon qi : activeIcons) {
                if (qi.iconId.equals(iconId)) {
                    long targetExpiration = System.currentTimeMillis() + durationMs;
                    long currentExpiration = qi.startTime + qi.durationMs;
                    if (targetExpiration > currentExpiration) {
                        qi.durationMs = (int)(targetExpiration - qi.startTime);
                    }
                    qi.flashing = flashing;
                    return;
                }
            }
            activeIcons.add(new QueuedIcon(iconId, durationMs, flashing));
        }
    }

    /**
     * Agnostic render call. Should be invoked by the Core module's HUD renderer.
     */
    public static void render(Object context) {
        RenderPipelineAdapter pipeline = PiggyServiceRegistry.getRenderPipelineAdapters().getFirst();
        
        int screenWidth = pipeline.getScreenWidth(context);
        int screenHeight = pipeline.getScreenHeight(context);

        int baseX = (screenWidth / 2) - (ICON_SIZE / 2);
        int baseY = (screenHeight / 2) + 10;

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

                // Delegating specialized inverted rendering to the SPI
                pipeline.drawInvertedTexture(context, qi.iconId, baseX, iconY, ICON_SIZE, ICON_SIZE, alpha);
                index++;
            }
        }
    }
}
