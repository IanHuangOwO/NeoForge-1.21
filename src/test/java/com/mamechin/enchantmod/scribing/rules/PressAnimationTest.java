package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PressAnimationTest {
    @Test
    void oneStrokeHasNoTurn() {
        assertEquals(PressAnimation.DOWN + PressAnimation.UP, PressAnimation.duration(1));
        assertEquals(0, PressAnimation.at(0, 1).head(), 1e-6);
        assertEquals(1, PressAnimation.at(PressAnimation.DOWN, 1).head(), 1e-6);
        assertEquals(0, PressAnimation.at(2, 1).turn(), 1e-6);
        assertEquals(0, PressAnimation.at(PressAnimation.DOWN + PressAnimation.UP, 1).head(), 1e-6);
    }

    @Test
    void everyStrokeAfterTheFirstTurnsFirst() {
        int n = 3;
        int secondStrokeStart = PressAnimation.DOWN + PressAnimation.UP;
        assertEquals(0, PressAnimation.at(secondStrokeStart, n).turn(), 1e-6);
        assertEquals(30, PressAnimation.at(secondStrokeStart + PressAnimation.TURN / 2.0, n).turn(), 1e-6);
        assertEquals(60, PressAnimation.at(secondStrokeStart + PressAnimation.TURN, n).turn(), 1e-6);
        assertEquals(1, PressAnimation.at(secondStrokeStart + PressAnimation.TURN + PressAnimation.DOWN, n).head(), 1e-6);
        assertEquals(120, PressAnimation.at(PressAnimation.duration(n), n).turn(), 1e-6); // finished: (n - 1) steps
        assertEquals(0, PressAnimation.at(PressAnimation.duration(n), n).head(), 1e-6);
    }
}
