package is.pig.minecraft.lib.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.Color;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class GenericRadialMenuScreen<T extends RadialMenuItem> extends Screen {

    private static final int ICON_SIZE = 16;
    private static final float INNER_RADIUS = 12f;
    private static final float OUTER_RADIUS = 34f;
    private static final float ICON_DISTANCE = 24f;

    private final T centerItem;
    private final List<T> radialItems;
    private final Consumer<T> onSelectionChanged; 
    private final Function<T, Component> extraInfoProvider;
    private final Runnable onCloseCallback;
    private final InputConstants.Key triggerKey;
    private final Predicate<Double> onScrollCallback;
    
    private final Predicate<T> isItemEnabled;
    private final Consumer<T> onBlockedAction;

    private T selectedItem;
    private T hoveredItem;

    private final Color highlightColor;

    public GenericRadialMenuScreen(Component title,
            T centerItem,
            List<T> radialItems,
            T currentSelection,
            InputConstants.Key triggerKey,
            Consumer<T> onSelectionChanged,
            Runnable onCloseCallback,
            Function<T, Component> extraInfoProvider,
            Predicate<Double> onScrollCallback,
            Predicate<T> isItemEnabled,
            Consumer<T> onBlockedAction) {
        super(title);
        this.centerItem = centerItem;
        this.radialItems = radialItems;
        this.selectedItem = currentSelection;
        this.hoveredItem = currentSelection;
        this.triggerKey = triggerKey;
        this.onSelectionChanged = onSelectionChanged;
        this.onCloseCallback = onCloseCallback;
        this.extraInfoProvider = extraInfoProvider;
        this.onScrollCallback = onScrollCallback;
        this.isItemEnabled = isItemEnabled != null ? isItemEnabled : (t) -> true;
        this.onBlockedAction = onBlockedAction;
        
        this.highlightColor = new Color(0, 255, 230, 100); 
    }
    
    public GenericRadialMenuScreen(Component title, T center, List<T> radials, T current, 
            InputConstants.Key key, Consumer<T> onChange, Runnable onClose, 
            Function<T, Component> info, Predicate<Double> scroll) {
        this(title, center, radials, current, key, onChange, onClose, info, scroll, null, null);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (selectedItem == centerItem) {
            return false;
        }
        if (onScrollCallback != null && scrollY != 0) {
            if (onScrollCallback.test(scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!isTriggerKeyPressed()) {
            if (hoveredItem != null && !isItemEnabled.test(hoveredItem)) {
                if (onBlockedAction != null) {
                    onBlockedAction.accept(hoveredItem);
                }
            } 
            this.onClose();
        }
    }

    private boolean isTriggerKeyPressed() {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (triggerKey.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(window, triggerKey.getValue());
        } else if (triggerKey.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, triggerKey.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (onCloseCallback != null)
            onCloseCallback.run();
        super.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int cx = width / 2;
        int cy = height / 2;
        
        updateHover(mouseX, mouseY, cx, cy);
        
        renderBackgroundGeometry(graphics, cx, cy);
        renderIcons(graphics, cx, cy);
    }

    private void updateHover(int mx, int my, int cx, int cy) {
        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);

        T candidate;

        if (dist < INNER_RADIUS) {
            candidate = centerItem;
        } else {
            double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
            if (angle < 0)
                angle += 2 * Math.PI;

            double anglePerItem = (2 * Math.PI) / radialItems.size();
            int index = (int) (angle / anglePerItem) % radialItems.size();
            candidate = radialItems.get(index);
        }
        
        this.hoveredItem = candidate;

        if (isItemEnabled.test(candidate) && candidate != selectedItem) {
            selectedItem = candidate;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(selectedItem);
            }
        }
    }

    private void renderBackgroundGeometry(GuiGraphics graphics, int cx, int cy) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 10);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        Matrix4f mat = poseStack.last().pose();

        double anglePerItem = (2 * Math.PI) / radialItems.size();
        for (int i = 0; i < radialItems.size(); i++) {
            T item = radialItems.get(i);
            boolean isHovered = (item == hoveredItem);
            boolean isSelected = (item == selectedItem);
            boolean enabled = isItemEnabled.test(item);

            float r = 1f, g = 1f, b = 1f, a = 0.3f;
            
            if (isHovered) {
                if (enabled) {
                    float[] rgba = highlightColor.getComponents(null);
                    r = rgba[0]; g = rgba[1]; b = rgba[2]; a = 0.6f;
                } else {
                    r = 0.8f; g = 0.2f; b = 0.2f; a = 0.4f; 
                }
            } else if (isSelected) {
                float[] rgba = highlightColor.getComponents(null);
                r = rgba[0]; g = rgba[1]; b = rgba[2]; a = 0.4f;
            }

            double start = (i * anglePerItem) - Math.toRadians(90);
            double end = ((i + 1) * anglePerItem) - Math.toRadians(90);
            double gap = Math.toRadians(2);

            drawArc(buffer, mat, cx, cy, INNER_RADIUS + 2, OUTER_RADIUS, start + gap, end - gap, r, g, b, a);
        }

        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        poseStack.popPose();
    }

    private void renderIcons(GuiGraphics graphics, int cx, int cy) {
        double anglePerItem = (2 * Math.PI) / radialItems.size();
        for (int i = 0; i < radialItems.size(); i++) {
            T item = radialItems.get(i);
            double midAngle = (i * anglePerItem + (anglePerItem / 2)) - Math.toRadians(90);
            int x = (int) (cx + Math.cos(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);
            int y = (int) (cy + Math.sin(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);

            drawItemIcon(graphics, item, x, y, item == selectedItem);
            
            if (item == selectedItem && isItemEnabled.test(item) && extraInfoProvider != null) {
                Component info = extraInfoProvider.apply(item);
                if (info != null) {
                    drawExtraInfo(graphics, info, x, y, midAngle);
                }
            }
        }

        drawItemIcon(graphics, centerItem, cx - (ICON_SIZE / 2), cy - (ICON_SIZE / 2), centerItem == selectedItem);
    }

    private void drawExtraInfo(GuiGraphics graphics, Component text, int iconX, int iconY, double angleRad) {
        float textDistance = ICON_SIZE * 0.8f;
        int textX = iconX + (ICON_SIZE / 2) + (int) (Math.cos(angleRad) * textDistance);
        int textY = iconY + (ICON_SIZE / 2) + (int) (Math.sin(angleRad) * textDistance);

        int textWidth = this.font.width(text);
        textX -= textWidth / 2;
        textY -= this.font.lineHeight / 2;

        graphics.drawString(this.font, text, textX, textY, 0xFFFFFF, true);
    }

    private void drawItemIcon(GuiGraphics graphics, T item, int x, int y, boolean selected) {
        RenderSystem.enableBlend();
        boolean enabled = isItemEnabled.test(item);

        if (!enabled) {
             RenderSystem.setShaderColor(0.5f, 0.5f, 0.5f, 0.8f);
        } else if (selected) {
            float[] rgba = highlightColor.getComponents(null);
            RenderSystem.setShaderColor(rgba[0], rgba[1], rgba[2], 1.0f);
        } else {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }

        graphics.blit(item.getIconLocation(selected), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private void drawArc(VertexConsumer buffer, Matrix4f mat, float cx, float cy, float rIn, float rOut, double start,
            double end, float r, float g, float b, float a) {
        int segments = 32;
        double step = (end - start) / segments;
        for (int i = 0; i < segments; i++) {
            double a1 = start + (i * step);
            double a2 = start + ((i + 1) * step);

            float x1In = (float) (cx + Math.cos(a1) * rIn);
            float y1In = (float) (cy + Math.sin(a1) * rIn);
            float x1Out = (float) (cx + Math.cos(a1) * rOut);
            float y1Out = (float) (cy + Math.sin(a1) * rOut);
            float x2In = (float) (cx + Math.cos(a2) * rIn);
            float y2In = (float) (cy + Math.sin(a2) * rIn);
            float x2Out = (float) (cx + Math.cos(a2) * rOut);
            float y2Out = (float) (cy + Math.sin(a2) * rOut);

            buffer.addVertex(mat, x1In, y1In, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x1Out, y1Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2In, y2In, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x1Out, y1Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2Out, y2Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2In, y2In, 0).setColor(r, g, b, a);
        }
    }
}