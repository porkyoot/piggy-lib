package is.pig.minecraft.lib.common;

import is.pig.minecraft.lib.api.IModAdapter;

public class ModCommon {
    private static IModAdapter adapter;

    public static void initialize(IModAdapter modAdapter) {
        adapter = modAdapter;
        System.out.println("piggy-lib: ModCommon initialized with " + modAdapter.getClass().getSimpleName());
    }

    public static IModAdapter getAdapter() {
        return adapter;
    }
}
