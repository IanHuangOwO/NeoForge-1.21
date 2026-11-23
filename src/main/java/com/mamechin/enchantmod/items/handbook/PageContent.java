package com.mamechin.enchantmod.items.handbook;

import com.mamechin.enchantmod.client.gui.GuiArt;
import java.util.List;

import com.mamechin.enchantmod.enchanting.Definitions;
import com.mamechin.enchantmod.enchanting.Enchant;
import com.mamechin.enchantmod.enchanting.Rune;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * What is written on one handbook page: pages 0..23 are the runes (name, board, short story, role of a utility rune), the
 * ones after are the enchants (why the runes make it, what it does, an example of the runes linked). The panel adds the tabs and arrows.
 */
final class PageContent {
    private static final int INK = 0xFF3A2918, TITLE = 0xFF5A2E12, LINE = 0xFFB8764A;
    private static final int BOARD = 26, GAP = 9; // the example uses the same 3x3 boards as the grid
    private static final ResourceLocation BOARD_3X3 = GuiArt.board(3);

    private final Font font;
    private int x, y; // top left of the page being drawn

    PageContent(Font font) {
        this.font = font;
    }

    void draw(GuiGraphics graphics, int x, int y, int page) {
        this.x = x;
        this.y = y;
        List<Rune> runes = Definitions.runes();
        List<Enchant> enchants = Definitions.enchants();
        if (page < 0) return;
        if (page < runes.size()) renderRune(graphics, runes.get(page));
        else if (page - runes.size() < enchants.size()) renderCore(graphics, enchants.get(page - runes.size()));
    }

    /** Draws wrapped text and returns the y just below it. */
    private int paragraph(GuiGraphics graphics, Component text, int top, int color) {
        for (FormattedCharSequence line : font.split(text, ResearchPanel.WIDTH - 16)) {
            graphics.drawString(font, line, x + 8, top, color, false);
            top += 10;
        }
        return top;
    }

    private void renderRune(GuiGraphics graphics, Rune rune) {
        int top = paragraph(graphics, Component.translatable(rune.langKey()), y + 8, TITLE) + 4;
        // the board at its grid size, centred in a 3x3 space
        int size = rune.size() * 9;
        int cx = x + ResearchPanel.WIDTH / 2;
        graphics.blit(GuiArt.board(rune.size()), cx - size / 2, top + (27 - size) / 2, 0, 0, size, size, size, size);
        GuiArt.glyph(graphics, font, rune, cx, top + 13.5F, GuiArt.glyphScale(rune));
        top += 27 + 8;
        top = paragraph(graphics, Component.translatable(rune.langKey() + ".meaning"), top, INK) + 6;
        if (rune.isUtility()) {
            graphics.drawString(font, Component.translatable("gui.enchantmod.research.role"), x + 8, top, TITLE, false);
            paragraph(graphics, Component.translatable(rune.langKey() + ".desc"), top + 11, INK);
        }
    }

    private void renderCore(GuiGraphics graphics, Enchant core) {
        int top = y + 8;
        top = paragraph(graphics, Component.translatable(core.nameKey()), top, TITLE) + 4;

        // why this combination makes the enchant, what it does, then how to link the runes
        top = paragraph(graphics, Component.translatable(core.textKey() + ".why"), top, INK) + 4;
        top = paragraph(graphics, Component.translatable(core.textKey()), top, INK) + 6;

        graphics.drawString(font, Component.translatable("gui.enchantmod.research.example"), x + 8, top, TITLE, false);
        top += 12;
        renderExample(graphics, core.runes(), x + ResearchPanel.WIDTH / 2, top);
    }

    /** The runes on their boards with copper lines through the gap between them, laid out as they would be on the grid. */
    private void renderExample(GuiGraphics graphics, List<String> runes, int centreX, int top) {
        int n = runes.size();
        int[][] at = new int[n][];
        if (n == 1) {
            at[0] = new int[] {centreX - BOARD / 2, top};
        } else {
            int left = centreX - (2 * BOARD + GAP) / 2;
            at[0] = new int[] {left, top};
            at[1] = new int[] {left + BOARD + GAP, top};
            if (n >= 3) at[2] = new int[] {centreX - BOARD / 2, top + BOARD + GAP};
        }
        if (n >= 2) graphics.fill(at[0][0] + BOARD, top + BOARD / 2 - 1, at[1][0], top + BOARD / 2 + 1, LINE);
        if (n >= 3) {
            graphics.fill(centreX - 9, top + BOARD, centreX - 7, top + BOARD + GAP, LINE);
            graphics.fill(centreX + 8, top + BOARD, centreX + 10, top + BOARD + GAP, LINE);
        }
        for (int i = 0; i < n; i++) {
            Rune rune = Rune.byId(runes.get(i));
            if (rune == null) continue;
            graphics.blit(BOARD_3X3, at[i][0], at[i][1], 0, 0, 27, 27, 27, 27);
            GuiArt.glyph(graphics, font, rune, at[i][0] + BOARD / 2.0F, at[i][1] + BOARD / 2.0F, 2.5F);
        }
    }
}
