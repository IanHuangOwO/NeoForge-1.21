package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BlueprintRulesTest {
    @org.junit.jupiter.api.BeforeAll
    static void definitions() {
        com.mamechin.enchantmod.scribing.rules.TestDefinitions.load();
    }

    private static final int W = BlueprintRules.WIDTH;

    private static int c(int x, int y) {
        return y * W + x;
    }

    /** Two core runes (3x3) with a one cell gap between them: centres (1,1) and (5,1). */
    private static Blueprint twoRunes() {
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, c(1, 1), "berkana");
        return BlueprintRules.place(bp, c(5, 1), "isa");
    }

    @Test
    void coreRunesCoverThreeByThreeStabilizersTwoByTwoAndTheBoostRuneOneCell() {
        assertEquals(9, BlueprintRules.footprint(c(1, 1), 3).size());
        assertEquals(java.util.Set.of(c(1, 1), c(2, 1), c(1, 2), c(2, 2)), BlueprintRules.footprint(c(1, 1), 2));
        assertNull(BlueprintRules.footprint(c(W - 1, 0), 2));                         // 2x2 would leave the grid
        assertEquals(3, Rune.byId("berkana").size());
        assertEquals(1, Rune.byId("uruz").size());
        assertEquals(2, Rune.byId("mannaz").size());
        assertEquals(2, Rune.byId("dagaz").size());
    }

    @Test
    void placeNeedsAFreeFootprintInsideTheGridAndAKnownRune() {
        Blueprint bp = twoRunes();
        assertNotNull(bp);
        assertNull(BlueprintRules.place(Blueprint.EMPTY, c(0, 0), "isa"));          // square would leave the grid
        assertNotNull(BlueprintRules.place(Blueprint.EMPTY, c(0, 0), "uruz"));      // one cell fits in the corner
        assertNull(BlueprintRules.place(bp, c(1, 1), "uruz"));
        assertNull(BlueprintRules.place(bp, BlueprintRules.CELLS, "uruz"));
        assertNull(BlueprintRules.place(bp, -1, "uruz"));
        assertNull(BlueprintRules.place(bp, c(9, 5), "nonsense"));
    }

    @Test
    void runesNeverTouchNotEvenAtACorner() {
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, c(1, 1), "berkana");
        assertNull(BlueprintRules.place(bp, c(4, 1), "isa"));    // side by side
        assertNull(BlueprintRules.place(bp, c(4, 4), "isa"));    // corner to corner
        assertNull(BlueprintRules.place(bp, c(3, 1), "uruz"));   // a small rune next to it
        assertNotNull(BlueprintRules.place(bp, c(5, 1), "isa"));
        assertNotNull(BlueprintRules.place(bp, c(4, 1), "uruz")); // one gap cell is enough
    }

    @Test
    void lineMustStartAndEndNextToTheRunesAndBeContiguous() {
        Blueprint bp = twoRunes();
        assertNotNull(BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(3, 1))));
        assertNull(BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of()));                 // runes never touch
        assertNull(BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(2, 1))));          // inside a rune
        assertNull(BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(3, 3))));          // not next to the runes
        assertNull(BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(3, 0), c(4, 1)))); // jump through a rune
        assertNull(BlueprintRules.addLine(bp, c(1, 1), c(1, 1), List.of(c(3, 1))));          // to itself
        assertNull(BlueprintRules.addLine(bp, c(1, 1), c(9, 9), List.of(c(3, 1))));          // no rune there
        Blueprint far = BlueprintRules.place(bp, c(9, 1), "isa");
        assertNull(BlueprintRules.addLine(far, c(1, 1), c(9, 1), List.of(c(3, 1), c(7, 1)))); // gap in the path
    }

    @Test
    void lineCountIsBounded() {
        Blueprint bp = twoRunes();
        List<Blueprint.Line> many = java.util.Collections.nCopies(BlueprintRules.CELLS, new Blueprint.Line(c(1, 1), c(5, 1), List.of(c(3, 2))));
        assertNull(BlueprintRules.addLine(new Blueprint(bp.runes(), many), c(1, 1), c(5, 1), List.of(c(3, 1))));
    }

    @Test
    void lineCellsAreOccupiedSoLinesNeverCross() {
        Blueprint bp = BlueprintRules.addLine(twoRunes(), c(1, 1), c(5, 1), List.of(c(3, 1)));
        assertNotNull(bp);
        assertNull(BlueprintRules.place(bp, c(3, 1), "uruz"));                    // a rune cannot sit on a line
        bp = BlueprintRules.place(bp, c(3, 5), "uruz");
        assertNotNull(bp);
        assertNull(BlueprintRules.addLine(bp, c(3, 5), c(1, 1), List.of(c(3, 4), c(3, 3), c(3, 2), c(3, 1)))); // would cross
    }

    @Test
    void erasingAnyCellOfARuneRemovesItAndItsLinesAndAnyLineCellRemovesOnlyThatLine() {
        Blueprint bp = BlueprintRules.addLine(twoRunes(), c(1, 1), c(5, 1), List.of(c(3, 1)));
        Blueprint noLine = BlueprintRules.erase(bp, c(3, 1));
        assertEquals(2, noLine.runes().size());
        assertTrue(noLine.lines().isEmpty());
        Blueprint noRune = BlueprintRules.erase(bp, c(0, 2));                     // a corner cell of the left rune
        assertEquals(1, noRune.runes().size());
        assertTrue(noRune.lines().isEmpty());
        assertNull(BlueprintRules.erase(bp, c(10, 8)));                           // nothing there
    }

    @Test
    void everyCellDrawnCostsOneInkAndErasingIsFree() {
        Blueprint empty = Blueprint.EMPTY;
        Blueprint core = BlueprintRules.place(empty, c(1, 1), "berkana");
        assertEquals(9, BlueprintRules.cost(empty, core));                                   // a core rune
        Blueprint pair = BlueprintRules.place(core, c(5, 1), "isa");
        assertEquals(9, BlueprintRules.cost(core, pair));
        assertEquals(4, BlueprintRules.cost(empty, BlueprintRules.place(empty, c(1, 1), "mannaz")));  // a stabilizer
        Blueprint line = BlueprintRules.addLine(pair, c(1, 1), c(5, 1), List.of(c(3, 1)));
        assertEquals(1, BlueprintRules.cost(pair, line));                                    // a line costs its length
        assertEquals(0, BlueprintRules.cost(line, BlueprintRules.erase(line, c(3, 1))));
    }

    @Test
    void parallelLinesAreAllowed() {
        Blueprint bp = twoRunes();
        bp = BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(3, 1)));
        bp = BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(3, 0)));
        bp = BlueprintRules.addLine(bp, c(1, 1), c(5, 1), List.of(c(3, 2)));
        assertNotNull(bp);
        assertEquals(3, bp.lines().size());
    }

    @Test
    void aTemplateOnlyAllowsItsDrawingArea() {
        TemplateTier small = new TemplateTier("x:small", 9, 7, Integer.MAX_VALUE); // columns 3..11, rows 2..8
        assertTrue(small.contains(c(3, 2)));
        assertFalse(small.contains(c(2, 2)));
        assertFalse(small.contains(c(3, 9)));
        assertNotNull(BlueprintRules.place(Blueprint.EMPTY, c(4, 3), "berkana", small));     // covers 3..5 x 2..4: just inside
        assertNull(BlueprintRules.place(Blueprint.EMPTY, c(3, 3), "berkana", small));        // covers column 2
        assertNotNull(BlueprintRules.place(Blueprint.EMPTY, c(3, 3), "berkana"));            // the old call allows the whole grid
        Blueprint bp = BlueprintRules.place(BlueprintRules.place(Blueprint.EMPTY, c(4, 3), "berkana", small), c(8, 3), "isa", small);
        assertNotNull(BlueprintRules.addLine(bp, c(4, 3), c(8, 3), List.of(c(6, 3)), small));
        Blueprint edge = BlueprintRules.place(BlueprintRules.place(Blueprint.EMPTY, c(4, 3), "berkana"), c(4, 7), "isa"); // 4,7 covers rows 6..8
        assertNull(BlueprintRules.addLine(edge, c(4, 3), c(4, 7), List.of(c(4, 5)), new TemplateTier("x:narrow", 3, 11, Integer.MAX_VALUE) /* columns 6..8 only */));
    }
}
