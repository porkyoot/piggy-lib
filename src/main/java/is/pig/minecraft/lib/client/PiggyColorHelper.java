package is.pig.minecraft.lib.client;
import is.pig.minecraft.api.*;

import is.pig.minecraft.api.ItemStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.FastColor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.platform.NativeImage;

import java.lang.reflect.Field;

public class PiggyColorHelper {
    private static Field originalImageField;

    static {
        try {
            originalImageField = SpriteContents.class.getDeclaredField("originalImage");
            originalImageField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            for (Field f : SpriteContents.class.getDeclaredFields()) {
                if (f.getType() == NativeImage.class) {
                    originalImageField = f;
                    originalImageField.setAccessible(true);
                    break;
                }
            }
        }
    }

    public static int getDominantColor(Object stackObj) {
        if (stackObj == null)
            return 0;
        try {
            net.minecraft.world.item.ItemStack mcStack = null;
            if (stackObj instanceof net.minecraft.world.item.ItemStack) {
                mcStack = (net.minecraft.world.item.ItemStack) stackObj;
            } else if (stackObj instanceof is.pig.minecraft.api.ItemStack apiStack) {
                if (apiStack.isEmpty()) return 0;
                mcStack = new net.minecraft.world.item.ItemStack(
                    BuiltInRegistries.ITEM.get(ResourceLocation.parse(apiStack.itemId())), 
                    apiStack.count()
                );
            }
            if (mcStack == null || mcStack.isEmpty())
                return 0;

            BakedModel model = Minecraft.getInstance().getItemRenderer().getModel(mcStack, null, null, 0);
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

            int width = image.getWidth();
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
            if (count == 0)
                return 0xFFFFFF;
            return FastColor.ARGB32.color(255, (int) (r / count), (int) (g / count), (int) (b / count));
        } catch (Exception e) {
            return 0xFFFFFF;
        }
    }
}
