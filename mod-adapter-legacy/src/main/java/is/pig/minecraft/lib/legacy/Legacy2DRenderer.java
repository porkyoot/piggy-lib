package is.pig.minecraft.lib.legacy;

import is.pig.minecraft.lib.api.I2DRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;

public class Legacy2DRenderer implements I2DRenderer {
    private static final java.lang.ThreadLocal<GuiGraphics> CURRENT_GRAPHICS = new java.lang.ThreadLocal<>();

    public static void setCurrentGraphics(GuiGraphics graphics) {
        CURRENT_GRAPHICS.set(graphics);
    }

    private GuiGraphics getGraphics() {
        GuiGraphics graphics = CURRENT_GRAPHICS.get();
        if (graphics == null) {
            throw new IllegalStateException("No GuiGraphics set for Legacy2DRenderer");
        }
        return graphics;
    }

    @Override
    public void drawText(String text, int x, int y, int color) {
        getGraphics().drawString(Minecraft.getInstance().font, text, x, y, color, false);
    }

    @Override
    public void drawRect(int x, int y, int width, int height, int color) {
        getGraphics().fill(x, y, x + width, y + height, color);
    }

    @Override
    public void drawTexture(String resourcePath, int x, int y, int width, int height) {
        ResourceLocation loc = ResourceLocation.parse(resourcePath);
        getGraphics().blit(RenderType::guiTextured, loc, x, y, 0f, 0f, width, height, width, height);
    }
}
