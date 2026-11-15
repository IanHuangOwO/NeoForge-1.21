package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mamechin.enchantmod.scribing.rules.Blueprint.RuneAt;

class TemplateSearchTest {
    @BeforeAll
    static void load() {
        TestDefinitions.load();
    }

    private static final Blueprint EFFICIENCY = new Blueprint(List.of(new RuneAt(1, "berkana")), List.of());

    @Test
    void anEmptyQueryMatchesEverything() {
        assertTrue(TemplateSearch.matches("My Pick", EFFICIENCY, "  ", Enchant::nameKey));
    }

    @Test
    void theQueryMatchesTheNameIgnoringCase() {
        assertTrue(TemplateSearch.matches("My Pick", EFFICIENCY, "my p", Enchant::nameKey));
        assertFalse(TemplateSearch.matches("My Pick", EFFICIENCY, "sword", Enchant::nameKey));
    }

    @Test
    void theQueryMatchesTheNameOfAnEnchantItMakes() {
        assertTrue(TemplateSearch.matches("x", EFFICIENCY, "efficiency", e -> "Efficiency"));
        assertFalse(TemplateSearch.matches("x", EFFICIENCY, "unbreaking", e -> "Efficiency"));
    }
}
