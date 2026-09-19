package com.radaeli.hotbarlocker.mixin.compat.create;

import com.radaeli.hotbarlocker.HotbarLockerApi;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Optional Create 6 Toolbox bridge. It uses string targeting and reflection so
 * Hotbar Locker has no runtime or compile dependency on Create.
 */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.equipment.toolbox.ToolboxEquipPacket", remap = false)
abstract class ToolboxEquipPacketMixin {
    private static final Logger LOGGER = LoggerFactory.getLogger("hotbarlocker/create");

    @Shadow @Final private BlockPos toolboxPos;
    @Shadow @Final private int slot;
    @Shadow @Final private int hotbarSlot;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void hotbarlocker$redirectLockedToolboxSlot(ServerPlayer player, CallbackInfo callback) {
        var destination = HotbarLockerApi.findReplacementSlot(player, hotbarSlot);
        if (destination.isEmpty()) {
            // This covers both an equip and an explicit unequip. Create must not
            // write a return stack back into a protected slot.
            callback.cancel();
            return;
        }
        int targetSlot = destination.getAsInt();
        if (targetSlot == hotbarSlot) return;
        try {
            Object redirected = getClass().getConstructor(BlockPos.class, int.class, int.class)
                    .newInstance(toolboxPos, slot, targetSlot);
            getClass().getMethod("handle", ServerPlayer.class).invoke(redirected, player);
            player.getInventory().selected = targetSlot;
            player.connection.send(new ClientboundSetCarriedItemPacket(targetSlot));
            callback.cancel();
        } catch (ReflectiveOperationException exception) {
            // Prefer a cancelled toolbox operation over a replacement that can
            // violate a lock after an incompatible Create update.
            LOGGER.error("Could not redirect Create Toolbox equip safely; operation was cancelled", exception);
            callback.cancel();
        }
    }
}
