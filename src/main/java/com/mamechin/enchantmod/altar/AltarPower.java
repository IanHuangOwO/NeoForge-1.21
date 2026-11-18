package com.mamechin.enchantmod.altar;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.balance.Balance;

/**
 * The altar's enchant power rules (pure; the numbers are in {@code altar.json}, {@link Balance.Altar}). The altar links the chiseled
 * bookshelves in its tier's cylinder; every enchanted book in them is worth the sum over its enchants of level times the
 * enchantment's weight. An enchanting costs the sum of the highest level each of the cube's enchants can roll, times the cost per
 * level, and is paid by draining whole books (a set number per shelf, the shelves in a random order) all at once during the spin.
 */
public final class AltarPower {
    private AltarPower() {}

    /** Offsets {dx, dy, dz} inside the cylinder (dx² + dz² ≤ radius²), the altar's own position left out. */
    public static List<int[]> cylinder(Balance.Area area) {
        List<int[]> list = new ArrayList<>();
        int r = area.radius();
        for (int dy = area.minY(); dy <= area.maxY(); dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r && (dx != 0 || dy != 0 || dz != 0)) list.add(new int[] {dx, dy, dz});
                }
            }
        }
        return list;
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
}
