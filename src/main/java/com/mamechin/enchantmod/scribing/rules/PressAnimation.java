package com.mamechin.enchantmod.scribing.rules;

/**
 * The timeline of one pressing, in ticks, with no Minecraft code. One stroke is the head going down and up; before every stroke
 * after the first, the turntable turns one step (60 degrees, so the cube meets the next template). The number of strokes is the
 * number of templates pressed.
 */
public final class PressAnimation {
    public static final int TURN = 8, DOWN = 4, UP = 6, STROKE = TURN + DOWN + UP;
    public static final float STEP_DEGREES = 60;

    /** head: 0 raised to 1 fully down. turn: degrees the turntable has turned since the start. spin: degrees of the side gears. */
    public record Pose(float head, float turn, float spin) {}

    private PressAnimation() {}

    /** Ticks from the start until the last stroke is finished. */
    public static int duration(int strokes) {
        return strokes * STROKE - TURN;
    }

    public static Pose at(double tick, int strokes) {
        if (strokes <= 0) return new Pose(0, 0, 0);
        if (tick >= duration(strokes)) return new Pose(0, STEP_DEGREES * (strokes - 1), 0);
        double t = Math.max(0, tick) + TURN; // the first stroke has no turn, so it starts where a turn would end
        int k = (int) (t / STROKE);
        double u = t - k * STROKE;
        float turn = STEP_DEGREES * Math.max(0, k - 1), head = 0;
        if (u < TURN) {
            if (k > 0) turn += (float) (STEP_DEGREES * smooth(u / TURN));
        } else {
            if (k > 0) turn += STEP_DEGREES;
            double s = u - TURN;
            head = s < DOWN ? (float) Math.pow(s / DOWN, 2) : (float) (1 - smooth((s - DOWN) / UP));
        }
        float spin = 45 * (k + (float) Math.max(0, Math.min(1, (u - TURN) / (STROKE - TURN)))); // 45 degrees looks the same as 0
        return new Pose(head, turn, spin);
    }

    private static double smooth(double p) {
        return p * p * (3 - 2 * p);
    }
}
