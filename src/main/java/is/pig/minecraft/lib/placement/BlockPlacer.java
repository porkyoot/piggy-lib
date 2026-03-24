package is.pig.minecraft.lib.placement;

import is.pig.minecraft.lib.action.IAction;
import is.pig.minecraft.lib.action.PiggyActionQueue;
import is.pig.minecraft.lib.action.world.InteractBlockAction;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockPlacer {

    public static boolean placeBlock(BlockPos pos, Direction face, InteractionHand hand) {
        return placeBlock(pos, face, hand, false);
    }

    public static boolean placeBlock(BlockPos pos, Direction face, InteractionHand hand, boolean ignoreGlobalCps) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) {
            return false;
        }

        BlockHitResult hitResult = createHitResult(pos, face);
        return placeBlock(hitResult, hand, ignoreGlobalCps);
    }

    public static boolean placeBlock(BlockHitResult hitResult, InteractionHand hand) {
        return placeBlock(hitResult, hand, false);
    }

    public static boolean placeBlock(BlockHitResult hitResult, InteractionHand hand, boolean ignoreGlobalCps) {
        try {
            IAction action = createAction(hitResult, hand, ignoreGlobalCps);
            if (action != null) {
                Minecraft mc = Minecraft.getInstance();
                // To avoid Mixin Accessor dependency cleanly, we simply enqueue the action
                // The ActionQueue's Interaction wrappers handle timing properly.
                PiggyActionQueue.getInstance().enqueue(action);
                triggerInventoryRefill(mc);
                return true;
            }
        } catch (Exception e) {
            is.pig.minecraft.lib.util.PiggyLog log = new is.pig.minecraft.lib.util.PiggyLog("piggy-lib", "BlockPlacer");
            log.error("Placement using action failed", e);
            return false;
        }
        return false;
    }

    public static IAction createAction(BlockHitResult hitResult, InteractionHand hand, boolean ignoreGlobalCps) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.gameMode == null) {
            return null;
        }

        ItemStack itemStack = mc.player.getItemInHand(hand);
        boolean isWaterBucket = itemStack.is(net.minecraft.world.item.Items.WATER_BUCKET);
        BlockPos targetPos = hitResult.getBlockPos().relative(hitResult.getDirection());
        
        java.util.function.BooleanSupplier verifyCondition = () -> {
            if (mc.level == null) return false;
            net.minecraft.world.level.block.state.BlockState state = mc.level.getBlockState(targetPos);
            return isWaterBucket ? state.is(net.minecraft.world.level.block.Blocks.WATER) : !state.isAir();
        };

        var action = new InteractBlockAction(hitResult, hand, "piggy-lib", verifyCondition);
        if (ignoreGlobalCps) action.setIgnoreGlobalCps(true);
        return action;
    }

    private static void triggerInventoryRefill(Minecraft mc) {
        try {
            Class<?> clazz = Class.forName("is.pig.minecraft.inventory.handler.AutoRefillHandler");
            Object instance = clazz.getMethod("getInstance").invoke(null);
            clazz.getMethod("onTick", Minecraft.class).invoke(instance, mc);
        } catch (Exception e) {
            // Ignored
        }
    }

    public static BlockHitResult createHitResult(BlockPos pos, Direction face) {
        Vec3 center = Vec3.atCenterOf(pos);
        Vec3 hitPos = center.add(
                face.getStepX() * 0.5,
                face.getStepY() * 0.5,
                face.getStepZ() * 0.5);

        return new BlockHitResult(hitPos, face, pos, false);
    }

    public static BlockHitResult createHitResult(BlockPos pos, Direction face, double u, double v) {
        Vec3 hitPos = calculateHitPosition(pos, face, u, v);
        return new BlockHitResult(hitPos, face, pos, false);
    }

    private static Vec3 calculateHitPosition(BlockPos pos, Direction face, double u, double v) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        return switch (face) {
            case DOWN -> new Vec3(x + u, y, z + v);
            case UP -> new Vec3(x + u, y + 1.0, z + v);
            case NORTH -> new Vec3(x + u, y + v, z);
            case SOUTH -> new Vec3(x + u, y + v, z + 1.0);
            case WEST -> new Vec3(x, y + v, z + u);
            case EAST -> new Vec3(x + 1.0, y + v, z + u);
        };
    }

    public static boolean canPlaceBlock(BlockPos pos, Direction face) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.level == null) {
            return false;
        }

        ItemStack itemStack = mc.player.getItemInHand(InteractionHand.MAIN_HAND);
        if (itemStack.isEmpty()) {
            return false;
        }

        if (!mc.level.isLoaded(pos)) {
            return false;
        }

        return true;
    }
}
