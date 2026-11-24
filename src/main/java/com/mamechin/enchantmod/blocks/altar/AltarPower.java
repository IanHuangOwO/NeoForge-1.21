package com.mamechin.enchantmod.blocks.altar;

import com.mamechin.enchantmod.balance.Balance;

/**
 * The altar's enchant power rules (pure; the numbers are in {@code altar.json}, {@link Balance.Altar}). The altar links the chiseled
 * bookshelves in its tier's cylinder; every enchanted book in them is worth the sum over its enchants of level times the
 * enchantment's weight. An enchanting costs the sum of the highest level each of the cube's enchants can roll, times the cost per
 * level, and is paid by draining whole books (a set number per shelf, the shelves in a random order) all at once during the spin.
 */
public final class AltarPower {
    private AltarPower() {}

    /** True when the offset (dx, dy, dz) from the altar is inside the area's cylinder (dx² + dz² ≤ radius²); the altar itself is not. */
    public static boolean inArea(Balance.Area area, int dx, int dy, int dz) {
        int r = area.radius();
        return dx * dx + dz * dz <= r * r && dy >= area.minY() && dy <= area.maxY() && (dx != 0 || dy != 0 || dz != 0);
    }

    /** How many books from the front of {@code powers} (already in the random order) pay {@code cost}; -1 when they all fall short. */
    public static int booksNeeded(int[] powers, int cost) {
        int sum = 0;
        for (int i = 0; i < powers.length; i++) {
            if (sum >= cost) return i;
            sum += powers[i];
        }
        return sum >= cost ? powers.length : -1;
    }

    /**
     * How much each book gives to an enchant costing {@code cost}, taking the books' {@code remaining} power in order until it is paid
     * (author, 2026-11-23: a book only gives what is needed; its leftover stays for the next enchant). Null when they all fall short.
     */
    public static int[] allot(int[] remaining, int cost) {
        int[] taken = new int[remaining.length];
        int left = cost;
        for (int i = 0; i < remaining.length && left > 0; i++) {
            taken[i] = Math.min(Math.max(0, remaining[i]), left);
            left -= taken[i];
        }
        return left > 0 ? null : taken;
    }
}
