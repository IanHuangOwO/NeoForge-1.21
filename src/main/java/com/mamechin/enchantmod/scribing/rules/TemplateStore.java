package com.mamechin.enchantmod.scribing.rules;

/** How many slots a handbook's template store has. It grows in steps of nine. */
public final class TemplateStore {
    public static final int STEP = 9, BASE = 9;

    private TemplateStore() {}

    /** The slot count for a capacity: at least the base, a multiple of nine, and never fewer than the slots in use (so a store that shrinks loses nothing). */
    public static int slots(int capacity, int used) {
        int wanted = Math.max(Math.max(capacity, used), BASE);
        return (wanted + STEP - 1) / STEP * STEP;
    }
}
