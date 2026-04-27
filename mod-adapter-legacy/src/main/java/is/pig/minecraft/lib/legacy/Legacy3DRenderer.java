package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.I3DRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public class Legacy3DRenderer implements I3DRenderer {
    private static final java.lang.ThreadLocal<RenderContext> CURRENT_CONTEXT = new java.lang.ThreadLocal<>();

    public static void setCurrentContext(PoseStack poseStack, VertexConsumer buffer) {
        CURRENT_CONTEXT.set(new RenderContext(poseStack, buffer));
    }

    private RenderContext getContext() {
        RenderContext ctx = CURRENT_CONTEXT.get();
        if (ctx == null) {
            throw new IllegalStateException("No RenderContext set for Legacy3DRenderer");
        }
        return ctx;
    }

    @Override
    public void drawLine(double x1, double y1, double z1, double x2, double y2, double z2, int color) {
        VertexConsumer buffer = getContext().buffer();
        PoseStack poseStack = getContext().poseStack();
        
        float a = ((color >> 24) & 0xFF) / 255f;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        
        buffer.addVertex(poseStack.last().pose(), (float) x1, (float) y1, (float) z1).setColor(r, g, b, a).setNormal(poseStack.last(), 0, 1, 0);
        buffer.addVertex(poseStack.last().pose(), (float) x2, (float) y2, (float) z2).setColor(r, g, b, a).setNormal(poseStack.last(), 0, 1, 0);
    }

    @Override
    public void drawBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color) {
        // Draw 12 lines for a wireframe box
        drawLine(minX, minY, minZ, maxX, minY, minZ, color);
        drawLine(maxX, minY, minZ, maxX, minY, maxZ, color);
        drawLine(maxX, minY, maxZ, minX, minY, maxZ, color);
        drawLine(minX, minY, maxZ, minX, minY, minZ, color);
        
        drawLine(minX, maxY, minZ, maxX, maxY, minZ, color);
        drawLine(maxX, maxY, minZ, maxX, maxY, maxZ, color);
        drawLine(maxX, maxY, maxZ, minX, maxY, maxZ, color);
        drawLine(minX, maxY, maxZ, minX, maxY, minZ, color);
        
        drawLine(minX, minY, minZ, minX, maxY, minZ, color);
        drawLine(maxX, minY, minZ, maxX, maxY, minZ, color);
        drawLine(maxX, minY, maxZ, maxX, maxY, maxZ, color);
        drawLine(minX, minY, maxZ, minX, maxY, maxZ, color);
    }

    @Override
    public void highlightBlock(int x, int y, int z, int color) {
        drawBox(x, y, z, x + 1, y + 1, z + 1, color);
    }

    private record RenderContext(PoseStack poseStack, VertexConsumer buffer) {}
}
