package com.mamechin.enchantmod.altar;

/**
 * Timing of an enchanting at the altar (pure), in ticks from the quill click: the flyer (and the spinning wheel at tier 1) spins up
 * for 3 seconds; then every picked book drains at once, each for one second per enchant level on it; when the last one is done the
 * enchant is rolled and the spin slows down for 2 seconds.
 */
public final class AltarSpin {
    public static final int RAMP_UP = 60, RAMP_DOWN = 40, TICKS_PER_LEVEL = 20;

    private AltarSpin() {}

    /** When a book with {@code levels} enchant levels has finished draining. */
    public static int drainEnd(int levels) {
        return RAMP_UP + TICKS_PER_LEVEL * levels;
    }

    /** The whole enchanting when the longest book has {@code maxLevels} levels. */
    public static int duration(int maxLevels) {
        return drainEnd(maxLevels) + RAMP_DOWN;
    }

    /** The enchant is rolled when the slowing down begins, right after the last book is drained. */
    public static int enchantAt(int duration) {
        return duration - RAMP_DOWN;
    }

    /** Spin speed from 0 to 1 at {@code elapsed} ticks of an enchanting lasting {@code duration}. */
    public static float speed(float elapsed, int duration) {
        if (elapsed <= 0 || elapsed >= duration) return 0;
        return Math.min(1, Math.min(elapsed / RAMP_UP, (duration - elapsed) / RAMP_DOWN));
    }
}
