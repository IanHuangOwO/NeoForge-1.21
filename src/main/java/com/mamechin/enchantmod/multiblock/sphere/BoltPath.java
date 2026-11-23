package com.mamechin.enchantmod.multiblock.sphere;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The shape of a refill lightning bolt (pure, no Minecraft): a jagged main bolt from {@code from} to {@code to} made by midpoint
 * displacement (each segment is split and its middle pushed sideways, half as far each round), and two or three shorter branches that
 * fork off it towards the target. The same seed gives the same bolt, so every player sees one shape; the client changes the seed every
 * two ticks to make it flicker.
 */
public final class BoltPath {
    /** Main bolt: rounds of splitting, and the first sideways push as a share of its length. */
    static final int MAIN_ROUNDS = 5, BRANCH_ROUNDS = 3;
    static final float MAIN_JITTER = 0.18F, BRANCH_JITTER = 0.2F;

    private BoltPath() {}

    /** Polylines of points {x, y, z}: the main bolt first (from {@code from} to {@code to}), then the branches. */
    public static List<List<float[]>> build(float[] from, float[] to, long seed) {
        Random random = new Random(seed);
        List<List<float[]>> lines = new ArrayList<>();
        List<float[]> main = displace(from, to, random, distance(from, to) * MAIN_JITTER, MAIN_ROUNDS);
        lines.add(main);
        int branches = 2 + random.nextInt(2);
        for (int b = 0; b < branches; b++) {
            float[] start = main.get(main.size() / 5 + random.nextInt(Math.max(1, main.size() * 3 / 5)));
            float[] dir = sub(to, start);
            float len = length(dir);
            if (len < 1e-4F) continue;
            for (int i = 0; i < 3; i++) dir[i] = dir[i] / len + (random.nextFloat() * 2 - 1) * 0.8F;
            float dirLen = length(dir), branchLen = len * (0.25F + random.nextFloat() * 0.2F);
            float[] end = new float[3];
            for (int i = 0; i < 3; i++) end[i] = start[i] + dir[i] / dirLen * branchLen;
            lines.add(displace(start, end, random, branchLen * BRANCH_JITTER, BRANCH_ROUNDS));
        }
        return lines;
    }

    private static List<float[]> displace(float[] from, float[] to, Random random, float jitter, int rounds) {
        float[] axis = sub(to, from);
        float axisLen = Math.max(length(axis), 1e-6F);
        List<float[]> points = new ArrayList<>(List.of(from.clone(), to.clone()));
        for (int round = 0; round < rounds; round++, jitter *= 0.5F) {
            List<float[]> next = new ArrayList<>();
            for (int i = 0; i + 1 < points.size(); i++) {
                float[] p = points.get(i), q = points.get(i + 1);
                float[] push = {random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1, random.nextFloat() * 2 - 1};
                float along = (push[0] * axis[0] + push[1] * axis[1] + push[2] * axis[2]) / (axisLen * axisLen);
                next.add(p);
                next.add(new float[] {(p[0] + q[0]) / 2 + (push[0] - along * axis[0]) * jitter, (p[1] + q[1]) / 2 + (push[1] - along * axis[1]) * jitter,
                        (p[2] + q[2]) / 2 + (push[2] - along * axis[2]) * jitter});
            }
            next.add(points.get(points.size() - 1));
            points = next;
        }
        return points;
    }

    static float[] sub(float[] a, float[] b) {
        return new float[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static float length(float[] v) {
        return (float) Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    static float distance(float[] a, float[] b) {
        return length(sub(a, b));
    }
}
