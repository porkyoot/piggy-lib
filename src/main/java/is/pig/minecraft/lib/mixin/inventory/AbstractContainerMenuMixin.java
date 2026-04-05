package is.pig.minecraft.lib.mixin.inventory;

import is.pig.minecraft.lib.action.telemetry.ActionForensics;
import is.pig.minecraft.lib.util.telemetry.MetaActionSessionManager;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.slf4j.event.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to capture and log every slot modification in any container menu.
 * Provides the "absolute verbosity" requested for action forensics.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method = "setItem", at = @At("HEAD"))
    private void onSetItem(int slotId, int stateId, ItemStack stack, CallbackInfo ci) {
        logSlotChange("setItem", slotId, stack);
    }

    @Inject(method = "setRemoteSlot", at = @At("HEAD"))
    private void onSetRemoteSlot(int slotId, ItemStack stack, CallbackInfo ci) {
        logSlotChange("setRemoteSlot", slotId, stack);
    }

    @Inject(method = "setRemoteCarried", at = @At("HEAD"))
    private void onSetRemoteCarried(ItemStack stack, CallbackInfo ci) {
        logSlotChange("setRemoteCarried", -1, stack); // -1 for cursor
    }

    private void logSlotChange(String source, int slotId, ItemStack stack) {
        if (!is.pig.minecraft.lib.config.PiggyClientConfig.getInstance().isFullActionDebug() && 
            MetaActionSessionManager.getInstance().getCurrentSession().isEmpty()) {
            return;
        }

        String itemName = stack.isEmpty() ? "Empty" : stack.getItem().toString() + " x" + stack.getCount();
        String message = String.format("SlotChange [%s] | Slot:%d | Item:%s", source, slotId, itemName);

        // 1. Log to the continuous forensic log (if enabled)
        ActionForensics.getInstance().log("SLOT_CHG", "piggy-lib", "SlotSync", message);

        // 2. Log to the current active MetaActionSession (if any)
        MetaActionSessionManager.getInstance().getCurrentSession().ifPresent(session -> {
            session.log(Level.INFO, message);
        });
    }
}
