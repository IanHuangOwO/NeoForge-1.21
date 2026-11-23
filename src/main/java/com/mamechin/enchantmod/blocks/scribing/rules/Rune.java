package com.mamechin.enchantmod.blocks.scribing.rules;

import java.util.List;

/**
 * One rune, as described by its JSON file (data/&lt;namespace&gt;/enchantmod/scribing/rune/&lt;name&gt;.json). The id is "namespace:name".
 *
 * @param glyph       the one character drawn for it
 * @param size        side of the square it covers on the grid (1 to 3)
 * @param role        core runes make up an enchant; boost and stabilizer runes only modify the level
 * @param order       position in the palette and the handbook (then by id)
 * @param boostLevels boost runes: levels added by each one joined to a core
 * @param unlockLevel stabilizer runes: levels from this one up need at least one of them joined
 * @param extraLevels stabilizer runes: further levels allowed by each copy after the first
 */
public record Rune(String id, String glyph, int size, Role role, int order, int boostLevels, int unlockLevel, int extraLevels) {
    public enum Role { CORE, BOOST, STABILIZER }

    /** Utility runes are never part of an enchant core: the boost and stabilizer runes. */
    public boolean isUtility() {
        return role != Role.CORE;
    }

    /** The lang key prefix of this rune: "rune.namespace.name" (add ".meaning" or ".desc" for the texts). */
    public String langKey() {
        return "rune." + id.replace(':', '.');
    }

    /** Every rune in palette order. */
    public static List<Rune> values() {
        return Definitions.runes();
    }

    /** The rune with this id (a bare id means the enchantmod namespace), or null when unknown. */
    public static Rune byId(String id) {
        return Definitions.rune(id);
    }
}
