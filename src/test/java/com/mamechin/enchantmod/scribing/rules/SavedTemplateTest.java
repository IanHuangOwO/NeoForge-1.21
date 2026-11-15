package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mamechin.enchantmod.scribing.rules.Blueprint.RuneAt;

class SavedTemplateTest {
    @BeforeAll
    static void load() {
        TestDefinitions.load();
    }

    private static final SavedTemplate EFFICIENCY = new SavedTemplate("My Pick", new Blueprint(List.of(new RuneAt(1, "berkana")), List.of()));

    @Test
    void anEmptyQueryMatchesEverything() {
        assertTrue(EFFICIENCY.matches("  ", Enchant::nameKey));
    }

    @Test
    void theQueryMatchesTheNameIgnoringCase() {
        assertTrue(EFFICIENCY.matches("my p", Enchant::nameKey));
        assertFalse(EFFICIENCY.matches("sword", Enchant::nameKey));
    }

    @Test
    void theQueryMatchesTheNameOfAnEnchantItMakes() {
        assertTrue(EFFICIENCY.matches("efficiency", e -> "Efficiency"));
        assertFalse(EFFICIENCY.matches("unbreaking", e -> "Efficiency"));
    }
}
