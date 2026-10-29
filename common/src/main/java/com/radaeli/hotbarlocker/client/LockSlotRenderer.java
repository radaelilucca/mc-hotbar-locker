package com.radaeli.hotbarlocker.client;

import com.radaeli.hotbarlocker.HotbarLockService;
import com.radaeli.hotbarlocker.HotbarLocker;
import java.util.Arrays;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Draws locked hotbar slots for both 1.20.1 and 1.21.1 using only drawing calls
 * that exist in both versions. Slot coordinates are the 20x20 slot origin;
 * the item icon occupies columns and rows 2..17 and is never painted over by
 * the frame or tint.
 */
public final class LockSlotRenderer {
    // Lock timeline in milliseconds; unlocking plays it backwards.
    private static final long DURATION = 520L;
    private static final float FRAME_END = 380F;
    private static final float BADGE_START = 360F;
    private static final long SELECTION_FADE = 120L;

    // One arm of the frame: half of the bottom edge, one side, half of the top.
    private static final int ARM_LENGTH = 9 + 17 + 8;

    // Vanilla selection sprite, drawn two pixels above/left of the slot origin.
    private static final int SELECTION_WIDTH = 24;
    private static final int SELECTION_HEIGHT = 23;
    // Covers the sprite's outline plus its three light rows/columns.
    private static final int SELECTION_BAND = 5;
    // Multiplies the greenish light-gray vanilla sprite (0xDAEDD5) into roughly
    // the frame's gold (0xDABE55) while keeping its shading.
    private static final float SELECTION_TINT_R = 1F;
    private static final float SELECTION_TINT_G = 0.80F;
    private static final float SELECTION_TINT_B = 0.40F;

    private static final int FRAME_GOLD = 0xF0D9A33D;
    private static final int CORNER_GOLD = 0xFFF0C45A;
    private static final int GLOW_GOLD = 0xFFFFE9A8;
    private static final int TINT_GOLD = 0x30D9A33D;

    private static final int OUTLINE = 0xFF3A2306;
    private static final int SHACKLE = 0xFFC3C8CF;
    private static final int BODY_LIGHT = 0xFFF6D27A;
    private static final int BODY = 0xFFE0A93A;
    private static final int BODY_SHADE = 0xFFB07A22;

    // 7x8 padlock; rows 0..2 are the shackle, which drops into place on lock.
    private static final String[] PADLOCK = {
            ".ooooo.",
            ".o###o.",
            ".o#o#o.",
            "ooooooo",
            "o+++++o",
            "o==o==o",
            "o-----o",
            "ooooooo"
    };
    private static final int SHACKLE_ROWS = 3;
    private static final int BADGE_X = -1;
    private static final int BADGE_Y = -3;

    private static Player trackedPlayer;
    private static int lastMask;
    private static final long[] changedAt = new long[HotbarLocker.HOTBAR_SIZE];
    private static float selectionGold;
    private static boolean selectionTracing;
    private static long lastSelectionUpdate;

    private LockSlotRenderer() {}

    /** Frame and tint; call after the hotbar background and before the items. */
    public static void renderBackground(GuiGraphics graphics, Player player, int left, int top) {
        long now = Util.getMillis();
        track(player, now);
        for (int slot = 0; slot < HotbarLocker.HOTBAR_SIZE; slot++) {
            float time = timelineTime(slot, now);
            if (time <= 0F) continue;
            boolean locking = isLocked(slot);
            drawFrame(graphics, left + slot * 20, top, time, locking);
        }
    }

    /** Padlock badges; call after the items so the badge sits on top. */
    public static void renderBadges(GuiGraphics graphics, Player player, int left, int top) {
        long now = Util.getMillis();
        track(player, now);
        for (int slot = 0; slot < HotbarLocker.HOTBAR_SIZE; slot++) {
            float time = timelineTime(slot, now);
            if (time <= BADGE_START) continue;
            drawBadge(graphics, left + slot * 20, top, (time - BADGE_START) / (DURATION - BADGE_START));
        }
    }

    private static void track(Player player, long now) {
        int mask = HotbarLockService.state(player).mask();
        if (player != trackedPlayer) {
            // Logging in or respawning shows existing locks without replaying them.
            trackedPlayer = player;
            lastMask = mask;
            Arrays.fill(changedAt, Long.MIN_VALUE);
            // Lets the selection color jump straight to its target.
            lastSelectionUpdate = 0L;
            selectionTracing = false;
            return;
        }
        int changed = mask ^ lastMask;
        if (changed == 0) return;
        for (int slot = 0; slot < HotbarLocker.HOTBAR_SIZE; slot++) {
            if ((changed & 1 << slot) == 0) continue;
            long elapsed = elapsed(slot, now);
            // Toggling mid-animation reverses from the current position.
            changedAt[slot] = now - (DURATION - elapsed);
        }
        lastMask = mask;
    }

    private static boolean isLocked(int slot) {
        return (lastMask & 1 << slot) != 0;
    }

    private static long elapsed(int slot, long now) {
        if (changedAt[slot] == Long.MIN_VALUE) return DURATION;
        return Math.min(DURATION, Math.max(0L, now - changedAt[slot]));
    }

    /** Position on the lock timeline; 0 is unlocked, DURATION is fully locked. */
    private static float timelineTime(int slot, long now) {
        long elapsed = elapsed(slot, now);
        return isLocked(slot) ? elapsed : DURATION - elapsed;
    }

    private static void drawFrame(GuiGraphics graphics, int x, int y, float time, boolean locking) {
        float progress = easeInOutCubic(clamp01(time / FRAME_END));
        graphics.fill(x + 2, y + 2, x + 18, y + 18, scaleAlpha(TINT_GOLD, progress));

        int color = FRAME_GOLD;
        if (locking && time > FRAME_END) {
            // Brief glow once the frame closes, settling back to the base gold.
            float glow = 1F - clamp01((time - FRAME_END) / (DURATION - FRAME_END));
            color = lerpColor(FRAME_GOLD, GLOW_GOLD, glow * glow);
        }

        if (progress >= 1F) {
            graphics.fill(x + 1, y + 1, x + 19, y + 2, color);
            graphics.fill(x + 1, y + 18, x + 19, y + 19, color);
            graphics.fill(x + 1, y + 2, x + 2, y + 18, color);
            graphics.fill(x + 18, y + 2, x + 19, y + 18, color);
            int corner = color == FRAME_GOLD ? CORNER_GOLD : color;
            graphics.fill(x + 1, y + 1, x + 2, y + 2, corner);
            graphics.fill(x + 18, y + 1, x + 19, y + 2, corner);
            graphics.fill(x + 1, y + 18, x + 2, y + 19, corner);
            graphics.fill(x + 18, y + 18, x + 19, y + 19, corner);
            return;
        }

        // Two mirrored arms trace the 1px ring around the icon: out from the
        // bottom center, up both sides, then closing across the top.
        float length = progress * ARM_LENGTH;
        for (int step = 0; step < ARM_LENGTH; step++) {
            float coverage = clamp01(length - step);
            if (coverage <= 0F) break;
            int px;
            int py;
            if (step < 9) { px = 9 - step; py = 18; }
            else if (step < 26) { px = 1; py = 17 - (step - 9); }
            else { px = 2 + (step - 26); py = 1; }
            int pixel = scaleAlpha(color, coverage);
            graphics.fill(x + px, y + py, x + px + 1, y + py + 1, pixel);
            graphics.fill(x + 19 - px, y + py, x + 20 - px, y + py + 1, pixel);
        }
    }

    /** Multiplies the vanilla selection sprite; call right before vanilla draws it. */
    public static void beginSelection(GuiGraphics graphics, Player player) {
        long now = Util.getMillis();
        track(player, now);
        int slot = player.getInventory().selected;
        boolean locked = isLocked(slot);
        boolean tracing = locked && timelineTime(slot, now) < FRAME_END;
        if (tracing) {
            // The trace paints gold over a vanilla-colored sprite.
            selectionGold = 0F;
        } else if (selectionTracing && locked) {
            // The trace just covered the whole sprite; hand over without a flash.
            selectionGold = 1F;
        } else {
            float step = (now - lastSelectionUpdate) / (float) SELECTION_FADE;
            float target = locked ? 1F : 0F;
            selectionGold = target > selectionGold
                    ? Math.min(target, selectionGold + step)
                    : Math.max(target, selectionGold - step);
        }
        selectionTracing = tracing;
        lastSelectionUpdate = now;
        setTint(graphics, selectionGold);
    }

    /**
     * Restores the shader color after vanilla's selection sprite, then, while
     * the selected slot is locking, redraws that sprite in gold clipped to the
     * part of the frame path traced so far.
     */
    public static void endSelection(GuiGraphics graphics, Player player, int left, int top, SelectionSprite sprite) {
        setTint(graphics, 0F);
        if (!selectionTracing) return;
        int slot = player.getInventory().selected;
        float progress = easeInOutCubic(clamp01(timelineTime(slot, Util.getMillis()) / FRAME_END));
        int x = left + slot * 20 - 2;
        int y = top - 2;
        int half = SELECTION_WIDTH / 2;
        float length = progress * (half + SELECTION_HEIGHT + half);
        int bottom = Math.round(Math.min(length, half));
        int side = Math.round(clamp(length - half, SELECTION_HEIGHT));
        int across = Math.round(clamp(length - half - SELECTION_HEIGHT, half));

        setTint(graphics, 1F);
        clipped(graphics, sprite, x, y, half - bottom, SELECTION_HEIGHT - SELECTION_BAND, half + bottom, SELECTION_HEIGHT);
        clipped(graphics, sprite, x, y, 0, SELECTION_HEIGHT - side, SELECTION_BAND, SELECTION_HEIGHT);
        clipped(graphics, sprite, x, y, SELECTION_WIDTH - SELECTION_BAND, SELECTION_HEIGHT - side, SELECTION_WIDTH, SELECTION_HEIGHT);
        clipped(graphics, sprite, x, y, 0, 0, across, SELECTION_BAND);
        clipped(graphics, sprite, x, y, SELECTION_WIDTH - across, 0, SELECTION_WIDTH, SELECTION_BAND);
        setTint(graphics, 0F);
    }

    /** Draws the version-specific vanilla selection sprite at a GUI position. */
    @FunctionalInterface
    public interface SelectionSprite {
        void draw(int x, int y);
    }

    private static void clipped(GuiGraphics graphics, SelectionSprite sprite, int x, int y,
                                int fromX, int fromY, int toX, int toY) {
        if (toX <= fromX || toY <= fromY) return;
        graphics.enableScissor(x + fromX, y + fromY, x + toX, y + toY);
        sprite.draw(x, y);
        graphics.disableScissor();
    }

    private static void setTint(GuiGraphics graphics, float gold) {
        graphics.setColor(
                1F + (SELECTION_TINT_R - 1F) * gold,
                1F + (SELECTION_TINT_G - 1F) * gold,
                1F + (SELECTION_TINT_B - 1F) * gold,
                1F);
    }

    private static void drawBadge(GuiGraphics graphics, int x, int y, float progress) {
        float alpha = easeOutCubic(progress);
        float scale = 0.6F + 0.4F * easeOutBack(progress);
        // The shackle starts raised and drops into the body at the end: the "click".
        float shackleLift = 2F * (1F - easeInQuad(clamp01((progress - 0.4F) / 0.6F)));

        var pose = graphics.pose();
        pose.pushPose();
        // Above item models (z 150) and their decorations (z 200).
        pose.translate(x + BADGE_X + 3.5F, y + BADGE_Y + 4F, 250F);
        pose.scale(scale, scale, 1F);
        pose.translate(-3.5F, -4F, 0F);
        for (int row = 0; row < PADLOCK.length; row++) {
            if (row == SHACKLE_ROWS) {
                pose.translate(0F, shackleLift, 0F);
            } else if (row == 0) {
                pose.translate(0F, -shackleLift, 0F);
            }
            String line = PADLOCK[row];
            for (int column = 0; column < line.length(); column++) {
                int color = switch (line.charAt(column)) {
                    case 'o' -> OUTLINE;
                    case '#' -> SHACKLE;
                    case '+' -> BODY_LIGHT;
                    case '=' -> BODY;
                    case '-' -> BODY_SHADE;
                    default -> 0;
                };
                if (color != 0) graphics.fill(column, row, column + 1, row + 1, scaleAlpha(color, alpha));
            }
        }
        pose.popPose();
    }

    private static int scaleAlpha(int color, float factor) {
        int alpha = Math.round((color >>> 24) * clamp01(factor));
        return alpha << 24 | color & 0xFFFFFF;
    }

    private static int lerpColor(int from, int to, float t) {
        int result = 0;
        for (int shift = 0; shift < 32; shift += 8) {
            int a = from >>> shift & 0xFF;
            int b = to >>> shift & 0xFF;
            result |= Math.round(a + (b - a) * t) << shift;
        }
        return result;
    }

    private static float clamp(float value, float max) {
        return Math.max(0F, Math.min(max, value));
    }

    private static float clamp01(float value) {
        return Math.max(0F, Math.min(1F, value));
    }

    private static float easeInOutCubic(float t) {
        return t < 0.5F ? 4F * t * t * t : 1F - (float) Math.pow(-2F * t + 2F, 3) / 2F;
    }

    private static float easeOutCubic(float t) {
        float inverse = 1F - t;
        return 1F - inverse * inverse * inverse;
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158F;
        float c3 = c1 + 1F;
        float shifted = t - 1F;
        return 1F + c3 * shifted * shifted * shifted + c1 * shifted * shifted;
    }

    private static float easeInQuad(float t) {
        return t * t;
    }
}
