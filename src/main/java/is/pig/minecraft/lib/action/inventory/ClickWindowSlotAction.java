package is.pig.minecraft.lib.action.inventory;

import is.pig.minecraft.lib.action.AbstractAction;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;
import java.util.function.Predicate;

public class ClickWindowSlotAction extends AbstractAction {
    private final int containerId;
    private final int slotId;
    private final int button;
    private final ClickType clickType;
    private final Predicate<ItemStack> expectedItemPredicate;
    private Predicate<ItemStack> expectedCursorBefore = (stack) -> true;
    private Predicate<ItemStack> expectedCursorAfter = (stack) -> true;
    private Predicate<ItemStack> expectedSlotBefore = (stack) -> true;

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, is.pig.minecraft.lib.action.ActionPriority priority, Predicate<ItemStack> expectedItemPredicate) {
        super(sourceMod, priority);
        this.containerId = containerId;
        this.slotId = slotId;
        this.button = button;
        this.clickType = clickType;
        this.expectedItemPredicate = expectedItemPredicate;
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, Predicate<ItemStack> expectedItemPredicate) {
        this(containerId, slotId, button, clickType, sourceMod, is.pig.minecraft.lib.action.ActionPriority.NORMAL, expectedItemPredicate);
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, ItemStack expectedItem) {
        this(containerId, slotId, button, clickType, sourceMod, (stack) -> ItemStack.isSameItemSameComponents(stack, expectedItem));
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod) {
        this(containerId, slotId, button, clickType, sourceMod, is.pig.minecraft.lib.action.ActionPriority.NORMAL, (stack) -> true);
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, is.pig.minecraft.lib.action.ActionPriority priority) {
        this(containerId, slotId, button, clickType, sourceMod, priority, (stack) -> true);
    }

    public ClickWindowSlotAction withExpectedCursorBefore(Predicate<ItemStack> expected) {
        this.expectedCursorBefore = expected;
        return this;
    }

    public ClickWindowSlotAction withExpectedCursorAfter(Predicate<ItemStack> expected) {
        this.expectedCursorAfter = expected;
        return this;
    }

    public ClickWindowSlotAction withExpectedSlotBefore(Predicate<ItemStack> expected) {
        this.expectedSlotBefore = expected;
        return this;
    }

    @Override
    public boolean checkPreconditions(Minecraft client) {
        if (client.player == null || client.player.containerMenu == null) return false;
        
        if (this.slotId >= 0 && this.slotId < client.player.containerMenu.slots.size()) {
            ItemStack stackBefore = client.player.containerMenu.getSlot(this.slotId).getItem();
            if (!expectedSlotBefore.test(stackBefore)) {
                return false;
            }
        }

        return expectedCursorBefore.test(client.player.containerMenu.getCarried());
    }

    @Override
    protected void onExecute(Minecraft client) {
        Player player = client.player;
        if (player != null && client.gameMode != null) {
            client.gameMode.handleInventoryMouseClick(this.containerId, this.slotId, this.button, this.clickType,
                    player);
        }
    }
    
    

    @Override
    protected Optional<Boolean> verify(Minecraft client) {
        if (client.player != null && client.player.containerMenu != null && client.player.containerMenu.containerId == this.containerId) {
            if (this.slotId >= 0 && this.slotId < client.player.containerMenu.slots.size()) {
                ItemStack stack = client.player.containerMenu.getSlot(this.slotId).getItem();
                ItemStack cursor = client.player.containerMenu.getCarried();
                if (expectedItemPredicate.test(stack) && expectedCursorAfter.test(cursor)) {
                    return Optional.of(true);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String getName() {
        return "Click Window Slot";
    }

    @Override
    public boolean isClick() {
        return true;
    }

    @Override
    public String getTelemetry(Minecraft client) {
        String itemInfo = "n/a";
        String cursorInfo = "n/a";
        
        if (client.player != null && client.player.containerMenu != null) {
            ItemStack cursorStack = client.player.containerMenu.getCarried();
            cursorInfo = cursorStack.isEmpty() ? "Air" : cursorStack.getItem().toString() + " x" + cursorStack.getCount();
            
            if (slotId >= 0 && slotId < client.player.containerMenu.slots.size()) {
                ItemStack slotStack = client.player.containerMenu.getSlot(slotId).getItem();
                itemInfo = slotStack.isEmpty() ? "Air" : slotStack.getItem().toString() + " x" + slotStack.getCount();
            }
        }
        
        return String.format("Slot=%d, Button=%d, Type=%s | SlotItem: %s, Cursor: %s", 
            slotId, button, clickType, itemInfo, cursorInfo);
    }
}
