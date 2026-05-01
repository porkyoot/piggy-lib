package is.pig.minecraft.lib.util.telemetry.formatter;
import is.pig.minecraft.api.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Common utilities for rounding and Minecraft type serialization.
 */
public class FormatterUtils {

    /**
     * Rounds a double to 2 decimal places.
     */
    public static String formatDouble(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /**
     * Formats a Vec3 as (X, Y, Z) with 2 decimal places.
     */
    public static String formatVec3(Vec3 vec) {
        if (vec == null) return "null";
        return String.format(Locale.ROOT, "(%s, %s, %s)", 
            formatDouble(vec.x), formatDouble(vec.y), formatDouble(vec.z));
    }

    public static String formatVec3(is.pig.minecraft.api.Vec3 vec) {
        if (vec == null) return "null";
        return String.format(Locale.ROOT, "(%s, %s, %s)", 
            formatDouble(vec.x()), formatDouble(vec.y()), formatDouble(vec.z()));
    }

    /**
     * Formats a BlockPos as (X, Y, Z).
     */
    public static String formatBlockPos(BlockPos pos) {
        if (pos == null) return "null";
        return String.format(Locale.ROOT, "(%d, %d, %d)", pos.getX(), pos.getY(), pos.getZ());
    }

    public static String formatBlockPos(is.pig.minecraft.api.BlockPos pos) {
        if (pos == null) return "null";
        return String.format(Locale.ROOT, "(%d, %d, %d)", pos.x(), pos.y(), pos.z());
    }

    /**
     * Extracts the ID of a registry entry (e.g., Item, Block).
     */
    public static String getId(Object entry) {
        if (entry == null) return "null";
        
        ResourceLocation id = null;
        if (entry instanceof net.minecraft.world.item.Item item) {
            id = BuiltInRegistries.ITEM.getKey(item);
        } else if (entry instanceof net.minecraft.world.level.block.Block block) {
            id = BuiltInRegistries.BLOCK.getKey(block);
        } else if (entry instanceof net.minecraft.world.entity.EntityType<?> type) {
            id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        }
        
        return id != null ? id.toString() : entry.getClass().getSimpleName();
    }
}
