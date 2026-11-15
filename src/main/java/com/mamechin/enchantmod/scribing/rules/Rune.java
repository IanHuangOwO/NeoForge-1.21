package com.mamechin.enchantmod.scribing.rules;

import java.util.Locale;

/** The 24 runes of the Elder Futhark, drawn as their Unicode glyph. The id is the lower-case name stored in blueprints. */
public enum Rune {
    FEHU('ᚠ'), URUZ('ᚢ'), THURISAZ('ᚦ'), ANSUZ('ᚨ'), RAIDHO('ᚱ'), KENAZ('ᚲ'), GEBO('ᚷ'), WUNJO('ᚹ'),
    HAGALAZ('ᚺ'), NAUTHIZ('ᚾ'), ISA('ᛁ'), JERA('ᛃ'), EIHWAZ('ᛇ'), PERTHRO('ᛈ'), ALGIZ('ᛉ'), SOWILO('ᛋ'),
    TIWAZ('ᛏ'), BERKANA('ᛒ'), EHWAZ('ᛖ'), MANNAZ('ᛗ'), LAGUZ('ᛚ'), INGWAZ('ᛜ'), DAGAZ('ᛞ'), OTHALA('ᛟ');

    private final char glyph;

    Rune(char glyph) {
        this.glyph = glyph;
    }

    /** Utility runes are never part of an enchant core: the boost rune and the two stabilizers. */
    public boolean isUtility() {
        return this == URUZ || this == MANNAZ || this == DAGAZ;
    }

    /** Side of the square this rune covers on the grid: boost 1, stabilizers 2, core runes 3. */
    public int size() {
        if (this == URUZ) return 1;
        return isUtility() ? 2 : 3;
    }

    public char glyph() {
        return glyph;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The rune with this id, or null when unknown. */
    public static Rune byId(String id) {
        for (Rune rune : values()) {
            if (rune.id().equals(id)) return rune;
        }
        return null;
    }
}
