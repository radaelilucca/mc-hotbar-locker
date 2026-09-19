package com.radaeli.hotbarlocker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HotbarLockStateTest {
    @Test void stateMasksInvalidBitsAndTogglesOnlyTheRequestedSlot() {
        HotbarLockState state = new HotbarLockState();
        state.setMask(-1);
        assertEquals(0x1ff, state.mask());
        state.toggle(4);
        assertFalse(state.isLocked(4));
        assertTrue(state.isLocked(3));
        assertThrows(IllegalArgumentException.class, () -> state.toggle(9));
    }

    @Test void plannerPrefersEmptyUnlockedSlotsInCircularOrder() {
        boolean[] empty = {false, false, false, true, true, false, false, false, false};
        boolean[] replaceable = {true, true, true, true, true, true, true, true, true};
        assertEquals(4, ReplacementPlanner.firstUnlocked(1 << 3, 3, empty, replaceable));
    }

    @Test void plannerFallsBackToReplaceableAndReportsNoDestinationWhenAllLocked() {
        boolean[] empty = new boolean[9];
        boolean[] replaceable = {false, false, false, false, false, true, false, false, false};
        assertEquals(5, ReplacementPlanner.firstUnlocked(0, 2, empty, replaceable));
        assertEquals(-1, ReplacementPlanner.firstUnlocked(0x1ff, 2, empty, replaceable));
    }
}
