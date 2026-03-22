package is.pig.minecraft.lib.inventory.search;

import net.minecraft.world.item.ItemStack;

@FunctionalInterface
public interface ItemCondition {
    boolean matches(ItemStack stack);
}
