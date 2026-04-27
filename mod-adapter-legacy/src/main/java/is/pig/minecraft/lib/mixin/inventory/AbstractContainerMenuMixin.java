package is.pig.minecraft.lib.mixin.inventory;

import is.pig.minecraft.lib.legacy.LegacyMixinCallbacks;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
        logSlotChange("setRemoteCarried", -1, stack);
    }

    private void logSlotChange(String source, int slotId, ItemStack stack) {
        String itemName = stack.isEmpty() ? "Empty" : stack.getItem().toString() + " x" + stack.getCount();
        LegacyMixinCallbacks.onSlotChange(source, slotId, itemName);
    }
}
