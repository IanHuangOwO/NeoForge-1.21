package com.mamechin.enchantmod.scribing.research;

import com.mamechin.enchantmod.scribing.rules.Rune;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** What the scribing table's screen and the handbook's screens share: GUI texture paths, rune glyph colours, scales and drawing. */
public final class GuiArt {
    // Every rune sits on the same dark slate board; the glyph colour tells the kind apart.
    private static final int CORE_GLYPH = 0xFFFFE290, BOOST_GLYPH = 0xFFF08C78, STABILIZER_GLYPH = 0xFF8CAAFF;

    private GuiArt() {}

    public static ResourceLocation gui(String name) {
        return ResourceLocation.fromNamespaceAndPath("enchantmod", "textures/gui/" + name + ".png");
    }

    public static ResourceLocation research(String name) {
        return gui("research/" + name);
    }

    /** The slate board for a rune of this size (1, 2 or 3 cells). */
    public static ResourceLocation board(int size) {
        return gui("rune_board_" + size + "x" + size);
    }

    public static int glyphColor(Rune rune) {
        return switch (rune.role()) {
            case BOOST -> BOOST_GLYPH;
            case STABILIZER -> STABILIZER_GLYPH;
            case CORE -> CORE_GLYPH;
        };
    }

    /** Glyph scale on the board: the 3x3 and 2x2 faces are smaller than the glyph at 3x and 2x, so it stops short of the rim. */
    public static float glyphScale(Rune rune) {
        return switch (rune.size()) {
            case 3 -> 2.5F;
            case 2 -> 1.5F;
            default -> 1.0F;
        };
    }

    /** Draws the glyph centred on a point, at any scale. */
    public static void glyph(GuiGraphics graphics, Font font, Rune rune, float centreX, float centreY, float scale) {
        String text = rune.glyph();
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, centreY, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-font.width(text) / 2.0F, -4.0F, 0); // fractional, so scaled glyphs stay centred
        graphics.drawString(font, text, 0, 0, glyphColor(rune), false);
        graphics.pose().popPose();
    }
}
