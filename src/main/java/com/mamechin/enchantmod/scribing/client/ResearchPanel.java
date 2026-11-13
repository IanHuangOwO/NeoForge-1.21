package com.mamechin.enchantmod.scribing.client;

import java.util.List;

import com.mamechin.enchantmod.scribing.EnchantForecast;
import com.mamechin.enchantmod.scribing.Rune;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * The pages of the research tablet, drawn beside the scribing table while the player carries one: which runes make which
 * enchant, then what the utility runes do. Flat colours for now, to be replaced by a drawn texture.
 */
public class ResearchPanel {
    public static final int WIDTH = 112;
    private static final int PER_PAGE = 8, ENTRY = 22, TOP = 22, BUTTON_W = 14, BUTTON_H = 12;
    private static final int EDGE = 0xFF8A5A2B, PAPER = 0xFFCEBC92, INK = 0xFF3A2918, BUTTON = 0xFFA86C4C;
    private static final ResourceLocation BOARD = ResourceLocation.fromNamespaceAndPath("enchantmod", "textures/gui/rune_board_1x1.png");

    private final Font font;
    private int page;
    private int x, y, height; // set on every render, used by the click handler

    public ResearchPanel(Font font) {
        this.font = font;
    }

    private static int coreRows() {
        return EnchantForecast.table().size();
    }

    private static int pages() {
        return (coreRows() + PER_PAGE - 1) / PER_PAGE + 1; // the last page holds the utility runes
    }

    private static boolean utilityPage(int page) {
        return page == pages() - 1;
    }

    public void render(GuiGraphics graphics, int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
        graphics.fill(x, y, x + WIDTH, y + height, EDGE);
        graphics.fill(x + 2, y + 2, x + WIDTH - 2, y + height - 2, PAPER);
        graphics.drawString(font, Component.translatable("gui.enchantmod.research.title"), x + 8, y + 7, INK, false);

        if (utilityPage(page)) {
            renderUtility(graphics);
        } else {
            List<EnchantForecast.Core> rows = EnchantForecast.table();
            for (int i = 0; i < PER_PAGE; i++) {
                int index = page * PER_PAGE + i;
                if (index >= rows.size()) break;
                renderCore(graphics, rows.get(index), x + 8, y + TOP + i * ENTRY);
            }
        }

        graphics.fill(x + 8, buttonY(), x + 8 + BUTTON_W, buttonY() + BUTTON_H, BUTTON);
        graphics.drawString(font, "<", x + 8 + 5, buttonY() + 2, INK, false);
        graphics.fill(x + WIDTH - 8 - BUTTON_W, buttonY(), x + WIDTH - 8, buttonY() + BUTTON_H, BUTTON);
        graphics.drawString(font, ">", x + WIDTH - 8 - BUTTON_W + 5, buttonY() + 2, INK, false);
        String number = (page + 1) + "/" + pages();
        graphics.drawString(font, number, x + (WIDTH - font.width(number)) / 2, buttonY() + 2, INK, false);
    }

    private int buttonY() {
        return y + height - 6 - BUTTON_H;
    }

    private void renderCore(GuiGraphics graphics, EnchantForecast.Core core, int left, int top) {
        graphics.drawString(font, Component.translatable("enchantment.minecraft." + core.enchant()), left, top, INK, false);
        int cursor = left;
        for (int i = 0; i < core.runes().size(); i++) {
            Rune rune = Rune.byId(core.runes().get(i));
            if (rune == null) continue;
            if (i > 0) {
                graphics.drawString(font, "+", cursor + 2, top + 12, INK, false);
                cursor += 10;
            }
            drawRune(graphics, rune, cursor, top + 10);
            cursor += 11;
        }
    }

    private void drawRune(GuiGraphics graphics, Rune rune, int left, int top) {
        graphics.blit(BOARD, left, top, 0, 0, 9, 9, 9, 9);
        String glyph = String.valueOf(rune.glyph());
        graphics.drawString(font, glyph, left + 4 - font.width(glyph) / 2, top, ScribingTableScreen.glyphColor(rune), false);
    }

    private void renderUtility(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("gui.enchantmod.research.utility"), x + 8, y + TOP, INK, false);
        int top = y + TOP + 14;
        for (Rune rune : Rune.values()) {
            if (!rune.isUtility()) continue;
            drawRune(graphics, rune, x + 8, top);
            graphics.drawString(font, Component.translatable("rune.enchantmod." + rune.id()), x + 22, top + 1, INK, false);
            for (FormattedCharSequence line : font.split(Component.translatable("rune.enchantmod." + rune.id() + ".desc"), WIDTH - 16)) {
                top += 10;
                graphics.drawString(font, line, x + 8, top + 2, INK, false);
            }
            top += 18;
        }
    }

    public boolean contains(double mouseX, double mouseY) {
        return mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) return false;
        if (mouseY >= buttonY() && mouseY < buttonY() + BUTTON_H) {
            if (mouseX < x + 8 + BUTTON_W) turn(-1);
            else if (mouseX >= x + WIDTH - 8 - BUTTON_W) turn(1);
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!contains(mouseX, mouseY) || scrollY == 0) return false;
        turn(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void turn(int by) {
        page = Math.max(0, Math.min(pages() - 1, page + by));
    }
}
