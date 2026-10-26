package com.radaeli.hotbarlocker.mixin.compat.create;

import com.radaeli.hotbarlocker.HotbarLockerApi;
import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.nbt.NbtUtils;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.concurrent.CompletableFuture;

/** Create 0.5.1 / 6 on Forge 1.20.1 dispatches Toolbox mutations via Context.enqueueWork. */
@Pseudo
@Mixin(targets = "com.simibubi.create.content.equipment.toolbox.ToolboxEquipPacket", remap = false)
abstract class ToolboxEquipPacket1201Mixin {
    @Shadow private BlockPos toolboxPos;
    @Shadow private int hotbarSlot;
    @Shadow private int slot;

    @Redirect(method = "handle", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/network/NetworkEvent$Context;enqueueWork(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;"), remap = false)
    private CompletableFuture<Void> hotbarlocker$guardToolboxOnServerThread(NetworkEvent.Context context, Runnable operation) {
        return context.enqueueWork(() -> {
            var player = context.getSender();
            if (player == null || hotbarSlot < 0 || hotbarSlot >= 9) return;
            if (!HotbarLockService.state(player).isLocked(hotbarSlot)) {
                operation.run();
                return;
            }
            // An unequip belongs to its original slot; never redirect it to a different binding.
            if (toolboxPos == null) return;
            var destination = HotbarLockerApi.findReplacementSlot(player, hotbarSlot);
            if (destination.isEmpty()) return;
            hotbarSlot = destination.getAsInt();
            operation.run();
            var binding = player.getPersistentData().getCompound("CreateToolboxData").getCompound(Integer.toString(hotbarSlot));
            // Native Create may reject equip for range, missing block or invalid compartment.
            if (binding.contains("Pos") && binding.getInt("Slot") == slot &&
                    NbtUtils.readBlockPos(binding.getCompound("Pos")).equals(toolboxPos)) {
                player.getInventory().selected = hotbarSlot;
                player.connection.send(new ClientboundSetCarriedItemPacket(hotbarSlot));
            }
        });
    }
}
