package com.mamechin.enchantmod.scribing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class BlueprintRulesTest {
    private static final int W = BlueprintRules.WIDTH;

    private static Blueprint twoRunes() {
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, 0, "berkana");
        return BlueprintRules.place(bp, 3, "uruz");
    }

    @Test
    void placeNeedsAFreeCellInTheGridAndAKnownRune() {
        Blueprint bp = twoRunes();
        assertNotNull(bp);
        assertNull(BlueprintRules.place(bp, 0, "isa"));
        assertNull(BlueprintRules.place(bp, BlueprintRules.CELLS, "isa"));
        assertNull(BlueprintRules.place(bp, -1, "isa"));
        assertNull(BlueprintRules.place(bp, 5, "nonsense"));
    }

    @Test
    void lineMustBeContiguousAndEndAtTheRunes() {
        Blueprint bp = twoRunes();
        assertNotNull(BlueprintRules.addLine(bp, 0, 3, List.of(1, 2)));
        assertNull(BlueprintRules.addLine(bp, 0, 3, List.of(1)));          // gap before the target
        assertNull(BlueprintRules.addLine(bp, 0, 3, List.of(W + 1, 2)));   // jump, and diagonal
        assertNull(BlueprintRules.addLine(bp, 0, 0, List.of()));           // to itself
        assertNull(BlueprintRules.addLine(bp, 0, 7, List.of(1, 2)));       // no rune at 7
    }

    @Test
    void adjacentRunesNeedNoPath() {
        Blueprint bp = BlueprintRules.place(BlueprintRules.place(Blueprint.EMPTY, 0, "isa"), 1, "isa");
        assertNotNull(BlueprintRules.addLine(bp, 0, 1, List.of()));
    }

    @Test
    void onlyOneDirectLinePerPairAndLineCountIsBounded() {
        Blueprint bp = BlueprintRules.place(BlueprintRules.place(Blueprint.EMPTY, 0, "isa"), 1, "isa");
        bp = BlueprintRules.addLine(bp, 0, 1, List.of());
        assertNotNull(bp);
        assertNull(BlueprintRules.addLine(bp, 0, 1, List.of()));
        assertNull(BlueprintRules.addLine(bp, 1, 0, List.of()));
        List<Blueprint.Line> many = java.util.Collections.nCopies(BlueprintRules.CELLS, new Blueprint.Line(0, 1, List.of(5)));
        assertNull(BlueprintRules.addLine(new Blueprint(bp.runes(), many), 0, 1, List.of(W, W + 1)));
    }

    @Test
    void lineCellsAreOccupiedSoLinesNeverCross() {
        Blueprint bp = BlueprintRules.place(Blueprint.EMPTY, W, "isa");
        bp = BlueprintRules.place(bp, W + 3, "isa");
        bp = BlueprintRules.addLine(bp, W, W + 3, List.of(W + 1, W + 2));  // row 1, left to right
        assertNull(BlueprintRules.place(bp, W + 1, "isa"));                // a rune cannot sit on a line
        bp = BlueprintRules.place(bp, 2, "isa");
        bp = BlueprintRules.place(bp, 2 + 2 * W, "isa");
        assertNotNull(bp);
        assertNull(BlueprintRules.addLine(bp, 2, 2 + 2 * W, List.of(W + 2))); // would cross the first line
    }

    @Test
    void erasingARuneRemovesItsLinesAndErasingALineCellRemovesOnlyThatLine() {
        Blueprint bp = BlueprintRules.addLine(twoRunes(), 0, 3, List.of(1, 2));
        Blueprint noLine = BlueprintRules.erase(bp, 2);
        assertEquals(2, noLine.runes().size());
        assertTrue(noLine.lines().isEmpty());
        Blueprint noRune = BlueprintRules.erase(bp, 0);
        assertEquals(1, noRune.runes().size());
        assertTrue(noRune.lines().isEmpty());
        assertNull(BlueprintRules.erase(bp, 50));                          // nothing there
    }

    @Test
    void parallelLinesAreAllowed() {
        Blueprint bp = twoRunes();
        bp = BlueprintRules.addLine(bp, 0, 3, List.of(1, 2));
        bp = BlueprintRules.addLine(bp, 0, 3, List.of(W, W + 1, W + 2, W + 3));
        assertNotNull(bp);
        assertEquals(2, bp.lines().size());
    }
}
