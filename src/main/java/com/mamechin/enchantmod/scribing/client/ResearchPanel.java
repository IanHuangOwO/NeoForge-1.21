package com.mamechin.enchantmod.scribing.client;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.item.ModItems;
import com.mamechin.enchantmod.scribing.EnchantForecast;
import com.mamechin.enchantmod.scribing.Rune;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The pages of the research tablet. Opened from the map it shows one page. Beside the scribing table it shows enchant pages
 * only, in tabs, with arrows to turn them. Pages: first one per rune with its short story (utility runes add their role), then one
 * per enchant with why those runes make it, what it does and a small example of how to link them.
 * Flat colours for now, to be replaced by a drawn texture.
 */
public class ResearchPanel {
    public static final int WIDTH = 112;
    private static final int BUTTON_W = 14, BUTTON_H = 12, BOARD = 26, GAP = 9; // the example uses the same 3x3 boards as the grid
    private static final int INK = 0xFF3A2918, TITLE = 0xFF5A2E12, LINE = 0xFFB8764A;
    private static final ResourceLocation BOARD_3X3 = gui("rune_board_3x3"), PAGE = research("page_panel"), TAB_OFF = research("tab_off"), TAB_ON = research("tab_on_page");

    private static final int TAB_OFF_W = 24, TAB_ON_W = 28, TAB_H = 28;

    private final Font font;
    private final boolean browse;  // true beside the scribing table: enchant pages only, grouped in tabs, turnable
    private final List<ResearchMap.Tab> tabs = new ArrayList<>(); // browse only: every tab but the runes
    private int tab, pos;          // browse only: the open tab and the page within it
    private int page;              // otherwise: the one page shown
    private int x, y, height; // set on every render, used by the click handler

    public ResearchPanel(Font font, boolean browse) {
        this.font = font;
        this.browse = browse;
        if (browse) for (ResearchMap.Tab t : ResearchMap.tabs()) if (!t.key().equals("runes")) tabs.add(t);
    }

    static ItemStack tabIcon(String key) {
        return new ItemStack(switch (key) {
            case "minecraft" -> Items.GRASS_BLOCK;
            default -> ModItems.ENCHANT_TEMPLATE.get();
        });
    }

    /** The enchant pages of the open tab, in order. */
    private List<Integer> tabPages() {
        int runes = Rune.values().length;
        return tabs.get(tab).nodes().stream().map(ResearchMap.Node::page).filter(p -> p >= runes).distinct().sorted().toList();
    }

    /** "Vanilla" for Minecraft, the mod's id for any other mod until it gets a name of its own. */
    static Component tabName(String key) {
        return Component.translatableWithFallback("gui.enchantmod.research.tab." + key, key);
    }

    private int tabY(int i) {
        return y + 8 + i * (TAB_H + 2);
    }

    private int tabAt(double mouseX, double mouseY) {
        if (!browse || mouseX < x - TAB_OFF_W || mouseX >= x) return -1;
        for (int i = 0; i < tabs.size(); i++) if (mouseY >= tabY(i) && mouseY < tabY(i) + TAB_H) return i;
        return -1;
    }

    private static ResourceLocation research(String name) {
        return gui("research/" + name);
    }

    private static ResourceLocation gui(String name) {
        return ResourceLocation.fromNamespaceAndPath("enchantmod", "textures/gui/" + name + ".png");
    }

    public void setPage(int page) {
        this.page = page;
    }


    public void render(GuiGraphics graphics, int x, int y, int height, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.height = height;
        NineSlice.draw(graphics, PAGE, x, y, WIDTH, height, 48, 8, true);

        List<EnchantForecast.Core> rows = EnchantForecast.table();
        int runes = Rune.values().length;
        int shown = browse ? tabPages().get(pos) : page;
        if (shown < runes) renderRune(graphics, Rune.values()[shown]); else renderCore(graphics, rows.get(shown - runes));

        if (!browse) return;
        boolean overLeft = overButton(mouseX, mouseY, x + 8), overRight = overButton(mouseX, mouseY, x + WIDTH - 8 - BUTTON_W);
        graphics.blit(research("arrow_left" + (overLeft ? "_hover" : "")), x + 8, buttonY(), 0, 0, BUTTON_W, BUTTON_H, BUTTON_W, BUTTON_H);
        graphics.blit(research("arrow_right" + (overRight ? "_hover" : "")), x + WIDTH - 8 - BUTTON_W, buttonY(), 0, 0, BUTTON_W, BUTTON_H, BUTTON_W, BUTTON_H);
        String number = (pos + 1) + "/" + tabPages().size();
        graphics.drawString(font, number, x + (WIDTH - font.width(number)) / 2, buttonY() + 2, INK, false);
        for (int i = 0; i < tabs.size(); i++) {
            int ty = tabY(i); // the open tab is wider and joins the page
            if (i == tab) graphics.blit(TAB_ON, x - TAB_OFF_W, ty, 0, 0, TAB_ON_W, TAB_H, TAB_ON_W, TAB_H);
            else graphics.blit(TAB_OFF, x - TAB_OFF_W, ty, 0, 0, TAB_OFF_W, TAB_H, TAB_OFF_W, TAB_H);
            graphics.renderItem(tabIcon(tabs.get(i).key()), x - TAB_OFF_W + 6, ty + 6);
        }
    }

    private boolean overButton(int mouseX, int mouseY, int bx) {
        return mouseX >= bx && mouseX < bx + BUTTON_W && mouseY >= buttonY() && mouseY < buttonY() + BUTTON_H;
    }

    /** Names the tab under the mouse; the owner calls this after its own tooltips so it is not drawn over. */
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int hovered = tabAt(mouseX, mouseY);
        if (hovered >= 0) graphics.renderTooltip(font, tabName(tabs.get(hovered).key()), mouseX, mouseY);
    }

    private int buttonY() {
        return y + height - 8 - BUTTON_H;
    }

    /** Draws wrapped text and returns the y just below it. */
    private int paragraph(GuiGraphics graphics, Component text, int top, int color) {
        for (FormattedCharSequence line : font.split(text, WIDTH - 16)) {
            graphics.drawString(font, line, x + 8, top, color, false);
            top += 10;
        }
        return top;
    }

    private void renderRune(GuiGraphics graphics, Rune rune) {
        int top = paragraph(graphics, Component.translatable("rune.enchantmod." + rune.id()), y + 8, TITLE) + 4;
        // the board at its grid size, centred in a 3x3 space
        int size = rune.size() * 9;
        int cx = x + WIDTH / 2;
        graphics.blit(boardFor(rune.size()), cx - size / 2, top + (27 - size) / 2, 0, 0, size, size, size, size);
        glyph(graphics, rune, cx, top + 13.5F, rune.size() == 3 ? 2.5F : rune.size() == 2 ? 1.5F : 1.0F);
        top += 27 + 8;
        top = paragraph(graphics, Component.translatable("rune.enchantmod." + rune.id() + ".meaning"), top, INK) + 6;
        if (rune.isUtility()) {
            graphics.drawString(font, Component.translatable("gui.enchantmod.research.role"), x + 8, top, TITLE, false);
            paragraph(graphics, Component.translatable("rune.enchantmod." + rune.id() + ".desc"), top + 11, INK);
        }
    }

    private static ResourceLocation boardFor(int size) {
        return gui("rune_board_" + size + "x" + size);
    }

    private void renderCore(GuiGraphics graphics, EnchantForecast.Core core) {
        int top = y + 8;
        top = paragraph(graphics, Component.translatable("enchantment.minecraft." + core.enchant()), top, TITLE) + 4;

        // why this combination makes the enchant, what it does, then how to link the runes
        top = paragraph(graphics, Component.translatable("research.enchantmod." + core.enchant() + ".why"), top, INK) + 4;
        top = paragraph(graphics, Component.translatable("research.enchantmod." + core.enchant()), top, INK) + 6;

        graphics.drawString(font, Component.translatable("gui.enchantmod.research.example"), x + 8, top, TITLE, false);
        top += 12;
        renderExample(graphics, core.runes(), x + WIDTH / 2, top);
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
            glyph(graphics, rune, at[i][0] + BOARD / 2.0F, at[i][1] + BOARD / 2.0F, 2.5F);
        }
    }

    private void glyph(GuiGraphics graphics, Rune rune, float centreX, float centreY, float scale) {
        String text = String.valueOf(rune.glyph());
        graphics.pose().pushPose();
        graphics.pose().translate(centreX, centreY, 0);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-font.width(text) / 2.0F, -4.0F, 0);
        graphics.drawString(font, text, 0, 0, ScribingTableScreen.glyphColor(rune), false);
        graphics.pose().popPose();
    }

    public boolean contains(double mouseX, double mouseY) {
        return tabAt(mouseX, mouseY) >= 0 || mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) return false;
        int clicked = tabAt(mouseX, mouseY);
        if (clicked >= 0) {
            tab = clicked;
            pos = 0;
            return true;
        }
        if (browse && mouseY >= buttonY() && mouseY < buttonY() + BUTTON_H) {
            if (mouseX < x + 8 + BUTTON_W) turn(-1);
            else if (mouseX >= x + WIDTH - 8 - BUTTON_W) turn(1);
        }
        return true;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (!browse || !contains(mouseX, mouseY) || scrollY == 0) return false;
        turn(scrollY > 0 ? -1 : 1);
        return true;
    }

    private void turn(int by) {
        pos = Math.max(0, Math.min(tabPages().size() - 1, pos + by));
    }
}
