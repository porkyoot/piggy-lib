package is.pig.minecraft.lib.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.renderer.RenderType;

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
    
    private static final float SUBMENU_INNER_RADIUS = 38f;
    private static final float SUBMENU_OUTER_RADIUS = 60f;
    private static final float SUBMENU_ICON_DISTANCE = 49f;

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

        renderBackgroundGeometry(graphics, cx, cy, mouseX, mouseY);
        renderIcons(graphics, cx, cy, mouseX, mouseY);
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
            T parent = radialItems.get(index);
            
            List<? extends RadialMenuItem> subItems = parent.getSubMenuItems();
            boolean isSubmenuActive = false;
            
            // Check if the currently selected or hovered item belongs to this parent's hierarchy
            if (hoveredItem == parent || selectedItem == parent) {
                isSubmenuActive = true;
            } else {
                for (RadialMenuItem sub : subItems) {
                    if (hoveredItem == sub || selectedItem == sub) {
                        isSubmenuActive = true;
                        break;
                    }
                }
            }

            if (dist >= OUTER_RADIUS && !subItems.isEmpty() && isSubmenuActive) {
                double startAngle = index * anglePerItem;
                double midAngle = startAngle + (anglePerItem / 2);
                
                // We want the submenus to take up a maximum of 80 degrees total, centered on the parent's midAngle
                double maxSubAngleTotal = Math.min(anglePerItem, Math.toRadians(80));
                double subAnglePerItem = maxSubAngleTotal / subItems.size();
                double subStartAngle = midAngle - (maxSubAngleTotal / 2);
                
                // Normalize absolute angle to be relative to subStartAngle
                double relAngle = angle - subStartAngle;
                // Handle wrap-around gracefully for relative comparisons
                while (relAngle < -Math.PI) relAngle += 2 * Math.PI;
                while (relAngle > Math.PI) relAngle -= 2 * Math.PI;
                
                if (relAngle >= 0 && relAngle <= maxSubAngleTotal) {
                    int subIndex = (int) (relAngle / subAnglePerItem);
                    if (subIndex >= 0 && subIndex < subItems.size()) {
                        candidate = (T) subItems.get(subIndex);
                    } else {
                        candidate = parent;
                    }
                } else {
                    candidate = parent;
                }
            } else {
                candidate = parent;
            }
        }

        this.hoveredItem = candidate;

        if (isItemEnabled.test(candidate) && candidate != selectedItem) {
            selectedItem = candidate;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(selectedItem);
            }
        }
    }

    private void renderBackgroundGeometry(GuiGraphics graphics, int cx, int cy, int mx, int my) {
        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0, 0, 10);

        // Use GuiGraphics built-in batch buffer for transparency instead of
        // RenderSystem
        VertexConsumer buffer = graphics.bufferSource().getBuffer(RenderType.gui());
        Matrix4f mat = poseStack.last().pose();

        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
        if (angle < 0) angle += 2 * Math.PI;

        double anglePerItem = (2 * Math.PI) / radialItems.size();
        int hoveredIndex = (int) (angle / anglePerItem) % radialItems.size();

        for (int i = 0; i < radialItems.size(); i++) {
            T item = radialItems.get(i);
            
            double start = (i * anglePerItem) - Math.toRadians(90);
            double end = ((i + 1) * anglePerItem) - Math.toRadians(90);
            double gap = Math.toRadians(2);

            drawRegion(buffer, mat, cx, cy, INNER_RADIUS + 2, OUTER_RADIUS, start + gap, end - gap, item);
            
            List<? extends RadialMenuItem> subItems = item.getSubMenuItems();
            
            boolean isSubmenuActive = false;
            // The submenu is visible if the mouse is hovering over the parent, hovering over a subitem, 
            // or if a subitem is currently selected.
            if (hoveredItem == item || selectedItem == item) {
                isSubmenuActive = true;
            } else {
                for (RadialMenuItem sub : subItems) {
                    if (hoveredItem == sub || selectedItem == sub) {
                        isSubmenuActive = true;
                        break;
                    }
                }
            }
            
            if (isSubmenuActive && !subItems.isEmpty()) {
                double parentMidAngle = start + (anglePerItem / 2);
                double maxSubAngleTotal = Math.min(anglePerItem, Math.toRadians(80));
                double subAnglePerItem = maxSubAngleTotal / subItems.size();
                double subStartAngle = parentMidAngle - (maxSubAngleTotal / 2);
                
                for (int j = 0; j < subItems.size(); j++) {
                    T subItem = (T) subItems.get(j);
                    double subStart = subStartAngle + (j * subAnglePerItem);
                    double subEnd = subStartAngle + ((j + 1) * subAnglePerItem);
                    drawRegion(buffer, mat, cx, cy, SUBMENU_INNER_RADIUS, SUBMENU_OUTER_RADIUS, subStart + gap, subEnd - gap, subItem);
                }
            }
        }

        graphics.bufferSource().endBatch(RenderType.gui());
        poseStack.popPose();
    }

    private void drawRegion(VertexConsumer buffer, Matrix4f mat, int cx, int cy, float rIn, float rOut, double start, double end, T item) {
        boolean isHovered = (item == hoveredItem);
        boolean isSelected = (item == selectedItem);
        boolean enabled = isItemEnabled.test(item);

        float r = 1f, g = 1f, b = 1f, a = 0.3f;

        if (isHovered) {
            if (enabled) {
                float[] rgba = highlightColor.getComponents(null);
                r = rgba[0];
                g = rgba[1];
                b = rgba[2];
                a = 0.6f;
            } else {
                r = 0.8f;
                g = 0.2f;
                b = 0.2f;
                a = 0.4f;
            }
        } else if (isSelected) {
            float[] rgba = highlightColor.getComponents(null);
            r = rgba[0];
            g = rgba[1];
            b = rgba[2];
            a = 0.4f;
        }

        drawArc(buffer, mat, cx, cy, rIn, rOut, start, end, r, g, b, a);
    }

    private void renderIcons(GuiGraphics graphics, int cx, int cy, int mx, int my) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 20); // Render above background

        double dx = mx - cx;
        double dy = my - cy;
        double dist = Math.sqrt(dx * dx + dy * dy);
        double angle = Math.atan2(dy, dx) - Math.toRadians(-90);
        if (angle < 0) angle += 2 * Math.PI;

        double anglePerItem = (2 * Math.PI) / radialItems.size();
        int hoveredIndex = (int) (angle / anglePerItem) % radialItems.size();

        for (int i = 0; i < radialItems.size(); i++) {
            T item = radialItems.get(i);
            double start = i * anglePerItem;
            double midAngle = start + (anglePerItem / 2) - Math.toRadians(90);
            
            int x = (int) (cx + Math.cos(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);
            int y = (int) (cy + Math.sin(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);

            drawItemIcon(graphics, item, x, y, item == selectedItem);

            if (item == selectedItem && isItemEnabled.test(item) && extraInfoProvider != null) {
                Component info = extraInfoProvider.apply(item);
                if (info != null) {
                    drawExtraInfo(graphics, info, x, y, midAngle);
                }
            }
            
            List<? extends RadialMenuItem> subItems = item.getSubMenuItems();
            
            boolean isSubmenuActive = false;
            // Same logic as background geometry
            if (hoveredItem == item || selectedItem == item) {
                isSubmenuActive = true;
            } else {
                for (RadialMenuItem sub : subItems) {
                    if (hoveredItem == sub || selectedItem == sub) {
                        isSubmenuActive = true;
                        break;
                    }
                }
            }

            if (isSubmenuActive && !subItems.isEmpty()) {
                double parentMidAngleAbsolute = start + (anglePerItem / 2);
                double maxSubAngleTotal = Math.min(anglePerItem, Math.toRadians(80));
                double subAnglePerItem = maxSubAngleTotal / subItems.size();
                double subStartAngleAbsolute = parentMidAngleAbsolute - (maxSubAngleTotal / 2);

                for (int j = 0; j < subItems.size(); j++) {
                    T subItem = (T) subItems.get(j);
                    double subMidAngle = subStartAngleAbsolute + (j * subAnglePerItem) + (subAnglePerItem / 2) - Math.toRadians(90);
                    int sx = (int) (cx + Math.cos(subMidAngle) * SUBMENU_ICON_DISTANCE) - (ICON_SIZE / 2);
                    int sy = (int) (cy + Math.sin(subMidAngle) * SUBMENU_ICON_DISTANCE) - (ICON_SIZE / 2);
                    drawItemIcon(graphics, subItem, sx, sy, subItem == selectedItem);
                    if (subItem == selectedItem && isItemEnabled.test(subItem) && extraInfoProvider != null) {
                        Component info = extraInfoProvider.apply(subItem);
                        if (info != null) {
                            drawExtraInfo(graphics, info, sx, sy, subMidAngle);
                        }
                    }
                }
            }
        }

        drawItemIcon(graphics, centerItem, cx - (ICON_SIZE / 2), cy - (ICON_SIZE / 2), centerItem == selectedItem);
        graphics.pose().popPose();
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
        boolean enabled = isItemEnabled.test(item);

        if (!enabled) {
            graphics.setColor(0.5f, 0.5f, 0.5f, 0.8f);
        } else if (selected) {
            float[] rgba = highlightColor.getComponents(null);
            graphics.setColor(rgba[0], rgba[1], rgba[2], 1.0f);
        } else {
            graphics.setColor(1f, 1f, 1f, 1f);
        }

        graphics.blit(item.getIconLocation(selected), x, y, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
        graphics.setColor(1f, 1f, 1f, 1f);
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

            // Quad CCW winding
            buffer.addVertex(mat, x1In, y1In, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2In, y2In, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x2Out, y2Out, 0).setColor(r, g, b, a);
            buffer.addVertex(mat, x1Out, y1Out, 0).setColor(r, g, b, a);
        }
    }
}