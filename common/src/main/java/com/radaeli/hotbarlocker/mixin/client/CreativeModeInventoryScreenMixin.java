package com.radaeli.hotbarlocker.mixin.client;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps protected hotbar stacks when Creative's Shift-click trash action clears inventory. */
@Mixin(CreativeModeInventoryScreen.class)
abstract class CreativeModeInventoryScreenMixin {
    @Shadow private Slot destroyItemSlot;

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void hotbarlocker$clearCreativeInventoryExceptLockedHotbar(
            Slot slot, int slotId, int button, ClickType clickType, CallbackInfo callback) {
        if (slot != destroyItemSlot || clickType != ClickType.QUICK_MOVE) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.gameMode == null) return;

        int lockMask = HotbarLockService.mask(minecraft.player);
        if (lockMask == 0) return;
        var inventoryItems = minecraft.player.inventoryMenu.getItems();
        for (int menuSlot = 0; menuSlot < inventoryItems.size(); menuSlot++) {
            int hotbarSlot = menuSlot - 36;
            if (hotbarSlot >= 0 && hotbarSlot < 9 && (lockMask & (1 << hotbarSlot)) != 0) continue;

            // Match vanilla's creative trash action while omitting protected
            // hotbar slots. This avoids clearing and racing to restore stacks.
            minecraft.gameMode.handleCreativeModeItemAdd(ItemStack.EMPTY, menuSlot);
        }
        callback.cancel();
    }
}
