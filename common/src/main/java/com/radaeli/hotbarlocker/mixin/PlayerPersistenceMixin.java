package com.radaeli.hotbarlocker.mixin;

import com.radaeli.hotbarlocker.HotbarLockService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerPersistenceMixin {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void hotbarlocker$saveLocks(CompoundTag tag, CallbackInfo callback) {
        tag.putInt(HotbarLockService.NBT_KEY, HotbarLockService.mask((Player) (Object) this));
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void hotbarlocker$loadLocks(CompoundTag tag, CallbackInfo callback) {
        HotbarLockService.setMask((Player) (Object) this, tag.getInt(HotbarLockService.NBT_KEY));
    }
}
