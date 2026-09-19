package com.radaeli.hotbarlocker;

import java.util.OptionalInt;

import net.minecraft.world.entity.player.Player;

/** Public, loader-neutral entry point for mods that replace a hotbar item. */
public final class HotbarLockerApi {
    private HotbarLockerApi() { }

    /**
     * Finds a safe replacement destination beginning at {@code requestedSlot}.
     * Empty unlocked slots are always preferred; an occupied unlocked slot is
     * only used when no eligible empty slot exists. An empty result means every
     * hotbar slot is protected.
     */
    public static OptionalInt findReplacementSlot(Player player, int requestedSlot) {
        if (requestedSlot < 0 || requestedSlot >= HotbarLocker.HOTBAR_SIZE) return OptionalInt.empty();
        HotbarLockState locks = HotbarLockService.state(player);
        for (int offset = 0; offset < HotbarLocker.HOTBAR_SIZE; offset++) {
            int slot = (requestedSlot + offset) % HotbarLocker.HOTBAR_SIZE;
            if (!locks.isLocked(slot) && player.getInventory().getItem(slot).isEmpty()) return OptionalInt.of(slot);
        }
        for (int offset = 0; offset < HotbarLocker.HOTBAR_SIZE; offset++) {
            int slot = (requestedSlot + offset) % HotbarLocker.HOTBAR_SIZE;
            if (!locks.isLocked(slot)) return OptionalInt.of(slot);
        }
        return OptionalInt.empty();
    }
}
