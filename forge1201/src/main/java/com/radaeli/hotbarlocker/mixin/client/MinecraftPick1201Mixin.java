package com.radaeli.hotbarlocker.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla syncs only the selected slot after Creative pick. Also sync moved/main stacks. */
@Mixin(Minecraft.class)
abstract class MinecraftPick1201Mixin {
    @Redirect(method = "pickBlock", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Inventory;setPickedItem(Lnet/minecraft/world/item/ItemStack;)V"))
    private void hotbarlocker$syncAllChangedPickSlots(Inventory inventory, ItemStack picked) {
        ItemStack[] before = inventory.items.stream().map(ItemStack::copy).toArray(ItemStack[]::new);
        inventory.setPickedItem(picked);
        var client = (Minecraft) (Object) this;
        if (client.gameMode == null) return;
        for (int slot = 0; slot < before.length; slot++) {
            if (!ItemStack.matches(before[slot], inventory.getItem(slot))) {
                client.gameMode.handleCreativeModeItemAdd(inventory.getItem(slot), slot < 9 ? slot + 36 : slot);
            }
        }
    }
}
