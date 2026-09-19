package com.radaeli.hotbarlocker.mixin;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
abstract class InventoryMixin {
    @Inject(method = "setPickedItem", at = @At("HEAD"), cancellable = true)
    private void hotbarlocker$keepFullyLockedHotbar(ItemStack stack, CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        int matchingSlot = inventory.findSlotMatchingItem(stack);
        // Existing hotbar items are still freely selectable; only a new creative
        // pick needs a main-inventory destination.
        if (Inventory.isHotbarSlot(matchingSlot)) return;
        if (matchingSlot >= 0) return; // vanilla swaps only with the main inventory here.
        if (HotbarLockService.allLocked(inventory.player)) {
            HotbarLockService.placePickedItemInMainInventory(inventory, stack);
            callback.cancel();
            return;
        }
        int destination = HotbarLockService.suitableHotbarSlot(inventory, inventory.selected);
        ItemStack replaced = inventory.getItem(destination);
        if (!replaced.isEmpty()) {
            int mainInventorySlot = HotbarLockService.firstEmptyMainInventorySlot(inventory);
            if (mainInventorySlot < 0) {
                callback.cancel(); // Safe fallback: do not evict into a locked empty slot.
                return;
            }
            inventory.setItem(mainInventorySlot, replaced.copy());
        }
        inventory.setItem(destination, stack.copy());
        inventory.selected = destination;
        callback.cancel();
    }

    @Inject(method = "pickSlot", at = @At("HEAD"), cancellable = true)
    private void hotbarlocker$cancelSurvivalSwapWhenFullyLocked(int sourceSlot, CallbackInfo callback) {
        Inventory inventory = (Inventory) (Object) this;
        if (HotbarLockService.allLocked(inventory.player)) callback.cancel();
    }

    @Inject(method = "getSuitableHotbarSlot", at = @At("RETURN"), cancellable = true)
    private void hotbarlocker$skipLockedSlots(CallbackInfoReturnable<Integer> callback) {
        callback.setReturnValue(HotbarLockService.suitableHotbarSlot((Inventory) (Object) this, callback.getReturnValue()));
    }
}
