package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class PressAnimationTest {
    @Test
    void oneSecondPerTemplate() {
        assertEquals(20, PressAnimation.duration(1));
        assertEquals(120, PressAnimation.duration(6));
    }

    @Test
    void oneStrokeNeverRolls() {
        assertEquals(1, PressAnimation.at(PressAnimation.DOWN, 1).head(), 1e-6);
        for (int t = 0; t < 20; t++) assertNull(PressAnimation.at(t, 1).rolling());
    }

    @Test
    void theHeadIsUpBeforeTheCubeMoves() {
        for (double t = 0; t < PressAnimation.duration(3); t += 0.25) {
            PressAnimation.Pose p = PressAnimation.at(t, 3);
            if (p.lift() > 0) assertEquals(0, p.head(), 1e-6);
        }
    }

    @Test
    void theCubeRollsBetweenStrokes() {
        int n = 3, rollStart = PressAnimation.DOWN + PressAnimation.UP + PressAnimation.LIFT;
        PressAnimation.Pose mid = PressAnimation.at(rollStart + PressAnimation.ROLL / 2.0, n);
        assertEquals('X', mid.rolling().axis());
        assertEquals(45, mid.angle(), 1e-6);
        assertEquals(1, mid.lift(), 1e-6);
        assertEquals(1, PressAnimation.at(PressAnimation.STROKE, n).done());
        assertEquals(0, PressAnimation.at(PressAnimation.STROKE, n).lift(), 1e-6);
        assertEquals(2, PressAnimation.at(2 * PressAnimation.STROKE + 19, n).done()); // the last stroke has no roll after it
    }

    @Test
    void sixFacesNeedFiveRolls() {
        assertEquals(5, PressAnimation.ROLLS.size());
        assertEquals(180, PressAnimation.ROLLS.get(4).degrees());
    }
}
