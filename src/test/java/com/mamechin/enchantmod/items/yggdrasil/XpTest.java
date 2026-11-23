package com.mamechin.enchantmod.items.yggdrasil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class XpTest {
    @Test
    void matchesTheVanillaTotals() {
        assertEquals(0, Xp.total(0, 0));
        assertEquals(160, Xp.total(10, 0), "wiki: level 10 = 160 points");
        assertEquals(1395, Xp.total(30, 0), "wiki: level 30 = 1395 points");
        assertEquals(160 + 14, Xp.total(10, 0.5F), "half of the 27 points to level 11, rounded");
    }
}
