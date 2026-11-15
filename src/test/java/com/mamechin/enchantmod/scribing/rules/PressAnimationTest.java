package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PressAnimationTest {
    @Test
    void oneSecondPerTemplate() {
        assertEquals(20, PressAnimation.duration(1));
        assertEquals(120, PressAnimation.duration(6));
    }

    @Test
    void oneStrokeDoesNotTurn() {
        assertEquals(0, PressAnimation.at(0, 1).head(), 1e-6);
        assertEquals(1, PressAnimation.at(PressAnimation.DOWN, 1).head(), 1e-6);
        assertEquals(0, PressAnimation.at(PressAnimation.DOWN + PressAnimation.UP, 1).head(), 1e-6);
        assertEquals(0, PressAnimation.at(19, 1).turn(), 1e-6);
        assertEquals(0, PressAnimation.at(20, 1).turn(), 1e-6);
    }

    @Test
    void theTurntableTurnsBetweenStrokes() {
        int n = 3, turnStart = PressAnimation.DOWN + PressAnimation.UP;
        assertEquals(0, PressAnimation.at(turnStart, n).turn(), 1e-6);
        assertEquals(30, PressAnimation.at(turnStart + PressAnimation.TURN / 2.0, n).turn(), 1e-6);
        assertEquals(60, PressAnimation.at(PressAnimation.STROKE, n).turn(), 1e-6);
        assertEquals(1, PressAnimation.at(PressAnimation.STROKE + PressAnimation.DOWN, n).head(), 1e-6); // second stroke
        assertEquals(120, PressAnimation.at(2 * PressAnimation.STROKE + 19, n).turn(), 1e-6);           // last stroke does not turn after
        assertEquals(120, PressAnimation.at(PressAnimation.duration(n), n).turn(), 1e-6);
    }
}
