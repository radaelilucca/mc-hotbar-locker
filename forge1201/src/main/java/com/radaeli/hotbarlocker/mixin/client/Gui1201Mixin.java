package com.radaeli.hotbarlocker.mixin.client;

import com.radaeli.hotbarlocker.client.LockSlotRenderer;

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

/** Draws the same lock frames, selection tint and padlocks as 1.21.1 through the shared renderer. */
@Mixin(Gui.class)
abstract class Gui1201Mixin {
    @Shadow @Final protected static ResourceLocation WIDGETS_LOCATION;

    @Inject(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void hotbarlocker$renderLockFrames(float partialTick, GuiGraphics graphics, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        // The hotbar slot is 20x20 and starts two pixels above/left of its 16x16 item icon.
        LockSlotRenderer.renderBackground(graphics, player, graphics.guiWidth() / 2 - 90, graphics.guiHeight() - 21);
    }

    @Inject(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 1
            )
    )
    private void hotbarlocker$tintSelection(float partialTick, GuiGraphics graphics, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        LockSlotRenderer.beginSelection(graphics, player);
    }

    @Inject(
            method = "renderHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 1,
                    shift = At.Shift.AFTER
            )
    )
    private void hotbarlocker$traceSelection(float partialTick, GuiGraphics graphics, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        LockSlotRenderer.endSelection(graphics, player, graphics.guiWidth() / 2 - 90, graphics.guiHeight() - 21,
                (x, y) -> graphics.blit(WIDGETS_LOCATION, x, y, 0, 22, 24, 22));
    }

    @Inject(
            method = "renderHotbar",
            at = @At("TAIL")
    )
    private void hotbarlocker$renderLockBadges(float partialTick, GuiGraphics graphics, CallbackInfo callback) {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;
        LockSlotRenderer.renderBadges(graphics, player, graphics.guiWidth() / 2 - 90, graphics.guiHeight() - 21);
    }
}
