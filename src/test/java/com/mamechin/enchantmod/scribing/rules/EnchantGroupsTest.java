package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EnchantGroupsTest {
    @Test
    void categoriesAndNamespaces() {
        assertEquals("weapons", EnchantGroups.category("sharpness"));
        assertEquals("general", EnchantGroups.category("mending"));
        assertEquals("general", EnchantGroups.category("othermod:thing"));
        assertEquals("minecraft", EnchantGroups.namespace("sharpness"));
        assertEquals("othermod", EnchantGroups.namespace("othermod:thing"));
    }

    @Test
    void everyTableEnchantIsInADisplayedCategory() {
        for (EnchantForecast.Core core : EnchantForecast.table()) assertTrue(EnchantGroups.CATEGORY_ORDER.contains(EnchantGroups.category(core.enchant())));
    }
}
