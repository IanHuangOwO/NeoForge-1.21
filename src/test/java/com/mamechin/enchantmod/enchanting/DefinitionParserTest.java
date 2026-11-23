package com.mamechin.enchantmod.enchanting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

/** A data pack that gets something wrong loses that entry, with a warning, and nothing else. */
class DefinitionParserTest {
    private static final String CORE = "{\"glyph\":\"A\",\"size\":3,\"role\":\"core\"}";

    @AfterAll
    static void restore() { // later test classes expect the shipped files
        TestDefinitions.load();
    }

    private static List<String> load(Map<String, String> runes, Map<String, String> enchants) {
        return TestDefinitions.install(runes, enchants, null);
    }

    @Test
    void anotherModCanAddARuneAndAnEnchant() {
        List<String> warnings = load(Map.of("enchantmod:a", CORE, "othermod:b", "{\"glyph\":\"B\",\"size\":2,\"role\":\"core\"}"),
                Map.of("othermod:thing", "{\"enchantment\":\"othermod:thing\",\"runes\":[\"othermod:b\",\"a\"],\"category\":\"tools\"}"));
        assertTrue(warnings.isEmpty(), warnings.toString());
        Enchant e = Definitions.enchants().get(0);
        assertEquals("othermod", e.namespace());
        assertEquals(List.of("enchantmod:a", "othermod:b"), e.runes());
        assertEquals(e, Definitions.enchantFor(List.of("enchantmod:a", "othermod:b")));
    }

    @Test
    void badRunesAreDropped() {
        List<String> warnings = load(Map.of(
                "enchantmod:big", "{\"glyph\":\"A\",\"size\":4,\"role\":\"core\"}",
                "enchantmod:two", "{\"glyph\":\"AB\",\"size\":3,\"role\":\"core\"}",
                "enchantmod:role", "{\"glyph\":\"A\",\"size\":3,\"role\":\"king\"}",
                "enchantmod:stab", "{\"glyph\":\"A\",\"size\":2,\"role\":\"stabilizer\",\"unlock_level\":1}",
                "enchantmod:ok", CORE), Map.of());
        assertEquals(4, warnings.size(), warnings.toString());
        assertEquals(1, Definitions.runes().size());
        assertNull(Rune.byId("big"));
    }

    @Test
    void badEnchantsAreDropped() {
        List<String> warnings = load(Map.of("enchantmod:a", CORE, "enchantmod:u", "{\"glyph\":\"U\",\"size\":1,\"role\":\"boost\"}"), Map.of(
                "enchantmod:unknown", "{\"enchantment\":\"x:y\",\"runes\":[\"enchantmod:nope\"]}",
                "enchantmod:boost", "{\"enchantment\":\"x:y\",\"runes\":[\"enchantmod:u\"]}",
                "enchantmod:none", "{\"enchantment\":\"x:y\",\"runes\":[]}",
                "enchantmod:twice", "{\"enchantment\":\"x:y\",\"runes\":[\"enchantmod:a\",\"enchantmod:a\"]}",
                "enchantmod:first", "{\"enchantment\":\"x:y\",\"runes\":[\"enchantmod:a\"],\"order\":1}",
                "enchantmod:second", "{\"enchantment\":\"x:z\",\"runes\":[\"enchantmod:a\"],\"order\":2}"));
        assertEquals(5, warnings.size(), warnings.toString());
        assertEquals(List.of("enchantmod:first"), Definitions.enchants().stream().map(Enchant::id).toList()); // the same runes: the earlier row wins
    }

    @Test
    void malformedJsonAndMissingForecastFallBackToDefaults() {
        List<String> warnings = TestDefinitions.install(Map.of("enchantmod:bad", "{not json", "enchantmod:a", CORE), Map.of(), "{\"line_break\":{\"max\":2}}");
        assertEquals(2, warnings.size(), warnings.toString());
        assertEquals(ForecastRules.DEFAULT, Definitions.rules());
    }

    @Test
    void forecastNumbersCanBeChanged() {
        TestDefinitions.install(Map.of(), Map.of(), "{\"line_break\":{\"base\":0.1,\"per_cell\":0.02,\"max\":0.5}}");
        assertEquals(new ForecastRules(0.1, 0.02, 0.5), Definitions.rules());
    }
}
