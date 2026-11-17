package com.mamechin.enchantmod.altar;

import java.util.ArrayList;
import java.util.List;

/**
 * The altar's enchant power rules (pure; first numbers, to tune). The altar links the chiseled bookshelves in a cylinder round it;
 * every enchanted book in them is worth the sum over its enchants of level times the enchantment's weight ({@link AltarPowerTable}). An enchanting costs
 * the sum of the highest level each of the cube's enchants can roll, and is paid by draining whole books, one per shelf, the shelves
 * in a random order; they all drain at once during the spin ({@link AltarSpin}).
 */
public final class AltarPower {
    private AltarPower() {}

    /** The scan cylinder of a tier: horizontal radius and the lowest and highest row, relative to the altar. */
    public record Area(int radius, int minY, int maxY) {}

    /** Tier 0: radius 4, the altar's row up to 2 above. Tier 1 (with the spinning wheel): radius 8, 9 rows centred on the wheel's core ({@code coreHeight} above the altar). */
    public static Area area(int tier, int coreHeight) {
        return tier >= 1 ? new Area(8, coreHeight - 4, coreHeight + 4) : new Area(4, 0, 2);
    }

    /** Offsets {dx, dy, dz} inside the cylinder (dx² + dz² ≤ radius²), the altar's own position left out. */
    public static List<int[]> cylinder(Area area) {
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
