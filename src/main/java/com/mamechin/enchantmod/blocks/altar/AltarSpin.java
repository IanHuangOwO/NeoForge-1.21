package com.mamechin.enchantmod.blocks.altar;

/**
 * Phases and timing of an enchanting at the altar (pure; author, 2026-11-23: one enchant at a time, each as long as its power takes to
 * flow). {@link #WARMUP}: the altar (and the wheel) spin up, {@code ramp_up_ticks}. {@link #CHARGE}: one enchant at a time, its rolled
 * level times the cost per level of power, drawn from the books it needs, each book taking {@code drain_ticks_per_book}
 * (author, 2026-11-24: 5 s a book); the enchant is fixed onto the item when its power is in, then the next one charges. {@link #COOLDOWN}: the spin slows down,
 * {@code ramp_down_ticks}.
 */
public final class AltarSpin {
    public static final int IDLE = 0, WARMUP = 1, CHARGE = 2, COOLDOWN = 3;

    private AltarSpin() {}

    /** Ticks to drain {@code books} books at {@code ticksPerBook} each, at least one. */
    public static int chargeTicks(int books, int ticksPerBook) {
        return Math.max(1, books * ticksPerBook);
    }

    /** Spin speed from 0 to 1 at {@code ticks} into a phase lasting {@code length}: rising in the warm-up, full while charging, falling after. */
    public static float speed(int phase, float ticks, int length) {
        float f = length <= 0 ? 1 : Math.max(0, Math.min(1, ticks / length));
        return switch (phase) {
            case WARMUP -> f;
            case CHARGE -> 1;
            case COOLDOWN -> 1 - f;
            default -> 0;
        };
    }
}
