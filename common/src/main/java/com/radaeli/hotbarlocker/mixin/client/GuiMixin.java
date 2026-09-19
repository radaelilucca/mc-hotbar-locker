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

/** Draws a complete gold frame underneath vanilla's selected-slot sprite. */
@Mixin(Gui.class)
abstract class GuiMixin {
    @Inject(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            )
    )
    private void hotbarlocker$renderLockMarkersBeforeSelection(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        Minecraft minecraft = Minecraft.getInstance();
        Player player = minecraft.player;
        if (player == null) return;
        // Vanilla's 16x16 item starts at (guiWidth / 2 - 88, guiHeight - 19).
        // Our frame is centered at local (10, 10), so its 20x20 slot origin
        // must be two pixels before the item, one inside the hotbar background.
        // Using the background origin (-91, -22) shifts the frame up and left.
        int left = graphics.guiWidth() / 2 - 90;
        int top = graphics.guiHeight() - 21;
        for (int slot = 0; slot < 9; slot++) {
            if (HotbarLockService.state(player).isLocked(slot)) {
                drawLockFrame(graphics, left + slot * 20, top);
            }
        }
    }

    /**
     * A complete inset border with compact corner accents. It is rendered before
     * the vanilla selected-slot sprite; its center matches the 16x16 item.
     */
    private static void drawLockFrame(GuiGraphics graphics, int slotLeft, int slotTop) {
        int shadowGold = 0xD0784512;
        int gold = 0xF0D9A33D;
        int cornerGold = 0xFFF0C45A;

        // Complete dark-gold outline, then a warm inner bevel; no glow or shine.
        graphics.fill(slotLeft + 1, slotTop + 1, slotLeft + 19, slotTop + 2, shadowGold);
        graphics.fill(slotLeft + 1, slotTop + 18, slotLeft + 19, slotTop + 19, shadowGold);
        graphics.fill(slotLeft + 1, slotTop + 2, slotLeft + 2, slotTop + 18, shadowGold);
        graphics.fill(slotLeft + 18, slotTop + 2, slotLeft + 19, slotTop + 18, shadowGold);
        graphics.fill(slotLeft + 2, slotTop + 2, slotLeft + 18, slotTop + 3, gold);
        graphics.fill(slotLeft + 2, slotTop + 3, slotLeft + 3, slotTop + 18, gold);
        graphics.fill(slotLeft + 3, slotTop + 17, slotLeft + 18, slotTop + 18, gold);
        graphics.fill(slotLeft + 17, slotTop + 3, slotLeft + 18, slotTop + 17, gold);

        // Two-pixel corner accents retain the ornament without competing with Create.
        graphics.fill(slotLeft + 2, slotTop + 2, slotLeft + 4, slotTop + 3, cornerGold);
        graphics.fill(slotLeft + 2, slotTop + 2, slotLeft + 3, slotTop + 4, cornerGold);
        graphics.fill(slotLeft + 16, slotTop + 2, slotLeft + 18, slotTop + 3, cornerGold);
        graphics.fill(slotLeft + 17, slotTop + 2, slotLeft + 18, slotTop + 4, cornerGold);
        graphics.fill(slotLeft + 2, slotTop + 17, slotLeft + 4, slotTop + 18, cornerGold);
        graphics.fill(slotLeft + 2, slotTop + 16, slotLeft + 3, slotTop + 18, cornerGold);
        graphics.fill(slotLeft + 16, slotTop + 17, slotLeft + 18, slotTop + 18, cornerGold);
        graphics.fill(slotLeft + 17, slotTop + 16, slotLeft + 18, slotTop + 18, cornerGold);
    }
}
