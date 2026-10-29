package com.radaeli.hotbarlocker.mixin.client;

import com.radaeli.hotbarlocker.client.LockSlotRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws lock frames under the selection sprite and items, tints the selection, then draws padlocks. */
@Mixin(Gui.class)
abstract class GuiMixin {
    @Shadow @Final private static ResourceLocation HOTBAR_SELECTION_SPRITE;

    @Inject(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void hotbarlocker$renderLockFrames(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        // The hotbar slot is 20x20 and starts two pixels above/left of its 16x16 item icon.
        LockSlotRenderer.renderBackground(graphics, player, graphics.guiWidth() / 2 - 90, graphics.guiHeight() - 21);
    }

    @Inject(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 1
            )
    )
    private void hotbarlocker$tintSelection(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        LockSlotRenderer.beginSelection(graphics, player);
    }

    @Inject(
            method = "renderItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void hotbarlocker$traceSelection(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        LockSlotRenderer.endSelection(graphics, player, graphics.guiWidth() / 2 - 90, graphics.guiHeight() - 21,
                (x, y) -> graphics.blitSprite(HOTBAR_SELECTION_SPRITE, x, y, 24, 23));
    }

    @Inject(
            method = "renderItemHotbar",
            at = @At("TAIL")
    )
    private void hotbarlocker$renderLockBadges(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        LockSlotRenderer.renderBadges(graphics, player, graphics.guiWidth() / 2 - 90, graphics.guiHeight() - 21);
    }
}
