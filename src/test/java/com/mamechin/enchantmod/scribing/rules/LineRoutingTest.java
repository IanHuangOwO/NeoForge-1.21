package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class LineRoutingTest {
    @org.junit.jupiter.api.BeforeAll
    static void definitions() {
        com.mamechin.enchantmod.scribing.rules.TestDefinitions.load();
    }

    private static final int W = BlueprintRules.WIDTH;

    private static int c(int x, int y) {
        return y * W + x;
    }

    @Test
    void aLineLeavesTheRuneOnTheSideFacingTheTargetAndWalksToIt() {
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, c(1, 1), "berkana"); // covers x 0..2, y 0..2
        List<Integer> path = new ArrayList<>();
        assertTrue(LineRouting.extend(bp, c(1, 1), path, c(6, 1)));
        assertEquals(List.of(c(3, 1), c(4, 1), c(5, 1), c(6, 1)), path);
    }

    @Test
    void movingTheTargetBackTakesTheLineBack() {
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, c(1, 1), "berkana");
        List<Integer> path = new ArrayList<>();
        LineRouting.extend(bp, c(1, 1), path, c(6, 1));
        LineRouting.extend(bp, c(1, 1), path, c(4, 1));
        assertEquals(List.of(c(3, 1), c(4, 1)), path);
    }

    @Test
    void aLineStopsAtAnotherRune() {
        Blueprint bp = BlueprintRules.place(BlueprintRules.place(Blueprint.EMPTY, c(1, 1), "berkana"), c(5, 1), "isa"); // x 4..6 is the second rune
        List<Integer> path = new ArrayList<>();
        LineRouting.extend(bp, c(1, 1), path, c(5, 1));
        assertEquals(List.of(c(3, 1)), path);
    }

    @Test
    void noRuneAtTheStartMeansNothingToExtend() {
        assertFalse(LineRouting.extend(Blueprint.EMPTY, c(1, 1), new ArrayList<>(), c(5, 1)));
    }

    @Test
    void adjacentMeansOneStepAcrossOrDown() {
        assertTrue(LineRouting.adjacent(c(2, 2), c(3, 2)));
        assertFalse(LineRouting.adjacent(c(2, 2), c(3, 3)));
    }

    @Test
    void aLineStopsAtTheEdgeOfTheTemplateArea() {
        TemplateLimits area = new TemplateLimits("x:area", 9, 7, Integer.MAX_VALUE); // columns 3..11
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, c(4, 3), "berkana", area);
        List<Integer> path = new ArrayList<>();
        LineRouting.extend(bp, c(4, 3), path, c(14, 3), area);
        assertEquals(List.of(c(6, 3), c(7, 3), c(8, 3), c(9, 3), c(10, 3), c(11, 3)), path);
    }
}
