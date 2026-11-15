package com.mamechin.enchantmod.scribing.rules;

/**
 * The six template slots of the printing press sit in a ring round the cube, numbered clockwise from the top (0 top, 1 upper
 * right, 2 lower right, 3 bottom, 4 lower left, 5 upper left). A cube opens slots spread evenly round the ring.
 */
public final class PressLayout {
    public static final int RING = 6;

    private PressLayout() {}

    /** True when ring slot {@code slot} is open for a cube that takes {@code capacity} templates: 1 opens the top, 3 the top and the two lower ones, 6 all. */
    public static boolean isOpen(int slot, int capacity) {
        if (capacity <= 0) return false;
        if (capacity >= RING) return true;
        return RING % capacity == 0 ? slot % (RING / capacity) == 0 : slot < capacity; // an uneven capacity fills from the top
    }
}
