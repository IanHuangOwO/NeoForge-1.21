package com.mamechin.enchantmod.particle;

/**
 * The wandering path of one drain glyph (pure): from {@code start} towards {@code end}, swaying sideways on two random waves across
 * the line. The sway is scaled by sin(pi t), so it is zero at both ends: every glyph leaves its shelf and lands exactly on its target,
 * however much it wandered on the way. Progress eases in and out.
 */
public record GlyphPath(double[] start, double[] end, double[] u, double[] v, double ampU, double ampV, double freqU, double freqV,
        double phaseU, double phaseV) {

    /**
     * A path with its sway set by {@code random} values in [0, 1): the amplitude grows with the distance (0.15 + 0.12 per block,
     * at most 1.2 blocks, times 0.5 to 1), each wave makes 1 to 3 swings, at a random phase.
     */
    public static GlyphPath of(double[] start, double[] end, double[] random) {
        double[] dir = {end[0] - start[0], end[1] - start[1], end[2] - start[2]};
        double len = Math.sqrt(dot(dir, dir));
        if (len < 1e-6) return new GlyphPath(start, end, new double[3], new double[3], 0, 0, 0, 0, 0, 0);
        double[] d = scale(dir, 1 / len);
        double[] helper = Math.abs(d[1]) < 0.9 ? new double[] {0, 1, 0} : new double[] {1, 0, 0};
        double[] u = normalize(cross(d, helper)), v = cross(d, u);
        double amp = Math.min(1.2, 0.15 + 0.12 * len);
        return new GlyphPath(start, end, u, v,
                amp * (0.5 + 0.5 * random[0]), amp * (0.5 + 0.5 * random[1]),
                1 + 2 * random[2], 1 + 2 * random[3],
                2 * Math.PI * random[4], 2 * Math.PI * random[5]);
    }

    /** The position at {@code t} from 0 (start) to 1 (end). */
    public double[] at(double t) {
        t = Math.max(0, Math.min(1, t));
        double p = t * t * (3 - 2 * t);
        double envelope = Math.sin(Math.PI * t);
        double su = envelope * ampU * Math.sin(2 * Math.PI * freqU * t + phaseU);
        double sv = envelope * ampV * Math.sin(2 * Math.PI * freqV * t + phaseV);
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) out[i] = start[i] + (end[i] - start[i]) * p + u[i] * su + v[i] * sv;
        return out;
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] scale(double[] a, double s) {
        return new double[] {a[0] * s, a[1] * s, a[2] * s};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] a) {
        return scale(a, 1 / Math.sqrt(dot(a, a)));
    }
}
