package com.mamechin.enchantmod.scribing;

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
