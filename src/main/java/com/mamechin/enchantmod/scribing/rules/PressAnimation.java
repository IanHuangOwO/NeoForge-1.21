package com.mamechin.enchantmod.scribing.rules;

/**
 * The timeline of one pressing, in ticks, with no Minecraft code. Each template takes one stroke of one second: the head goes
 * down and up, then (after every stroke but the last) the turntable turns one step of 60 degrees to the next template. The
 * number of strokes is the number of templates pressed.
 */
public final class PressAnimation {
    public static final int DOWN = 6, UP = 6, TURN = 8, STROKE = DOWN + UP + TURN; // 20 ticks, one second
    public static final float STEP_DEGREES = 60;

    /** head: 0 raised to 1 fully down. turn: degrees the turntable has turned since the start. spin: degrees of the side gears. */
    public record Pose(float head, float turn, float spin) {}

    private PressAnimation() {}

    /** Ticks from the start until the pressing is over. */
    public static int duration(int strokes) {
        return strokes * STROKE;
    }

    public static Pose at(double tick, int strokes) {
        if (strokes <= 0) return new Pose(0, 0, 0);
        if (tick >= duration(strokes)) return new Pose(0, STEP_DEGREES * (strokes - 1), 0);
        double t = Math.max(0, tick);
        int k = (int) (t / STROKE);
        double u = t - k * STROKE;
        float turn = STEP_DEGREES * k, head = 0;
        if (u < DOWN) head = (float) Math.pow(u / DOWN, 2);
        else if (u < DOWN + UP) head = (float) (1 - smooth((u - DOWN) / UP));
        else if (k < strokes - 1) turn += (float) (STEP_DEGREES * smooth((u - DOWN - UP) / TURN)); // the last stroke just settles
        float spin = 45 * (k + (float) Math.min(1, u / (DOWN + UP))); // 45 degrees looks the same as 0
        return new Pose(head, turn, spin);
    }

    private static double smooth(double p) {
        return p * p * (3 - 2 * p);
    }
}
