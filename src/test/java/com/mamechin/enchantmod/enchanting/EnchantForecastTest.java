package com.mamechin.enchantmod.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mamechin.enchantmod.enchanting.Blueprint.Line;
import com.mamechin.enchantmod.enchanting.Blueprint.RuneAt;

class EnchantForecastTest {
    @org.junit.jupiter.api.BeforeAll
    static void definitions() {
        com.mamechin.enchantmod.enchanting.TestDefinitions.load();
    }

    private static final double S = 1 - 0.05; // survival of one line with a one cell path: 4% + 1%

    private static Line line(int a, int b) {
        return new Line(a, b, List.of(1000 + a * 10 + b)); // path cells are only counted, not checked
    }

    private static double total(EnchantForecast.Result r) {
        return r.levels().values().stream().mapToDouble(Double::doubleValue).sum();
    }

    @Test
    void aSingleCoreRuneMakesItsEnchantAtLevelOne() {
        List<EnchantForecast.Result> results = EnchantForecast.forecast(new Blueprint(List.of(new RuneAt(1, "berkana")), List.of()));
        assertEquals(1, results.size());
        assertEquals("minecraft:efficiency", results.get(0).enchant().enchantment());
        assertEquals(1.0, results.get(0).levels().get(1), 1e-9);
        assertEquals(0.0, results.get(0).broken(), 1e-9);
    }

    @Test
    void aTwoRuneCoreNeedsItsLinkAndParallelLinesHelp() {
        Blueprint one = new Blueprint(List.of(new RuneAt(1, "tiwaz"), new RuneAt(2, "sowilo")), List.of(line(1, 2)));
        EnchantForecast.Result smite = EnchantForecast.forecast(one).get(0);
        assertEquals("minecraft:smite", smite.enchant().enchantment());
        assertEquals(S, smite.levels().get(1), 1e-9);
        assertEquals(1 - S, smite.broken(), 1e-9);

        Blueprint two = new Blueprint(one.runes(), List.of(line(1, 2), new Line(1, 2, List.of(7, 8))));
        EnchantForecast.Result both = EnchantForecast.forecast(two).get(0);
        assertEquals(1 - 0.05 * 0.06, both.levels().get(1), 1e-9); // the second line is two cells: 6%
    }

    @Test
    void groupsThatMatchNoRowMakeNothing() {
        assertTrue(EnchantForecast.forecast(new Blueprint(List.of(new RuneAt(1, "berkana"), new RuneAt(2, "isa")), List.of(line(1, 2)))).isEmpty());
        assertTrue(EnchantForecast.forecast(new Blueprint(List.of(new RuneAt(1, "berkana"), new RuneAt(2, "berkana")), List.of(line(1, 2)))).isEmpty());
        assertTrue(EnchantForecast.forecast(Blueprint.EMPTY).isEmpty());
        assertTrue(EnchantForecast.forecast(new Blueprint(List.of(new RuneAt(1, "uruz")), List.of())).isEmpty());
    }

    @Test
    void eachBoostRuneAddsALevelWhenItsLinkHolds() {
        Blueprint bp = new Blueprint(List.of(new RuneAt(1, "berkana"), new RuneAt(2, "uruz"), new RuneAt(3, "uruz")), List.of(line(1, 2), line(1, 3)));
        EnchantForecast.Result r = EnchantForecast.forecast(bp).get(0);
        assertEquals(S * S, r.levels().get(3), 1e-9);
        assertEquals(2 * S * (1 - S), r.levels().get(2), 1e-9);
        assertEquals((1 - S) * (1 - S), r.levels().get(1), 1e-9);
        assertEquals(1.0, total(r), 1e-9);
    }

    @Test
    void stabilizersLiftTheLevelCap() {
        List<RuneAt> runes = new ArrayList<>(List.of(new RuneAt(1, "berkana")));
        List<Line> lines = new ArrayList<>();
        for (int i = 0; i < 6; i++) { // six boost runes, each joined by three lines so the link almost never breaks
            runes.add(new RuneAt(10 + i, "uruz"));
            for (int j = 0; j < 3; j++) lines.add(new Line(1, 10 + i, List.of(500 + i * 3 + j)));
        }
        EnchantForecast.Result bare = EnchantForecast.forecast(new Blueprint(runes, lines)).get(0);
        assertEquals(4, bare.levels().keySet().stream().mapToInt(Integer::intValue).max().getAsInt()); // no Mannaz: stops at 4

        runes.add(new RuneAt(30, "mannaz"));
        for (int j = 0; j < 3; j++) lines.add(new Line(1, 30, List.of(600 + j)));
        EnchantForecast.Result stable = EnchantForecast.forecast(new Blueprint(runes, lines)).get(0);
        assertEquals(7, stable.levels().keySet().stream().mapToInt(Integer::intValue).max().getAsInt()); // 1 + 6
        assertEquals(1.0, total(stable), 1e-9);
        assertEquals(4, EnchantForecast.levelCap(java.util.Map.of("enchantmod:mannaz", 0, "enchantmod:dagaz", 3)));
        assertEquals(9, EnchantForecast.levelCap(java.util.Map.of("enchantmod:mannaz", 1, "enchantmod:dagaz", 0)));
        assertEquals(19, EnchantForecast.levelCap(java.util.Map.of("enchantmod:mannaz", 1, "enchantmod:dagaz", 1)));
        assertEquals(29, EnchantForecast.levelCap(java.util.Map.of("enchantmod:mannaz", 2, "enchantmod:dagaz", 2)));
    }
}
