package com.radaeli.hotbarlocker;

/** Pure destination selector used by integrations and unit tests. */
public final class ReplacementPlanner {
    private ReplacementPlanner() { }

    /** @return first unlocked slot in circular order, or {@code -1} when none exists. */
    public static int firstUnlocked(int lockMask, int selectedSlot, boolean[] emptySlots, boolean[] replaceableSlots) {
        validate(selectedSlot, emptySlots, replaceableSlots);
        for (int offset = 0; offset < HotbarLocker.HOTBAR_SIZE; offset++) {
            int slot = (selectedSlot + offset) % HotbarLocker.HOTBAR_SIZE;
            if ((lockMask & (1 << slot)) == 0 && emptySlots[slot]) return slot;
        }
        for (int offset = 0; offset < HotbarLocker.HOTBAR_SIZE; offset++) {
            int slot = (selectedSlot + offset) % HotbarLocker.HOTBAR_SIZE;
            if ((lockMask & (1 << slot)) == 0 && replaceableSlots[slot]) return slot;
        }
        return -1;
    }
    private static void validate(int selectedSlot, boolean[] emptySlots, boolean[] replaceableSlots) {
        if (selectedSlot < 0 || selectedSlot >= HotbarLocker.HOTBAR_SIZE || emptySlots.length != HotbarLocker.HOTBAR_SIZE || replaceableSlots.length != HotbarLocker.HOTBAR_SIZE) {
            throw new IllegalArgumentException("Expected exactly nine hotbar slots");
        }
    }
}
