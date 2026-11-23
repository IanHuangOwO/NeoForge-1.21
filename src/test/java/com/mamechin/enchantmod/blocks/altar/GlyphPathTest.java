package com.mamechin.enchantmod.blocks.altar;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class GlyphPathTest {
    @Test
    void wandersButStartsAndEndsOnTheLine() {
        Random random = new Random(7);
        double[] start = {3, 70, -5}, end = {0.5, 66, 2.5};
        double maxSway = 0;
        for (int n = 0; n < 200; n++) {
            double[] r = new double[6];
            for (int i = 0; i < 6; i++) r[i] = random.nextDouble();
            GlyphPath path = GlyphPath.of(start, end, r);
            assertArrayEquals(start, path.at(0), 1e-9);
            assertArrayEquals(end, path.at(1), 1e-9);
            double[] mid = path.at(0.5), straight = {(start[0] + end[0]) / 2, (start[1] + end[1]) / 2, (start[2] + end[2]) / 2};
            double sway = Math.sqrt(Math.pow(mid[0] - straight[0], 2) + Math.pow(mid[1] - straight[1], 2) + Math.pow(mid[2] - straight[2], 2));
            assertTrue(sway <= 1.2 * Math.sqrt(2) + 1e-9, "sway stays within the cap");
            maxSway = Math.max(maxSway, sway);
        }
        assertTrue(maxSway > 0.3, "paths really wander");
        assertArrayEquals(start, GlyphPath.of(start, start, new double[6]).at(0.5), 1e-9, "no distance: no sway");
    }
}
