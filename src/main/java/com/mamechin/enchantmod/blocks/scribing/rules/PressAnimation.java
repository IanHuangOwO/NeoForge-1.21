package com.mamechin.enchantmod.blocks.scribing.rules;

import java.util.List;

/**
 * The timeline of one pressing, in ticks, with no Minecraft code. Each template takes one stroke of one second: the head goes
 * down and up; then, after every stroke but the last, the cube lifts 2 px, rolls a quarter turn about a horizontal axis (so a new
 * face is on top) and settles. Six templates use the six faces: X, X, X, then Z, then a half turn about Z.
 */
public final class PressAnimation {
    public static final int DOWN = 5, UP = 5, LIFT = 2, ROLL = 6, SETTLE = 2, STROKE = DOWN + UP + LIFT + ROLL + SETTLE; // 20 ticks, one second

    /** A turn of the cube about a world axis ('X' or 'Z') through its centre. */
    public record Roll(char axis, int degrees) {}

    public static final List<Roll> ROLLS = List.of(new Roll('X', 90), new Roll('X', 90), new Roll('X', 90), new Roll('Z', 90), new Roll('Z', 180));

    /**
     * head: 0 raised to 1 fully down. lift: 0 resting to 1 up 2 px. done: rolls finished, the first {@code done} of ROLLS.
     * rolling: the roll in progress, or null, and angle how far it has turned in degrees.
     */
    public record Pose(float head, float lift, int done, Roll rolling, float angle) {
        public static final Pose REST = new Pose(0, 0, 0, null, 0);
    }

    private PressAnimation() {}

    /** Ticks from the start until the pressing is over. */
    public static int duration(int strokes) {
        return strokes * STROKE;
    }

    public static Pose at(double tick, int strokes) {
        if (strokes <= 0 || tick >= duration(strokes)) return Pose.REST;
        double t = Math.max(0, tick);
        int k = (int) (t / STROKE);
        double u = t - k * STROKE;
        if (u < DOWN) return new Pose((float) Math.pow(u / DOWN, 2), 0, k, null, 0);
        if (u < DOWN + UP) return new Pose((float) (1 - smooth((u - DOWN) / UP)), 0, k, null, 0);
        if (k >= strokes - 1) return new Pose(0, 0, k, null, 0); // the last stroke just settles, no roll after it
        u -= DOWN + UP;
        Roll roll = ROLLS.get(k);
        if (u < LIFT) return new Pose(0, (float) (u / LIFT), k, null, 0);
        u -= LIFT;
        if (u < ROLL) return new Pose(0, 1, k, roll, (float) (roll.degrees() * smooth(u / ROLL)));
        u -= ROLL;
        return new Pose(0, (float) (1 - u / SETTLE), k + 1, null, 0);
    }

    private static double smooth(double p) {
        return p * p * (3 - 2 * p);
    }
}
