package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CubeContentsTest {
    private static Enchant efficiency;

    @BeforeAll
    static void load() {
        TestDefinitions.load();
        efficiency = Definitions.enchants().stream().filter(e -> e.enchantment().equals("minecraft:efficiency")).findFirst().orElseThrow();
    }

    private static EnchantForecast.Result result(Map<Integer, Double> levels) {
        return new EnchantForecast.Result(efficiency, levels, 1 - levels.values().stream().mapToDouble(Double::doubleValue).sum());
    }

    @Test
    void oneResultKeepsItsChances() {
        CubeContents.Entry entry = CubeContents.combine(List.of(result(Map.of(2, 0.6, 3, 0.3)))).entries().get(0);
        assertEquals(2, entry.chances().get(0).level());
        assertEquals(0.6, entry.chances().get(0).chance(), 1e-9);
        assertEquals(0.3, entry.chances().get(1).chance(), 1e-9);
        assertEquals(0.1, entry.failChance(), 1e-9);
    }

    @Test
    void theSameEnchantAddsUp() {
        // A: level 2 at 50%, else fail. B: level 3 at 40%, level 2 at 30%, else fail.
        List<CubeContents.Entry> entries = CubeContents.combine(List.of(result(Map.of(2, 0.5)), result(Map.of(3, 0.4, 2, 0.3)))).entries();
        assertEquals(1, entries.size());
        CubeContents.Entry entry = entries.get(0);
        assertEquals(List.of(2, 3, 4, 5), entry.chances().stream().map(CubeContents.Chance::level).toList());
        assertEquals(0.30, entry.chances().get(0).chance(), 1e-9); // 2: B makes 2 and A fails, or A makes 2 and B fails
        assertEquals(0.20, entry.chances().get(1).chance(), 1e-9); // 3: B makes 3 and A fails
        assertEquals(0.15, entry.chances().get(2).chance(), 1e-9); // 4: both make 2
        assertEquals(0.20, entry.chances().get(3).chance(), 1e-9); // 5: A makes 2 and B makes 3
        assertEquals(0.15, entry.failChance(), 1e-9);              // both fail
    }

    @Test
    void aRollPicksTheLevelOrFails() {
        CubeContents.Entry entry = new CubeContents.Entry("minecraft:efficiency", List.of(new CubeContents.Chance(1, 0.5), new CubeContents.Chance(2, 0.25)));
        assertEquals(1, entry.roll(0.0));
        assertEquals(2, entry.roll(0.6));
        assertEquals(0, entry.roll(0.8));
    }

    @Test
    void aCapMergesTheHigherLevelsIntoIt() {
        CubeContents capped = new CubeContents(List.of(new CubeContents.Entry("minecraft:efficiency",
                List.of(new CubeContents.Chance(2, 0.3), new CubeContents.Chance(5, 0.2), new CubeContents.Chance(7, 0.1))))).capped(5);
        CubeContents.Entry entry = capped.entries().get(0);
        assertEquals(2, entry.chances().size());
        assertEquals(5, entry.chances().get(1).level());
        assertEquals(0.3, entry.chances().get(1).chance(), 1e-9);
        assertEquals(0.4, entry.failChance(), 1e-9);
    }
}
