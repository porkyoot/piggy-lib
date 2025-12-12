package is.pig.minecraft.lib.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.FastColor;
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
            NativeImage image = (NativeImage) originalImageField.get(sprite.contents());
            if (image == null)
                return 0xFFFFFF;

            int width = image.getWidth(); // NativeImage methods
            int height = image.getHeight();
            long r = 0, g = 0, b = 0;
            int count = 0;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int color = image.getPixelRGBA(x, y);

                    int alpha = FastColor.ABGR32.alpha(color);
                    if (alpha < 10)
                        continue;

                    r += FastColor.ABGR32.red(color);
                    g += FastColor.ABGR32.green(color);
                    b += FastColor.ABGR32.blue(color);
                    count++;
                }
            }
            image.close();
            if (count == 0)
                return 0xFFFFFF;
            return FastColor.ARGB32.color(255, (int) (r / count), (int) (g / count), (int) (b / count));
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }

    public static float[] colorToHSB(int color) {
        int r = FastColor.ARGB32.red(color);
        int g = FastColor.ARGB32.green(color);
        int b = FastColor.ARGB32.blue(color);
        return java.awt.Color.RGBtoHSB(r, g, b, null);
    }
}
