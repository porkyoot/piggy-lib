package is.pig.minecraft.lib.common;

import is.pig.minecraft.lib.api.IModAdapter;
import is.pig.minecraft.lib.api.IInventoryManager;
import java.util.UUID;

public class PiggyCore {
    private static IModAdapter adapter;

    public static void init(IModAdapter modAdapter) {
        adapter = modAdapter;
        System.out.println("piggy-lib: PiggyCore initialized with " + modAdapter.getClass().getSimpleName());
        
        IInventoryManager inventoryManager = adapter.getInventoryManager();
        if (inventoryManager != null) {
            UUID dummyUuid = UUID.randomUUID();
            boolean hasItem = inventoryManager.hasItem(dummyUuid, "minecraft:diamond");
            System.out.println("piggy-lib: Dependency Inversion check: hasItem returned " + hasItem);
        }
    }

    public static IModAdapter getAdapter() {
        return adapter;
    }
}
