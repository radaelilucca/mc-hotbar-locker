package com.radaeli.hotbarlocker;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** State access and the vanilla-compatible hotbar destination policy. */
public final class HotbarLockService {
    public static final String NBT_KEY = "HotbarLockerLocks";
    private static final Map<UUID, HotbarLockState> STATES = new ConcurrentHashMap<>();

    private HotbarLockService() { }

    public static HotbarLockState state(Player player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new HotbarLockState());
    }

    public static void setMask(Player player, int mask) { state(player).setMask(mask); }
    public static int mask(Player player) { return state(player).mask(); }
    public static boolean allLocked(Player player) { return mask(player) == (1 << HotbarLocker.HOTBAR_SIZE) - 1; }

    /** Returns an empty main-inventory slot only; the hotbar is intentionally excluded. */
    public static int firstEmptyMainInventorySlot(Inventory inventory) {
        for (int slot = HotbarLocker.HOTBAR_SIZE; slot < inventory.items.size(); slot++) {
            if (inventory.getItem(slot).isEmpty()) return slot;
        }
        return -1;
    }

    /** Handles creative pick when every hotbar slot is protected. */
    public static boolean placePickedItemInMainInventory(Inventory inventory, ItemStack stack) {
        int destination = firstEmptyMainInventorySlot(inventory);
        if (destination < 0) return false;
        inventory.setItem(destination, stack.copy());
        return true;
    }

    /**
     * Mirrors vanilla's empty-first / replaceable-second selection but omits
     * protected slots. It deliberately does not affect manual selection.
     */
    public static int suitableHotbarSlot(Inventory inventory, int vanillaFallback) {
        HotbarLockState state = state(inventory.player);
        int selected = inventory.selected;
        for (int offset = 0; offset < HotbarLocker.HOTBAR_SIZE; offset++) {
            int slot = (selected + offset) % HotbarLocker.HOTBAR_SIZE;
            if (!state.isLocked(slot) && inventory.getItem(slot).isEmpty()) return slot;
        }
        // The name of vanilla's "not replaceable by pick action" helper differs
        // between the Fabric and official-mapping toolchains. The operation is
        // still safe when it uses the first unlocked occupied slot: vanilla will
        // move that stack to the main inventory before replacing it.
        for (int offset = 0; offset < HotbarLocker.HOTBAR_SIZE; offset++) {
            int slot = (selected + offset) % HotbarLocker.HOTBAR_SIZE;
            if (!state.isLocked(slot)) return slot;
        }
        return vanillaFallback;
    }
}
