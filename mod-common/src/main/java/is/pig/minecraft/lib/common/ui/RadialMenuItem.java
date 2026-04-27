package is.pig.minecraft.lib.common.ui;

/**
 * Interface for any enum or object that wishes to be displayed in the GenericRadialMenu.
 */
public interface RadialMenuItem {
    String getIconPath(boolean isSelected);
    String getDisplayName();
    
    default java.util.List<? extends RadialMenuItem> getSubMenuItems() {
        return java.util.Collections.emptyList();
    }
}
