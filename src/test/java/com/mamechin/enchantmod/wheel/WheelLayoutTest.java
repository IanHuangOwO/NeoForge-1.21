package com.mamechin.enchantmod.wheel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class WheelLayoutTest {
    @Test
    void ringInThePlaneAcrossTheFacing() {
        for (boolean facingX : new boolean[] {false, true}) {
            List<int[]> bodies = WheelLayout.bodies(facingX);
            Set<List<Integer>> distinct = new HashSet<>();
            for (int[] o : bodies) {
                distinct.add(List.of(o[0], o[1], o[2]));
                assertTrue(WheelLayout.isBody(o[0], o[1], o[2], facingX));
                assertTrue(WheelLayout.isBody(-o[0], -o[1], -o[2], facingX), "symmetric");
                assertEquals(0, facingX ? o[0] : o[2], "flat across the facing");
            }
            assertEquals(24, distinct.size());
            assertFalse(distinct.contains(List.of(0, 0, 0)));
        }
        assertTrue(WheelLayout.isBody(0, -3, 0, false), "bottom of the ring");
        assertTrue(WheelLayout.isBody(3, 1, 0, false), "side of the ring");
        assertTrue(WheelLayout.isBody(2, 0, 0, false), "beam");
        assertFalse(WheelLayout.isBody(3, 3, 0, false), "cut corner");
        assertFalse(WheelLayout.isBody(1, 1, 0, false), "between beams");
        assertFalse(WheelLayout.isBody(1, 0, 0, true), "off the plane");
    }

    @Test
    void pillarsStandBesideTheRingUpToTheCore() {
        List<int[]> pillars = WheelLayout.pillars(false);
        assertEquals(8, pillars.size());
        for (int[] o : pillars) {
            assertEquals(4, Math.abs(o[0]));
            assertTrue(o[1] >= -3 && o[1] <= 0);
            assertFalse(WheelLayout.isBody(o[0], o[1], o[2], false));
        }
        assertEquals(0, WheelLayout.pillars(true).get(0)[0], "along Z when facing east or west");
    }
}
