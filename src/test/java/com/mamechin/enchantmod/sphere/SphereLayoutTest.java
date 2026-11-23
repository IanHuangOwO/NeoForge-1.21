package com.mamechin.enchantmod.sphere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SphereLayoutTest {
    private static Set<List<Integer>> keys(List<int[]> offsets) {
        Set<List<Integer>> set = new HashSet<>();
        for (int[] o : offsets) set.add(List.of(o[0], o[1], o[2]));
        return set;
    }

    @Test
    void shellIsHollowAndClosed() {
        Set<List<Integer>> shell = keys(SphereLayout.SHELL);
        assertEquals(SphereLayout.SHELL.size(), shell.size(), "no duplicates");
        assertFalse(shell.contains(List.of(0, 0, 0)), "hollow");
        int r = SphereLayout.SHELL_RADIUS;
        assertFalse(shell.contains(List.of(r - 1, 0, 0)), "hollow inside the radius");
        assertTrue(shell.contains(List.of(r, 0, 0)) && shell.contains(List.of(0, -r, 0)) && shell.contains(List.of(0, 0, r)));
        // closed: a flood fill from the core through face neighbours never gets past the shell
        Set<List<Integer>> seen = new HashSet<>(List.of(List.of(0, 0, 0)));
        java.util.ArrayDeque<List<Integer>> queue = new java.util.ArrayDeque<>(seen);
        int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!queue.isEmpty()) {
            List<Integer> c = queue.poll();
            assertTrue(c.get(0) * c.get(0) + c.get(1) * c.get(1) + c.get(2) * c.get(2) < r * r, "leaked to " + c);
            for (int[] f : faces) {
                List<Integer> n = List.of(c.get(0) + f[0], c.get(1) + f[1], c.get(2) + f[2]);
                if (!shell.contains(n) && seen.add(n)) queue.add(n);
            }
        }
    }

    @Test
    void ringsAreThreeCirclesOutsideTheShell() {
        Set<List<Integer>> rings = keys(SphereLayout.RINGS), shell = keys(SphereLayout.SHELL);
        assertEquals(SphereLayout.RINGS.size(), rings.size(), "shared cells listed once");
        for (int[] o : SphereLayout.RINGS) {
            assertTrue(o[0] == 0 || o[1] == 0 || o[2] == 0, "in an axis plane");
            assertFalse(shell.contains(List.of(o[0], o[1], o[2])), "outside the shell");
        }
        for (List<Integer> axis : List.of(List.of(SphereLayout.RING_RADIUS, 0, 0), List.of(0, SphereLayout.RING_RADIUS, 0), List.of(0, 0, -SphereLayout.RING_RADIUS))) assertTrue(rings.contains(axis));
        int perRing = (int) SphereLayout.RINGS.stream().filter(o -> o[2] == 0).count();
        assertEquals(3 * perRing - 6, SphereLayout.RINGS.size(), "three equal rings sharing the 6 axis cells");
    }
}
