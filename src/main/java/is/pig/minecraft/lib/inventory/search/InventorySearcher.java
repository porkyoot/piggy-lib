package is.pig.minecraft.lib.inventory.search;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class InventorySearcher {

    private InventorySearcher() {
        // Utility class
    }

    public static int findSlotInHotbar(Inventory inv, ItemCondition condition) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (condition.matches(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static int findSlotInMain(Inventory inv, ItemCondition condition) {
        // Player main inventory usually spans slots 9 to 35
        for (int i = 9; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (condition.matches(stack)) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> findAllSlots(Inventory inv, ItemCondition condition) {
        List<Integer> slots = new ArrayList<>();
        // inv.getContainerSize() returns the total number of slots
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (condition.matches(stack)) {
                slots.add(i);
            }
        }
        return slots;
    }
}
