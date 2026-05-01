package is.pig.minecraft.lib.util;
import is.pig.minecraft.api.*;

public class TypeConverter {
    public static net.minecraft.core.BlockPos toMinecraft(is.pig.minecraft.api.BlockPos pos) {
        return pos == null ? null : new net.minecraft.core.BlockPos(pos.x(), pos.y(), pos.z());
    }

    public static net.minecraft.world.phys.Vec3 toMinecraft(is.pig.minecraft.api.Vec3 vec) {
        return vec == null ? null : new net.minecraft.world.phys.Vec3(vec.x(), vec.y(), vec.z());
    }
    
    public static is.pig.minecraft.api.BlockPos fromMinecraft(net.minecraft.core.BlockPos pos) {
        return pos == null ? null : new is.pig.minecraft.api.BlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static is.pig.minecraft.api.Vec3 fromMinecraft(net.minecraft.world.phys.Vec3 vec) {
        return vec == null ? null : new is.pig.minecraft.api.Vec3(vec.x, vec.y, vec.z);
    }
}
