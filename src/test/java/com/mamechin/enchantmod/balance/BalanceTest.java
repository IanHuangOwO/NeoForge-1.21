package com.mamechin.enchantmod.balance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BalanceTest {
    @Test
    void copiesMergeKeyByKey() {
        String merged = Balance.merge(List.of(
                "{ \"rarities\": { \"common\": 1, \"rare\": 4 }, \"enchantments\": { \"mending\": \"rare\" }, \"spin\": { \"ramp_up_ticks\": 60 } }",
                "not json",
                "{ \"rarities\": { \"rare\": 5 }, \"spin\": { \"ramp_down_ticks\": 10 } }"), message -> {});
        List<String> warnings = new ArrayList<>();
        Balance.Altar altar = Balance.parse(Map.of("altar", merged), warnings::add).altar();
        assertEquals(5, altar.weight("minecraft:mending"), "bare id means minecraft; the later weight wins");
        assertEquals(60, altar.rampUp(), "kept from the first copy");
        assertEquals(10, altar.rampDown(), "added by the second copy");
        assertTrue(warnings.isEmpty(), warnings.toString());
    }

    @Test
    void missingFilesKeepTheDefaults() {
        Balance b = Balance.parse(Map.of(), message -> {});
        assertEquals(Balance.DEFAULT_ALTAR, b.altar());
        assertEquals(20, b.press().ticksPerTemplate());
        assertEquals(new Balance.Cube(3, 10), b.cube("enchantmod:enchant_cube_tier1", new Balance.Cube(3, 10)));
        assertEquals(1, b.altar().weight("minecraft:sharpness"), "no rarities: every level worth 1");
        assertEquals(new Balance.Area(8, -1, 7), b.altar().area(5), "a higher tier uses the last area");
    }

    @Test
    void heatSourcesByBlockId() {
        List<String> warnings = new ArrayList<>();
        Balance b = Balance.parse(Map.of("heat", "{ \"torch\": { \"tier\": 1 }, \"minecraft:campfire\": { \"tier\": 2, \"requires_lit\": true }, \"minecraft:dirt\": { } }"), warnings::add);
        assertEquals(new Balance.Heat(1, false), b.heat().get("minecraft:torch"), "bare id means minecraft");
        assertEquals(new Balance.Heat(2, true), b.heat().get("minecraft:campfire"));
        assertTrue(!b.heat().containsKey("minecraft:dirt"));
        assertEquals(1, warnings.size(), warnings.toString());
    }

    @Test
    void badValuesAreDroppedWithAWarning() {
        List<String> warnings = new ArrayList<>();
        Balance b = Balance.parse(Map.of(
                "altar", "{ \"spin\": { \"ramp_up_ticks\": 0 }, \"scan\": [ { \"radius\": 3 } ], \"rarities\": { \"common\": 2, \"neg\": -1 }, \"default_rarity\": \"common\", \"enchantments\": { \"mending\": \"mythic\" } }",
                "cubes", "{ \"enchantmod:enchant_cube_tier0\": { \"capacity\": 9, \"max_level\": 7 }, \"enchantmod:bad\": { \"capacity\": 1 } }",
                "quills", "{ \"enchantmod:quill_tier0\": { \"power\": 12, \"xp_levels\": 4 } }",
                "press", "{ \"ticks_per_template\": \"slow\" }"), warnings::add);
        assertEquals(60, b.altar().rampUp());
        assertEquals(Balance.DEFAULT_ALTAR.scan(), b.altar().scan());
        assertEquals(2, b.altar().weight("minecraft:mending"), "unknown rarity falls back to the default");
        assertEquals(new Balance.Cube(6, 7), b.cubes().get("enchantmod:enchant_cube_tier0"), "capacity capped at the press's six slots");
        assertTrue(!b.cubes().containsKey("enchantmod:bad"));
        assertEquals(new Balance.Quill(12, 4), b.quills().get("enchantmod:quill_tier0"));
        assertEquals(20, b.press().ticksPerTemplate());
        assertEquals(6, warnings.size(), warnings.toString());
    }
}
