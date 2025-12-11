package is.pig.minecraft.lib.ui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Interface for any enum or object that wishes to be displayed in the GenericRadialMenu.
 * Moved from piggy-build to piggy-lib for shared use.
 */
public interface RadialMenuItem {
    ResourceLocation getIconLocation(boolean isSelected);
    Component getDisplayName();
}