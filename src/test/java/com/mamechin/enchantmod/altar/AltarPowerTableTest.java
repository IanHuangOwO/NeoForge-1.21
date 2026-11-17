package com.mamechin.enchantmod.altar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AltarPowerTableTest {
    @Test
    void rarityGivesTheWeightAndLaterFilesOverride() {
        List<String> warnings = new ArrayList<>();
        AltarPowerTable table = AltarPowerTable.parse(List.of(
                "{ \"rarities\": { \"common\": 1, \"rare\": 4 }, \"default_rarity\": \"common\", \"enchantments\": { \"mending\": \"rare\", \"minecraft:sharpness\": \"common\" } }",
                "{ \"rarities\": { \"rare\": 5 }, \"enchantments\": { \"minecraft:sharpness\": \"rare\" } }"),
                warnings::add);
        assertEquals(5, table.weight("minecraft:mending"), "bare id means minecraft; the later rarity weight wins");
        assertEquals(5, table.weight("minecraft:sharpness"), "the later file moves sharpness to rare");
        assertEquals(1, table.weight("othermod:zap"), "unlisted uses the default rarity");
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void badEntriesAreDroppedWithAWarning() {
        List<String> warnings = new ArrayList<>();
        AltarPowerTable table = AltarPowerTable.parse(List.of(
                "not json",
                "{ \"rarities\": { \"common\": 2, \"broken\": \"x\", \"neg\": -1 }, \"default_rarity\": \"common\", \"enchantments\": { \"minecraft:mending\": \"mythic\" } }"),
                warnings::add);
        assertEquals(4, warnings.size(), warnings.toString());
        assertEquals(2, table.weight("minecraft:mending"), "unknown rarity falls back to the default");
        assertEquals(1, AltarPowerTable.EMPTY.weight("minecraft:mending"), "no file: every level worth 1");
    }
}
