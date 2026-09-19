package com.radaeli.hotbarlocker;

/**
 * Loader-neutral identity and shared constants.
 *
 * <p>The loader adapters own input and transport; this package owns the rules
 * that must behave identically on every supported loader.
 */
public final class HotbarLocker {
    public static final String MOD_ID = "hotbarlocker";
    public static final int HOTBAR_SIZE = 9;

    private HotbarLocker() {
    }
}
