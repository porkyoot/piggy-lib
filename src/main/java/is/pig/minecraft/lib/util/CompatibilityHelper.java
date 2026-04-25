package is.pig.minecraft.lib.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Method;
import java.util.function.Consumer;

public class CompatibilityHelper {

    public static void blit(GuiGraphics graphics, net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight) {
        try {
            // Try 1.21.2: blit(Function, ResourceLocation, int, int, float, float, int, int, int, int)
            Method m = GuiGraphics.class.getMethod("blit", java.util.function.Function.class, net.minecraft.resources.ResourceLocation.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
            
            java.util.function.Function<net.minecraft.resources.ResourceLocation, net.minecraft.client.renderer.RenderType> guiTextured = (loc) -> {
                try {
                    Method mGui = net.minecraft.client.renderer.RenderType.class.getMethod("guiTextured", net.minecraft.resources.ResourceLocation.class);
                    return (net.minecraft.client.renderer.RenderType) mGui.invoke(null, loc);
                } catch (Exception e) {
                    return null;
                }
            };
            
            m.invoke(graphics, guiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
        } catch (NoSuchMethodException e) {
            try {
                // Try 1.21.1: blit(ResourceLocation, int, int, float, float, int, int, int, int)
                Method m = GuiGraphics.class.getMethod("blit", net.minecraft.resources.ResourceLocation.class, int.class, int.class, float.class, float.class, int.class, int.class, int.class, int.class);
                m.invoke(graphics, texture, x, y, u, v, width, height, textureWidth, textureHeight);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static net.minecraft.client.renderer.RenderType getTranslucentRenderType(net.minecraft.resources.ResourceLocation texture) {
        String[] possibleMethods = {
            "entityTranslucentCull",
            "itemEntityTranslucentCull", 
            "entityTranslucent",
            "entityTranslucentEmissive"
        };
        
        for (String methodName : possibleMethods) {
            try {
                Method m = net.minecraft.client.renderer.RenderType.class.getMethod(methodName, net.minecraft.resources.ResourceLocation.class);
                return (net.minecraft.client.renderer.RenderType) m.invoke(null, texture);
            } catch (Exception ignored) {}
        }
        
        for (Method m : net.minecraft.client.renderer.RenderType.class.getMethods()) {
            if (m.getParameterCount() == 1 && 
                m.getParameterTypes()[0] == net.minecraft.resources.ResourceLocation.class &&
                m.getReturnType() == net.minecraft.client.renderer.RenderType.class &&
                (m.getName().toLowerCase().contains("translucent") || m.getName().toLowerCase().contains("entity"))) {
                try {
                    return (net.minecraft.client.renderer.RenderType) m.invoke(null, texture);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    public static Object createMovePlayerPacketPos(double x, double y, double z, boolean onGround) {
        try {
            Class<?> posClass = Class.forName("net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$Pos");
            for (java.lang.reflect.Constructor<?> c : posClass.getConstructors()) {
                if (c.getParameterCount() == 4 && 
                    c.getParameterTypes()[0] == double.class && 
                    c.getParameterTypes()[1] == double.class && 
                    c.getParameterTypes()[2] == double.class && 
                    c.getParameterTypes()[3] == boolean.class) {
                    return c.newInstance(x, y, z, onGround);
                }
                if (c.getParameterCount() == 5 && 
                    c.getParameterTypes()[0] == double.class && 
                    c.getParameterTypes()[1] == double.class && 
                    c.getParameterTypes()[2] == double.class && 
                    c.getParameterTypes()[3] == boolean.class && 
                    c.getParameterTypes()[4] == boolean.class) {
                    return c.newInstance(x, y, z, onGround, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isSolidRender(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos) {
        try {
            Method m = state.getClass().getMethod("isSolidRender", net.minecraft.world.level.BlockGetter.class, net.minecraft.core.BlockPos.class);
            return (Boolean) m.invoke(state, level, pos);
        } catch (Exception e) {
            try {
                Method m = state.getClass().getMethod("isSolidRender");
                return (Boolean) m.invoke(state);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public static void sendSystemMessage(net.minecraft.world.entity.player.Player player, Component message) {
        try {
            Method m = player.getClass().getMethod("sendSystemMessage", Component.class);
            m.invoke(player, message);
        } catch (NoSuchMethodException e) {
            try {
                Method m = player.getClass().getMethod("displayClientMessage", Component.class, boolean.class);
                m.invoke(player, message, false);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendSystemMessage(Minecraft client, Component message) {
        if (client.player != null) {
            sendSystemMessage(client.player, message);
        }
    }

    public static boolean shouldSwing(InteractionResult result) {
        try {
            Method m = result.getClass().getMethod("shouldSwing");
            return (boolean) m.invoke(result);
        } catch (NoSuchMethodException e) {
            // In 1.21.2, check if SUCCESS or SUCCESS_SERVER
            String str = result.toString().toUpperCase();
            return str.contains("SUCCESS");
        } catch (Exception e) {
            return false;
        }
    }

    public static void drawCustom(GuiGraphics graphics, Consumer<Object> customDraw) {
        try {
            // Try 1.21.2: drawSpecial(Consumer)
            Method m = GuiGraphics.class.getMethod("drawSpecial", Consumer.class);
            m.invoke(graphics, customDraw);
        } catch (NoSuchMethodException e) {
            try {
                // Try 1.21.1: bufferSource()
                Method m = GuiGraphics.class.getMethod("bufferSource");
                Object bufferSource = m.invoke(graphics);
                customDraw.accept(bufferSource);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getMinBuildHeight(Level level) {
        try {
            // Try 1.21.2: getMinY()
            Method m = level.getClass().getMethod("getMinY");
            return (int) m.invoke(level);
        } catch (NoSuchMethodException e) {
            try {
                // Try 1.21.1: getMinBuildHeight()
                Method m = level.getClass().getMethod("getMinBuildHeight");
                return (int) m.invoke(level);
            } catch (Exception ex) {
                return -64;
            }
        } catch (Exception e) {
            return -64;
        }
    }

    public static int getPixelRGBA(NativeImage image, int x, int y) {
        try {
            // Try 1.21.2: getColorArgb(int, int)
            Method m = image.getClass().getMethod("getColorArgb", int.class, int.class);
            return (int) m.invoke(image, x, y);
        } catch (NoSuchMethodException e) {
            try {
                // Try 1.21.1: getPixelRGBA(int, int)
                Method m = image.getClass().getMethod("getPixelRGBA", int.class, int.class);
                return (int) m.invoke(image, x, y);
            } catch (Exception ex) {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public static void setColor(GuiGraphics graphics, float r, float g, float b, float a) {
        try {
            Method m = GuiGraphics.class.getMethod("setColor", float.class, float.class, float.class, float.class);
            m.setAccessible(true);
            m.invoke(graphics, r, g, b, a);
        } catch (NoSuchMethodException e) {
            // In 1.21.2 we use RenderSystem.setShaderColor
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(r, g, b, a);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
