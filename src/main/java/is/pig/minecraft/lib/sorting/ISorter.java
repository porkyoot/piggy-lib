package is.pig.minecraft.lib.sorting;

import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/**
 * Interface for inventory sorting algorithms.
 */
public interface ISorter {

    /**
     * Sorts the given list of ItemStacks in place.
     * 
     * @param items The list of items to sort. Mutiable.
     */
    void sort(List<ItemStack> items);

    /**
     * Returns a comparator for ItemStacks defined by this sorter.
     * This can be used if standard list sorting is preferred.
     *
     * @return A comparator for ItemStacks.
     */
    Comparator<ItemStack> getComparator();

    /**
     * Gets the unique identifier for this sorter (e.g., "alphabetical", "color").
     *
     * @return The unique ID.
     */
    String getId();

    /**
     * Gets the display name of this sorter.
     *
     * @return The display name.
     */
    String getName();
}
