package is.pig.minecraft.lib.util.telemetry.formatter;

import is.pig.minecraft.lib.util.telemetry.data.FallPredictionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import is.pig.minecraft.lib.util.telemetry.StructuredEvent;
import is.pig.minecraft.lib.util.telemetry.EventTranslatorRegistry;
import is.pig.minecraft.lib.I18n;
import java.util.function.BiFunction;
import java.util.Map;
import java.util.TreeMap;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Standardized telemetry formatting for Minecraft game objects.
 */
public class PiggyTelemetryFormatter {

    /**
     * Translates a structured event into a human-readable narrative.
     * Use this as the 'Narrative Engine' to generate descriptive stories from telemetry data.
     */
    public static String formatNarrative(StructuredEvent event) {
        if (event == null) return "Unknown Event";
        
        String translatedAndFormattedString;
        BiFunction<StructuredEvent, I18n, String> translator = EventTranslatorRegistry.getInstance().getTranslator(event.getClass());
        
        if (translator != null) {
            translatedAndFormattedString = translator.apply(event, I18n.getInstance());
        } else {
            // Generic Narrative Engine Fallback
            String template = I18n.getInstance().translate(event.getEventKey());
            if (template.equals(event.getEventKey())) {
                translatedAndFormattedString = "[No Narrative] " + event.getEventKey() + ": " + event.getEventData();
            } else {
                try {
                    // Sort keys to provide deterministic positional arguments for String.format
                    Map<String, Object> sortedData = new TreeMap<>(event.getEventData());
                    translatedAndFormattedString = String.format(template, sortedData.values().toArray());
                } catch (Exception e) {
                    translatedAndFormattedString = "[Narrative Error] " + template + " | Data: " + event.getEventData();
                }
            }
        }
        
        return String.format("[%s] %s", event.getCategoryIcon(), translatedAndFormattedString);
    }

    public static String formatItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "[Item|empty]";
        return String.format("[Item|%s|%d/%d]", 
            FormatterUtils.getId(stack.getItem()), stack.getCount(), stack.getMaxStackSize());
    }

    public static String formatEntity(Entity entity) {
        if (entity == null) return "[Entity|null]";
        
        StringBuilder sb = new StringBuilder("[Entity|");
        sb.append(FormatterUtils.getId(entity.getType())).append("|");
        sb.append(entity.level().dimension().location()).append("|");
        sb.append(FormatterUtils.formatVec3(entity.position())).append("|");
        sb.append(FormatterUtils.formatVec3(entity.getDeltaMovement())).append("|");
        sb.append(String.format("(%.2f, %.2f)", entity.getXRot(), entity.getYRot()));
        sb.append("]");
        
        return sb.toString();
    }

    public static String formatPlayer(Player player) {
        if (player == null) return "[Player|null]";
        
        StringBuilder sb = new StringBuilder("[Player|");
        sb.append(player.getName().getString()).append("|");
        sb.append(player.level().dimension().location()).append("|");
        sb.append(FormatterUtils.formatVec3(player.position())).append("|");
        sb.append("Main:").append(formatItem(player.getMainHandItem())).append("|");
        sb.append("Off:").append(formatItem(player.getOffhandItem()));
        sb.append("]");
        
        return sb.toString();
    }

    public static String formatBlock(BlockPos pos, BlockState state, Level level) {
        if (state == null) return "[Block|null]";
        
        StringBuilder sb = new StringBuilder("[Block|");
        sb.append(FormatterUtils.getId(state.getBlock())).append("|");
        sb.append(FormatterUtils.formatBlockPos(pos)).append("|");
        
        // Compact blockstate properties
        String props = state.getValues().entrySet().stream()
            .map(e -> e.getKey().getName() + "=" + e.getValue().toString())
            .collect(Collectors.joining(",", "{", "}"));
        sb.append(props);
        sb.append("]");
        
        return sb.toString();
    }

    public static String formatInventory(List<net.minecraft.world.inventory.Slot> slots) {
        if (slots == null || slots.isEmpty()) return "[Inventory|empty]";
        
        return slots.stream()
            .filter(slot -> !slot.getItem().isEmpty())
            .map(slot -> String.format("Slot %d:%s", slot.index, formatItem(slot.getItem())))
            .collect(Collectors.joining(", ", "[Inventory|", "]"));
    }

    public static String formatContainer(net.minecraft.world.Container container) {
        if (container == null) return "[Container|null]";
        if (container.isEmpty()) return "[Container|empty]";
        
        StringBuilder sb = new StringBuilder("[Container|");
        boolean first = true;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                if (!first) sb.append(", ");
                sb.append(String.format("Slot %d:%s", i, formatItem(stack)));
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String formatItemStacks(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return "[ItemStacks|empty]";
        
        StringBuilder sb = new StringBuilder("[ItemStacks|");
        boolean first = true;
        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            if (!stack.isEmpty()) {
                if (!first) sb.append(", ");
                sb.append(String.format("%d:%s", i, formatItem(stack)));
                first = false;
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public static String formatNearbyEntities(BlockPos center, double radius, Level level) {
        if (level == null || center == null) return "[NearbyEntities|error]";
        
        net.minecraft.world.phys.AABB area = new net.minecraft.world.phys.AABB(center).inflate(radius);
        List<Entity> entities = level.getEntities((Entity) null, area, e -> true);
        
        if (entities.isEmpty()) return "[NearbyEntities|none]";
        
        return entities.stream()
            .map(e -> String.format("%s@%s", FormatterUtils.getId(e.getType()), FormatterUtils.formatVec3(e.position())))
            .collect(Collectors.joining(", ", "[NearbyEntities|", "]"));
    }

    public static String formatPhysics(Entity entity) {
        if (entity == null) return "[Physics|null]";
        
        StringBuilder sb = new StringBuilder("[Physics|");
        sb.append("Pos:").append(FormatterUtils.formatVec3(entity.position())).append("|");
        sb.append("Vel:").append(FormatterUtils.formatVec3(entity.getDeltaMovement())).append("|");
        sb.append("Rot:").append(String.format("(%.2f, %.2f)", entity.getXRot(), entity.getYRot())).append("|");
        sb.append("Falling:").append(entity.fallDistance > 0);
        sb.append("]");
        
        return sb.toString();
    }

    public static String formatInventoryContext(ItemStack cursor, int sourceSlot, int targetSlot, ItemStack sourceItem, ItemStack targetItem) {
        StringBuilder sb = new StringBuilder("[InvContext|");
        sb.append("Cursor:").append(formatItem(cursor)).append("|");
        sb.append(String.format("Slot %d:%s -> Slot %d:%s", 
            sourceSlot, formatItem(sourceItem), targetSlot, formatItem(targetItem)));
        sb.append("]");
        return sb.toString();
    }

    public static String formatTrajectory(FallPredictionResult prediction, Level level) {
        if (prediction == null) return "[Trajectory|null]";
        
        StringBuilder sb = new StringBuilder("[Trajectory|");
        sb.append("Fatal:").append(prediction.isFatal()).append("|");
        sb.append("ImpactTicks:").append(prediction.ticksToImpact()).append("|");
        sb.append("ImpactPos:").append(FormatterUtils.formatBlockPos(prediction.landingPos())).append("|");
        
        if (level != null) {
            BlockPos solidBelow = findFirstSolidBelow(prediction.landingPos(), level);
            if (solidBelow != null) {
                sb.append("SolidBelow:").append(formatBlock(solidBelow, level.getBlockState(solidBelow), level));
            } else {
                sb.append("SolidBelow:none");
            }
        }
        
        sb.append("]");
        return sb.toString();
    }

    public static String formatFullPlayerInventory(Player player) {
        if (player == null) return "[PlayerInv|null]";
        
        StringBuilder sb = new StringBuilder("[PlayerInv|");
        
        // Hotbar (0-8)
        sb.append("Hotbar:[");
        for (int i = 0; i < 9; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append(":").append(formatItem(player.getInventory().getItem(i)));
        }
        sb.append("]|");
        
        // Main Inventory (9-35)
        sb.append("Main:[");
        for (int i = 9; i < 36; i++) {
            if (i > 9) sb.append(", ");
            sb.append(i).append(":").append(formatItem(player.getInventory().getItem(i)));
        }
        sb.append("]|");
        
        // Offhand
        sb.append("Offhand:").append(formatItem(player.getOffhandItem())).append("|");
        
        // Armor
        sb.append("Armor:[");
        for (int i = 0; i < 4; i++) {
            if (i > 0) sb.append(", ");
            sb.append(i).append(":").append(formatItem(player.getInventory().armor.get(i)));
        }
        sb.append("]|");
        
        // Cursor
        sb.append("Cursor:").append(formatItem(player.containerMenu.getCarried()));
        
        sb.append("]");
        return sb.toString();
    }

    public static String formatValidationFailure(int slotIndex, ItemStack expected, ItemStack actual) {
        return String.format("[ValidationFailure|Slot %d|Expected:%s|Actual:%s]", 
            slotIndex, formatItem(expected), formatItem(actual));
    }

    private static BlockPos findFirstSolidBelow(BlockPos start, Level level) {
        BlockPos.MutableBlockPos mutable = start.mutable();
        mutable.move(net.minecraft.core.Direction.DOWN);
        while (mutable.getY() >= level.getMinBuildHeight()) {
            if (level.getBlockState(mutable).blocksMotion()) {
                return mutable.immutable();
            }
            mutable.move(net.minecraft.core.Direction.DOWN);
        }
        return null;
    }
}
