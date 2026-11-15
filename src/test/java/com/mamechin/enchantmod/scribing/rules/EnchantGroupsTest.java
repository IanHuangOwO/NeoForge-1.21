package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The shipped JSON files: what they describe has to match the design (24 runes, 26 enchant rows, their kinds and tools). */
class EnchantGroupsTest {
    @org.junit.jupiter.api.BeforeAll
    static void definitions() {
        com.mamechin.enchantmod.scribing.rules.TestDefinitions.load();
    }

    private static Enchant row(String name) {
        return Definitions.enchants().stream().filter(e -> e.id().equals("enchantmod:" + name)).findFirst().orElseThrow();
    }

    @Test
    void shippedFilesLoadInFull() {
        assertEquals(24, Definitions.runes().size());
        assertEquals(26, Definitions.enchants().size());
        assertEquals("ᚠ", Definitions.runes().get(0).glyph()); // Fehu first: palette order is the Elder Futhark
        assertEquals("enchantmod:othala", Definitions.runes().get(23).id());
    }

    @Test
    void shippedTemplateTiers() {
        TemplateTier tier0 = Definitions.template("enchantmod:enchant_template_tier0"), tier2 = Definitions.template("enchantmod:enchant_template_tier2");
        assertEquals(9, tier0.width());
        assertEquals(7, tier0.height());
        assertEquals(4, tier0.maxLevel());
        assertEquals(BlueprintRules.WIDTH, tier2.width());
        assertEquals(Integer.MAX_VALUE, tier2.maxLevel());
        assertEquals(TemplateTier.FULL, Definitions.template("othermod:unknown"));
    }

    @Test
    void runeRolesAndSizes() {
        assertEquals(Rune.Role.BOOST, Rune.byId("uruz").role());
        assertEquals(1, Rune.byId("uruz").size());
        assertEquals(2, Rune.byId("dagaz").size());
        assertEquals(3, Rune.byId("algiz").size());
        assertEquals(Rune.Role.CORE, Rune.byId("algiz").role());
        assertEquals(3, Definitions.runes().stream().filter(Rune::isUtility).count());
    }

    @Test
    void categoriesAndNamespaces() {
        assertEquals("weapons", row("sharpness").category());
        assertEquals("general", row("mending").category());
        assertEquals("armor", row("swift_sneak").category());
        assertEquals("bows", row("infinity").category());
        assertEquals("minecraft", row("sharpness").namespace());
        assertEquals("enchantment.minecraft.sharpness", row("sharpness").nameKey());
        assertEquals("research.enchantmod.sharpness", row("sharpness").textKey());
    }

    @Test
    void everyEnchantIsInADisplayedCategory() {
        for (Enchant enchant : Definitions.enchants()) assertTrue(EnchantGroups.CATEGORY_ORDER.contains(enchant.category()));
    }

    @Test
    void bareAndNamespacedRuneIdsAreTheSameRune() {
        assertEquals(Rune.byId("berkana"), Rune.byId("enchantmod:berkana"));
    }
}
