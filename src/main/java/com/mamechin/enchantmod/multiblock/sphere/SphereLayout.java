package com.mamechin.enchantmod.multiblock.sphere;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Where the parts of the glass sphere sit, as offsets {dx, dy, dz} from its core (pure, no Minecraft): a hollow shell of glass at
 * radius {@link #SHELL_RADIUS} and three rings of waxed copper at radius {@link #RING_RADIUS}, one in each axis plane. A cell is on
 * radius r when its distance from the core rounds to r (r - 0.5 < d ≤ r + 0.5). The rings lie outside the shell; they cross each other
 * on the axes, where the shared cell is listed once.
 */
public final class SphereLayout {
    public static final int SHELL_RADIUS = 4, RING_RADIUS = 5;
    public static final List<int[]> SHELL = shell(), RINGS = rings();

    private SphereLayout() {}

    static boolean onRadius(int d2, int r) {
        return d2 > (r - 0.5) * (r - 0.5) && d2 <= (r + 0.5) * (r + 0.5);
    }

    private static List<int[]> shell() {
        int r = SHELL_RADIUS;
        List<int[]> list = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dy = -r; dy <= r; dy++)
                for (int dz = -r; dz <= r; dz++)
                    if (onRadius(dx * dx + dy * dy + dz * dz, r)) list.add(new int[] {dx, dy, dz});
        return List.copyOf(list);
    }

    private static List<int[]> rings() {
        int r = RING_RADIUS;
        List<int[]> list = new ArrayList<>();
        Set<List<Integer>> seen = new HashSet<>();
        for (int a = -r; a <= r; a++) {
            for (int b = -r; b <= r; b++) {
                if (!onRadius(a * a + b * b, r)) continue;
                for (int[] o : new int[][] {{a, b, 0}, {a, 0, b}, {0, a, b}}) {
                    if (seen.add(List.of(o[0], o[1], o[2]))) list.add(o);
                }
            }
        }
        return List.copyOf(list);
    }
}
