package com.mamechin.enchantmod.altar;

import com.mamechin.enchantmod.balance.Balance;

/**
 * Timing of an enchanting at the altar (pure), in ticks from the quill click, from {@code altar.json} ({@link Balance.Altar}; shipped:
 * 3 s up, 1 s per level, 2 s down): the flyer (and the spinning wheel at tier 1) spins up; then every picked book drains at once,
 * each for a time per enchant level on it; when the last one is done the enchant is rolled and the spin slows down.
 */
public final class AltarSpin {
    private AltarSpin() {}

    private static Balance.Altar settings() {
        return Balance.current().altar();
    }

    /** When a book with {@code levels} enchant levels has finished draining. */
    public static int drainEnd(int levels) {
        return settings().rampUp() + settings().drainTicksPerLevel() * levels;
    }

    /** The whole enchanting when the longest book has {@code maxLevels} levels. */
    public static int duration(int maxLevels) {
        return drainEnd(maxLevels) + settings().rampDown();
    }

    /** The enchant is rolled when the slowing down begins, right after the last book is drained. */
    public static int enchantAt(int duration) {
        return duration - settings().rampDown();
    }

    public static int rampUp() {
        return settings().rampUp();
    }

    public static int rampDown() {
        return settings().rampDown();
    }

    /** Spin speed from 0 to 1 at {@code elapsed} ticks of an enchanting lasting {@code duration}. */
    public static float speed(float elapsed, int duration) {
        if (elapsed <= 0 || elapsed >= duration) return 0;
        return Math.min(1, Math.min(elapsed / settings().rampUp(), (duration - elapsed) / settings().rampDown()));
    }
}
