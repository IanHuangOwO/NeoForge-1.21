package com.mamechin.enchantmod.multiblock.sphere;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BoltPathTest {
    private static final float[] FROM = {0, 0, 0}, TO = {10, 3, -4};

    @Test
    void mainBoltRunsFromStartToTarget() {
        List<float[]> main = BoltPath.build(FROM, TO, 42).get(0);
        assertArrayEquals(FROM, main.get(0));
        assertArrayEquals(TO, main.get(main.size() - 1));
        assertEquals(33, main.size(), "five rounds of splitting: 2^5 + 1 points");
    }

    @Test
    void staysNearTheStraightLine() {
        float len = BoltPath.distance(FROM, TO);
        for (List<float[]> line : BoltPath.build(FROM, TO, 7)) {
            for (float[] p : line) {
                // distance from the line through FROM and TO
                float[] d = BoltPath.sub(p, FROM), axis = BoltPath.sub(TO, FROM);
                float t = (d[0] * axis[0] + d[1] * axis[1] + d[2] * axis[2]) / (len * len);
                float[] off = {d[0] - t * axis[0], d[1] - t * axis[1], d[2] - t * axis[2]};
                assertTrue(BoltPath.length(off) < len * 0.75F, "a point strays too far: " + BoltPath.length(off));
            }
        }
    }

    @Test
    void sameSeedSameBoltAndBranchesForkFromTheMainBolt() {
        List<List<float[]>> a = BoltPath.build(FROM, TO, 99), b = BoltPath.build(FROM, TO, 99);
        assertEquals(a.size(), b.size());
        assertTrue(a.size() >= 3 && a.size() <= 4, "two or three branches");
        for (int i = 0; i < a.size(); i++) for (int j = 0; j < a.get(i).size(); j++) assertArrayEquals(a.get(i).get(j), b.get(i).get(j));
        for (List<float[]> branch : a.subList(1, a.size())) {
            assertTrue(a.get(0).stream().anyMatch(p -> java.util.Arrays.equals(p, branch.get(0))), "a branch starts on the main bolt");
        }
    }
}
