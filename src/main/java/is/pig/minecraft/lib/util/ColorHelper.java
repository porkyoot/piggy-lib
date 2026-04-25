package is.pig.minecraft.lib.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.platform.NativeImage;

import java.lang.reflect.Field;

/**
 * Utility class for color analysis of items.
 * Uses client-side classes.
 */
@Environment(EnvType.CLIENT)
public class ColorHelper {

    private static Field originalImageField;

    static {
        try {
            // Mapping for originalImage might be obfuscated in prod,
            // but in dev environment (Mojang mappings) it is originalImage.
            // We'll try "originalImage" first.
            originalImageField = SpriteContents.class.getDeclaredField("originalImage");
            originalImageField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // Try fallback logic or log
            // In a real mod, we might search by type
            for (Field f : SpriteContents.class.getDeclaredFields()) {
                if (f.getType() == NativeImage.class) {
                    originalImageField = f;
                    originalImageField.setAccessible(true);
                    break;
                }
            }
        }
    }

    public static int getDominantColor(ItemStack stack) {
        if (stack.isEmpty())
            return 0;
        try {
            BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(stack, null, null, 0);
            if (model == null)
                return 0;

            TextureAtlasSprite sprite = model.getParticleIcon();
            if (sprite == null)
                return 0;

            return calculateAverageColor(sprite);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int calculateAverageColor(TextureAtlasSprite sprite) {
        if (originalImageField == null)
            return 0xFFFFFF;

        try {
            @SuppressWarnings("resource")
            NativeImage image = (NativeImage) originalImageField.get(sprite.contents());
            if (image == null)
                return 0xFFFFFF;

            int width = image.getWidth(); // NativeImage methods
            int height = image.getHeight();
            long r = 0, g = 0, b = 0;
            int count = 0;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int color = CompatibilityHelper.getPixelRGBA(image, x, y);

                    int alpha = (color >> 24) & 0xFF;
                    if (alpha < 10)
                        continue;

                    r += color & 0xFF;
                    g += (color >> 8) & 0xFF;
                    b += (color >> 16) & 0xFF;
                    count++;
                }
            }
            if (count == 0)
                return 0xFFFFFF;
            
            int avgR = (int) (r / count);
            int avgG = (int) (g / count);
            int avgB = (int) (b / count);
            return (255 << 24) | ((avgR & 0xFF) << 16) | ((avgG & 0xFF) << 8) | (avgB & 0xFF);
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    public static float[] colorToHSB(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return java.awt.Color.RGBtoHSB(r, g, b, null);
    }
}
