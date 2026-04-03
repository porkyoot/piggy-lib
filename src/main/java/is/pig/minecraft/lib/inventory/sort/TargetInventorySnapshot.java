package is.pig.minecraft.lib.inventory.sort;

import net.minecraft.world.item.ItemStack;
import java.util.Map;

/**
 * A immutable snapshot of the desired state of a Minecraft inventory after a sort.
 * Primarily used by the RobustSortOrchestrator to perform state reconciliation.
 *
 * @param containerId The ID of the container being sorted (e.g. from {@code player.containerMenu.containerId}).
 * @param slotTargets A map of real Minecraft slot indices to their desired target ItemStacks.
 * @param cursorTarget The desired ItemStack for the player's cursor (mouse cursor) after sorting.
 * @param sourceMod The identifier of the mod triggering the sort (for action tracking and CPS bypass).
 */
public record TargetInventorySnapshot(
        int containerId,
        Map<Integer, ItemStack> slotTargets,
        ItemStack cursorTarget,
        String sourceMod
) {
    /**
     * Creates a deep-copy clone of this snapshot to ensure immutability of ItemStacks.
     * @return A new TargetInventorySnapshot with cloned ItemStacks.
     */
    public TargetInventorySnapshot copy() {
        return new TargetInventorySnapshot(
                containerId,
                Map.copyOf(slotTargets.entrySet().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue().copy()
                        ))),
                cursorTarget.copy(),
                sourceMod
        );
    }
}
