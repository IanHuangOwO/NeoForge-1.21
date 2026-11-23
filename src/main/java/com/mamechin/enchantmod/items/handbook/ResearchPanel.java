package com.mamechin.enchantmod.items.handbook;

import com.mamechin.enchantmod.client.gui.GuiArt;
import com.mamechin.enchantmod.client.gui.NineSlice;
import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.enchanting.Definitions;
import com.mamechin.enchantmod.enchanting.EnchantGroups;
import com.mamechin.enchantmod.enchanting.Rune;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The pages of the research handbook. Opened from the map it shows one page. Beside the scribing table it shows enchant pages
 * only, in tabs, with arrows to turn them. Pages: first one per rune with its short story (utility runes add their role), then one
 * per enchant with why those runes make it, what it does and a small example of how to link them.
 * Flat colours for now, to be replaced by a drawn texture.
 */
public class ResearchPanel {
    public static final int WIDTH = 112;
    private static final int BUTTON_W = 14, BUTTON_H = 12;
    private static final int INK = 0xFF3A2918;
    private static final ResourceLocation PAGE = GuiArt.research("page_panel"), TAB_OFF = GuiArt.research("tab_off"), TAB_ON = GuiArt.research("tab_on_page");

    private static final int TAB_OFF_W = 24, TAB_ON_W = 28, TAB_H = 28;

    private final Font font;
    private final PageContent content;
    private final boolean browse;  // true beside the scribing table: enchant pages only, grouped in tabs, turnable
    private final List<ResearchMap.Tab> tabList = new ArrayList<>(); // browse only: every tab but the runes, rebuilt when the definitions change
    private int tabsVersion = -1;
    private int tab, pos;          // browse only: the open mod tab and the page within it
    private int category;          // browse only: index into categories(), 0 = all enchants of the mod
    private int page;              // otherwise: the one page shown
    private int x, y, height; // set on every render, used by the click handler

    public ResearchPanel(Font font, boolean browse) {
        this.font = font;
        this.content = new PageContent(font);
        this.browse = browse;
    }

    static ItemStack tabIcon(String key) {
        return new ItemStack(switch (key) {
            case "minecraft" -> Items.GRASS_BLOCK;
            default -> Items.LAPIS_LAZULI; // the runes tab, and any mod without an icon of its own
        });
    }

    /** The mod tabs; they are rebuilt, and the open tab reset, when a reload or a sync changes the definitions. */
    private List<ResearchMap.Tab> tabs() {
        int version = Definitions.current().version();
        if (tabsVersion != version) {
            tabsVersion = version;
            tabList.clear();
            if (browse) for (ResearchMap.Tab t : ResearchMap.tabs()) if (!t.key().equals("runes")) tabList.add(t);
            tab = 0;
            pos = 0;
            category = 0;
        }
        return tabList;
    }

    private String categoryOf(int page) {
        return Definitions.enchants().get(page - Definitions.runes().size()).category();
    }

    /** The enchant pages of the open mod tab, in order. */
    private List<Integer> modPages() {
        if (tabs().isEmpty()) return List.of();
        int runes = Definitions.runes().size();
        return tabs().get(tab).nodes().stream().map(ResearchMap.Node::page).filter(p -> p >= runes).distinct().sorted().toList();
    }

    /** "all" first, then each kind of tool the open mod has enchants for. */
    private List<String> categories() {
        List<String> found = modPages().stream().map(p -> categoryOf(p)).distinct().toList();
        List<String> list = new ArrayList<>(List.of("all"));
        for (String c : EnchantGroups.CATEGORY_ORDER) if (found.contains(c)) list.add(c);
        return list;
    }

    /** The enchant pages of the open mod tab and tool tab, in order. */
    private List<Integer> tabPages() {
        List<String> cats = categories();
        category = Math.min(category, cats.size() - 1);
        String wanted = cats.get(category);
        return modPages().stream().filter(p -> wanted.equals("all") || categoryOf(p).equals(wanted)).toList();
    }

    private static ItemStack categoryIcon(String key) {
        return new ItemStack(switch (key) {
            case "general" -> Items.ANVIL;
            case "tools" -> Items.DIAMOND_PICKAXE;
            case "weapons" -> Items.DIAMOND_SWORD;
            case "armor" -> Items.DIAMOND_CHESTPLATE;
            case "bows" -> Items.BOW;
            default -> Items.NETHER_STAR; // all
        });
    }

    // the tool tabs are the left-hand tab textures turned a quarter and drawn at half size, so six fit within the page width
    private static final int CAT_W = 14, CAT_H = 12, CAT_PITCH = 15;
    private static final ResourceLocation CAT_OFF = GuiArt.research("cat_off"), CAT_ON = GuiArt.research("cat_on");

    /** The tool tab under the mouse (they sit along the top of the page), or -1. */
    private int categoryAt(double mouseX, double mouseY) {
        if (!browse || mouseY < y - CAT_H || mouseY >= y) return -1;
        int i = (int) Math.floor((mouseX - (x + 4)) / CAT_PITCH);
        return mouseX >= x + 4 && i >= 0 && i < categories().size() && mouseX < x + 4 + i * CAT_PITCH + CAT_W ? i : -1;
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
        for (int i = 0; i < tabs().size(); i++) if (mouseY >= tabY(i) && mouseY < tabY(i) + TAB_H) return i;
        return -1;
    }

    public void setPage(int page) {
        this.page = page;
    }


    public void render(GuiGraphics graphics, int x, int y, int height, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.height = height;
        NineSlice.draw(graphics, PAGE, x, y, WIDTH, height, 48, 8, true);

        List<Integer> pages = browse ? tabPages() : List.of();
        pos = Math.max(0, Math.min(pos, pages.size() - 1));
        content.draw(graphics, x, y, browse ? (pages.isEmpty() ? -1 : pages.get(pos)) : page);

        if (!browse) return;
        boolean overLeft = overButton(mouseX, mouseY, x + 8), overRight = overButton(mouseX, mouseY, x + WIDTH - 8 - BUTTON_W);
        graphics.blit(GuiArt.research("arrow_left" + (overLeft ? "_hover" : "")), x + 8, buttonY(), 0, 0, BUTTON_W, BUTTON_H, BUTTON_W, BUTTON_H);
        graphics.blit(GuiArt.research("arrow_right" + (overRight ? "_hover" : "")), x + WIDTH - 8 - BUTTON_W, buttonY(), 0, 0, BUTTON_W, BUTTON_H, BUTTON_W, BUTTON_H);
        String number = (pages.isEmpty() ? 0 : pos + 1) + "/" + pages.size();
        graphics.drawString(font, number, x + (WIDTH - font.width(number)) / 2, buttonY() + 2, INK, false);
        List<String> cats = categories();
        for (int i = 0; i < cats.size(); i++) {
            int cx = x + 4 + i * CAT_PITCH, cy = y - CAT_H;
            graphics.pose().pushPose();
            graphics.pose().translate(cx, cy, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            if (i == category) graphics.blit(CAT_ON, 0, 0, 0, 0, 28, 28, 28, 28); // the open tab reaches 2 px into the page
            else graphics.blit(CAT_OFF, 0, 0, 0, 0, 28, 24, 28, 24);
            graphics.pose().popPose();
            graphics.pose().pushPose();
            graphics.pose().translate(cx + 3, cy + 3, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.renderItem(categoryIcon(cats.get(i)), 0, 0);
            graphics.pose().popPose();
        }
        for (int i = 0; i < tabs().size(); i++) {
            int ty = tabY(i); // the open tab is wider and joins the page
            if (i == tab) graphics.blit(TAB_ON, x - TAB_OFF_W, ty, 0, 0, TAB_ON_W, TAB_H, TAB_ON_W, TAB_H);
            else graphics.blit(TAB_OFF, x - TAB_OFF_W, ty, 0, 0, TAB_OFF_W, TAB_H, TAB_OFF_W, TAB_H);
            graphics.renderItem(tabIcon(tabs().get(i).key()), x - TAB_OFF_W + 6, ty + 6);
        }
    }

    private boolean overButton(int mouseX, int mouseY, int bx) {
        return mouseX >= bx && mouseX < bx + BUTTON_W && mouseY >= buttonY() && mouseY < buttonY() + BUTTON_H;
    }

    /** Names the tab under the mouse; the owner calls this after its own tooltips so it is not drawn over. */
    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int cat = categoryAt(mouseX, mouseY);
        if (cat >= 0) graphics.renderTooltip(font, Component.translatable("gui.enchantmod.research.category." + categories().get(cat)), mouseX, mouseY);
        int hovered = tabAt(mouseX, mouseY);
        if (hovered >= 0) graphics.renderTooltip(font, tabName(tabs().get(hovered).key()), mouseX, mouseY);
    }

    private int buttonY() {
        return y + height - 8 - BUTTON_H;
    }

    public boolean contains(double mouseX, double mouseY) {
        return tabAt(mouseX, mouseY) >= 0 || categoryAt(mouseX, mouseY) >= 0 || mouseX >= x && mouseX < x + WIDTH && mouseY >= y && mouseY < y + height;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !contains(mouseX, mouseY)) return false;
        int cat = categoryAt(mouseX, mouseY);
        if (cat >= 0) {
            category = cat;
            pos = 0;
            return true;
        }
        int clicked = tabAt(mouseX, mouseY);
        if (clicked >= 0) {
            tab = clicked;
            category = 0; // the new mod may not have the same kinds of tool
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
