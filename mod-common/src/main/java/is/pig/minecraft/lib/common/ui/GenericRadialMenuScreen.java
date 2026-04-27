package is.pig.minecraft.lib.common.ui;

import is.pig.minecraft.lib.api.I2DRenderer;
import java.util.List;
import java.util.function.Consumer;

public class GenericRadialMenuScreen<T extends RadialMenuItem> {
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
    
    private T selectedItem;
    private T hoveredItem;

    public GenericRadialMenuScreen(T centerItem, List<T> radialItems, T currentSelection, Consumer<T> onSelectionChanged) {
        this.centerItem = centerItem;
        this.radialItems = radialItems;
        this.selectedItem = currentSelection;
        this.hoveredItem = currentSelection;
        this.onSelectionChanged = onSelectionChanged;
    }

    public void updateHover(int mx, int my, int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;
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
                
                double maxSubAngleTotal = Math.min(anglePerItem, Math.toRadians(80));
                double subAnglePerItem = maxSubAngleTotal / subItems.size();
                double subStartAngle = midAngle - (maxSubAngleTotal / 2);
                
                double relAngle = angle - subStartAngle;
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

        if (candidate != selectedItem) {
            selectedItem = candidate;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(selectedItem);
            }
        }
    }

    public void render(I2DRenderer renderer, int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        updateHover(mouseX, mouseY, screenWidth, screenHeight);

        double anglePerItem = (2 * Math.PI) / radialItems.size();

        for (int i = 0; i < radialItems.size(); i++) {
            T item = radialItems.get(i);
            double start = i * anglePerItem;
            double midAngle = start + (anglePerItem / 2) - Math.toRadians(90);
            
            int x = (int) (cx + Math.cos(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);
            int y = (int) (cy + Math.sin(midAngle) * ICON_DISTANCE) - (ICON_SIZE / 2);

            boolean isSelected = (item == selectedItem);
            renderer.drawTexture(item.getIconPath(isSelected), x, y, ICON_SIZE, ICON_SIZE);

            if (isSelected) {
                renderer.drawText(item.getDisplayName(), x, y - 10, 0xFFFFFF);
            }
            
            List<? extends RadialMenuItem> subItems = item.getSubMenuItems();
            boolean isSubmenuActive = (hoveredItem == item || selectedItem == item);
            if (!isSubmenuActive) {
                for (RadialMenuItem sub : subItems) {
                    if (hoveredItem == sub || selectedItem == sub) {
                        isSubmenuActive = true;
                        break;
                    }
                }
            }

            if (isSubmenuActive && !subItems.isEmpty()) {
                double maxSubAngleTotal = Math.min(anglePerItem, Math.toRadians(80));
                double subAnglePerItem = maxSubAngleTotal / subItems.size();
                double subStartAngle = midAngle - (maxSubAngleTotal / 2);
                
                for (int j = 0; j < subItems.size(); j++) {
                    T subItem = (T) subItems.get(j);
                    double subMidAngle = subStartAngle + (j * subAnglePerItem) + (subAnglePerItem / 2);
                    int sx = (int) (cx + Math.cos(subMidAngle) * SUBMENU_ICON_DISTANCE) - (ICON_SIZE / 2);
                    int sy = (int) (cy + Math.sin(subMidAngle) * SUBMENU_ICON_DISTANCE) - (ICON_SIZE / 2);
                    
                    renderer.drawTexture(subItem.getIconPath(subItem == selectedItem), sx, sy, ICON_SIZE, ICON_SIZE);
                    if (subItem == selectedItem) {
                        renderer.drawText(subItem.getDisplayName(), sx, sy - 10, 0xFFFFFF);
                    }
                }
            }
        }

        renderer.drawTexture(centerItem.getIconPath(centerItem == selectedItem), cx - (ICON_SIZE / 2), cy - (ICON_SIZE / 2), ICON_SIZE, ICON_SIZE);
        if (centerItem == selectedItem) {
            renderer.drawText(centerItem.getDisplayName(), cx - (ICON_SIZE / 2), cy - (ICON_SIZE / 2) - 10, 0xFFFFFF);
        }
    }
}
