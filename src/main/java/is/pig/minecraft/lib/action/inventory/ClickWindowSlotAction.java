package is.pig.minecraft.lib.action.inventory;

import is.pig.minecraft.api.AbstractAction;
import is.pig.minecraft.api.ActionPriority;
import is.pig.minecraft.api.ClickType;
import is.pig.minecraft.api.registry.PiggyServiceRegistry;
import is.pig.minecraft.api.spi.InventoryInteractionAdapter;
import is.pig.minecraft.api.spi.ItemDataAdapter;
import is.pig.minecraft.api.spi.ScreenAdapter;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Platform-agnostic action for clicking a slot in a container screen.
 */
public class ClickWindowSlotAction extends AbstractAction {
    private final int containerId;
    private final int slotId;
    private final int button;
    private final ClickType clickType;
    private final Predicate<Object> expectedItemPredicate;
    private Predicate<Object> expectedCursorBefore = (stack) -> true;
    private Predicate<Object> expectedCursorAfter = (stack) -> true;
    private Predicate<Object> expectedSlotBefore = (stack) -> true;

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, ActionPriority priority, Predicate<Object> expectedItemPredicate) {
        super(sourceMod, priority);
        this.containerId = containerId;
        this.slotId = slotId;
        this.button = button;
        this.clickType = clickType;
        this.expectedItemPredicate = expectedItemPredicate;
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, Predicate<Object> expectedItemPredicate) {
        this(containerId, slotId, button, clickType, sourceMod, ActionPriority.NORMAL, expectedItemPredicate);
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod) {
        this(containerId, slotId, button, clickType, sourceMod, ActionPriority.NORMAL, (stack) -> true);
    }

    public ClickWindowSlotAction(int containerId, int slotId, int button, ClickType clickType, String sourceMod, ActionPriority priority) {
        this(containerId, slotId, button, clickType, sourceMod, priority, (stack) -> true);
    }

    public ClickWindowSlotAction withExpectedCursorBefore(Predicate<Object> expected) {
        this.expectedCursorBefore = expected;
        return this;
    }

    public ClickWindowSlotAction withExpectedCursorAfter(Predicate<Object> expected) {
        this.expectedCursorAfter = expected;
        return this;
    }

    public ClickWindowSlotAction withExpectedSlotBefore(Predicate<Object> expected) {
        this.expectedSlotBefore = expected;
        return this;
    }

    @Override
    public boolean checkPreconditions(Object client) {
        ScreenAdapter screenAdapter = PiggyServiceRegistry.getScreenAdapter();
        if (!screenAdapter.isContainerScreenOpen(client)) return false;
        
        if (this.slotId >= 0) {
            Object stackBefore = screenAdapter.getStackInSlot(client, this.slotId);
            if (!expectedSlotBefore.test(stackBefore)) {
                return false;
            }
        }

        return expectedCursorBefore.test(screenAdapter.getCursorStack(client));
    }

    @Override
    protected void onExecute(Object client) {
        InventoryInteractionAdapter interaction = PiggyServiceRegistry.getInventoryInteractionAdapter();
        interaction.clickSlot(client, this.containerId, this.slotId, this.button, this.clickType);
    }

    @Override
    protected Optional<Boolean> verify(Object client) {
        ScreenAdapter screenAdapter = PiggyServiceRegistry.getScreenAdapter();
        if (screenAdapter.isContainerScreenOpen(client) && screenAdapter.getContainerId(client) == this.containerId) {
            if (this.slotId >= 0) {
                Object stack = screenAdapter.getStackInSlot(client, this.slotId);
                Object cursor = screenAdapter.getCursorStack(client);
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
    public String getTelemetry(Object client) {
        ScreenAdapter screenAdapter = PiggyServiceRegistry.getScreenAdapter();
        ItemDataAdapter itemAdapter = PiggyServiceRegistry.getItemDataAdapter();
        
        String itemInfo = "n/a";
        String cursorInfo = "n/a";
        
        if (screenAdapter.isContainerScreenOpen(client)) {
            Object cursorStack = screenAdapter.getCursorStack(client);
            cursorInfo = itemAdapter.getCount(cursorStack) <= 0 ? "Air" : itemAdapter.getName(cursorStack) + " x" + itemAdapter.getCount(cursorStack);
            
            if (slotId >= 0) {
                Object slotStack = screenAdapter.getStackInSlot(client, slotId);
                itemInfo = itemAdapter.getCount(slotStack) <= 0 ? "Air" : itemAdapter.getName(slotStack) + " x" + itemAdapter.getCount(slotStack);
            }
        }
        
        return String.format("Slot=%d, Button=%d, Type=%s | SlotItem: %s, Cursor: %s", 
            slotId, button, clickType, itemInfo, cursorInfo);
    }
}
