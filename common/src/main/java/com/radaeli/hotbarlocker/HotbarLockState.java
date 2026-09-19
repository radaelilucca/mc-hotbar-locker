package com.radaeli.hotbarlocker;

/** Compact, serializable lock state for the nine hotbar slots. */
public final class HotbarLockState {
    private static final int VALID_BITS = (1 << HotbarLocker.HOTBAR_SIZE) - 1;
    private int mask;

    public int mask() { return mask; }
    public void setMask(int mask) { this.mask = mask & VALID_BITS; }
    public boolean isLocked(int slot) { checkSlot(slot); return (mask & (1 << slot)) != 0; }
    public boolean toggle(int slot) { checkSlot(slot); mask ^= 1 << slot; return isLocked(slot); }
    public void setLocked(int slot, boolean locked) {
        checkSlot(slot);
        mask = locked ? mask | 1 << slot : mask & ~(1 << slot);
    }

    private static void checkSlot(int slot) {
        if (slot < 0 || slot >= HotbarLocker.HOTBAR_SIZE) throw new IllegalArgumentException("Invalid hotbar slot: " + slot);
    }
}
