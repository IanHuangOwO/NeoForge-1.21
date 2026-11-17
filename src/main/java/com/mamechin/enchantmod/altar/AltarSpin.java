package com.mamechin.enchantmod.altar;

/**
 * Timing of an enchanting at the altar (pure): the flyer, or the spinning wheel at tier 2, spins up, holds, the enchant is rolled,
 * then it slows down. Ticks counted from the quill click.
 */
public final class AltarSpin {
    public static final int DURATION = 200, RAMP = 40;
    /** The enchant is rolled when the slowing down begins. */
    public static final int ENCHANT_AT = DURATION - RAMP;

    private AltarSpin() {}

    /** Spin speed from 0 to 1 at {@code elapsed} ticks: up over the first ramp, full, down over the last ramp. */
    public static float speed(float elapsed) {
        if (elapsed <= 0 || elapsed >= DURATION) return 0;
        return Math.min(1, Math.min(elapsed, DURATION - elapsed) / RAMP);
    }
}
