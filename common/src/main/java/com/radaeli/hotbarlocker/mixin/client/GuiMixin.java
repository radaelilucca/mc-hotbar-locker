package com.radaeli.hotbarlocker.mixin.client;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws a subtle lock frame after vanilla has drawn the item hotbar. */
@Mixin(Gui.class)
abstract class GuiMixin {
    @Inject(method = "renderItemHotbar", at = @At("TAIL"))
    private void hotbarlocker$renderLockMarkers(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;
        int left = minecraft.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = minecraft.getWindow().getGuiScaledHeight() - 22;
        for (int slot = 0; slot < 9; slot++) {
            if (HotbarLockService.state(player).isLocked(slot)) {
                drawLockFrame(graphics, left + slot * 20, top);
            }
        }
    }

    /**
     * Four inset rails communicate the lock without occupying the 16x16 item
     * area. Corners stay open so Create's toolbox frame remains legible.
     */
    private static void drawLockFrame(GuiGraphics graphics, int slotLeft, int slotTop) {
        int mutedGold = 0xD09B741F;
        int brightGold = 0xF0E1B64A;
        graphics.fill(slotLeft + 4, slotTop + 1, slotLeft + 16, slotTop + 2, mutedGold);
        graphics.fill(slotLeft + 4, slotTop + 18, slotLeft + 16, slotTop + 19, mutedGold);
        graphics.fill(slotLeft + 1, slotTop + 4, slotLeft + 2, slotTop + 16, mutedGold);
        graphics.fill(slotLeft + 18, slotTop + 4, slotLeft + 19, slotTop + 16, mutedGold);
        graphics.fill(slotLeft + 7, slotTop + 1, slotLeft + 13, slotTop + 2, brightGold);
        graphics.fill(slotLeft + 7, slotTop + 18, slotLeft + 13, slotTop + 19, brightGold);
    }
}
