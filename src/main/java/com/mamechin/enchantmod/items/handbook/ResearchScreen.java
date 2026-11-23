package com.mamechin.enchantmod.items.handbook;

import com.mamechin.enchantmod.client.gui.GuiArt;
import com.mamechin.enchantmod.client.gui.NineSlice;
import java.util.List;

import com.mamechin.enchantmod.enchanting.Definitions;
import com.mamechin.enchantmod.enchanting.Rune;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The research handbook: a map the player drags around, with one node per rune and enchant. Clicking a node opens its page;
 * Esc or right click goes back to the map. Flat colours for now. Every node is shown until there is a progression system.
 */
public class ResearchScreen extends Screen {
    private static final int NODE = 24, LIMIT = 260, TAB_W = 30, TAB_H = 28;
    private static final ResourceLocation MAP_BG = GuiArt.research("map_bg"), MAP_FRAME = GuiArt.research("map_frame"), TAB_OFF = GuiArt.research("tab_off"), TAB_ON = GuiArt.research("tab_on_map");
    private static final int LINK = 0x99B8764A;
    private static double panX, panY; // kept between openings

    private final ResearchPanel panel;
    private final List<ResearchMap.Tab> tabs = ResearchMap.tabs();
    private int tab;
    private List<ResearchMap.Node> nodes = tabs.get(0).nodes();
    private final int version = Definitions.current().version(); // a reload or a sync that changes the definitions closes the screen
    private boolean reading;
    private double dragged;

    public ResearchScreen() {
        super(Component.translatable("item.enchantmod.research_handbook"));
        panel = new ResearchPanel(Minecraft.getInstance().font, false);
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ResearchScreen());
    }

    private int originX() {
        return (TAB_W + width) / 2 + (int) panX;
    }

    private int originY() {
        return height / 2 + (int) panY;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (version != Definitions.current().version()) {
            onClose();
            return;
        }
        if (reading) {
            int h = Math.min(height - 30, 200);
            panel.render(graphics, (width - ResearchPanel.WIDTH) / 2, (height - h) / 2, h, mouseX, mouseY);
            closeButton(graphics, (width + ResearchPanel.WIDTH) / 2 - 10, (height - h) / 2 - 12, mouseX, mouseY);
            return;
        }
        NineSlice.tile(graphics, MAP_BG, TAB_W - 4, 4, width - TAB_W, height - 8, 0, 0, 16, 16, 16);
        graphics.enableScissor(TAB_W + 2, 10, width - 10, height - 10);
        int runes = Definitions.runes().size();
        for (ResearchMap.Node node : nodes) {
            for (int link : node.links()) {
                ResearchMap.Node rune = nodes.get(link);
                int ex = originX() + node.x(), ey = originY() + node.y(), rx = originX() + rune.x(), ry = originY() + rune.y();
                graphics.fill(Math.min(ex, rx), ey, Math.max(ex, rx) + 1, ey + 1, LINK); // across, then up or down
                graphics.fill(rx, Math.min(ey, ry), rx + 1, Math.max(ey, ry) + 1, LINK);
            }
        }
        ResearchMap.Node hovered = nodeAt(mouseX, mouseY);
        for (ResearchMap.Node node : nodes) {
            int nx = originX() + node.x() - NODE / 2, ny = originY() + node.y() - NODE / 2;
            boolean rune = node.page() < runes;
            graphics.blit(GuiArt.research((rune ? "node_rune" : "node_enchant") + (node == hovered ? "_hover" : "")), nx, ny, 0, 0, NODE, NODE, NODE, NODE);
            if (node.page() < runes) GuiArt.glyph(graphics, font, Rune.values().get(node.page()), nx + NODE / 2.0F, ny + NODE / 2.0F, 1.5F);
            else graphics.renderItem(new ItemStack(Items.ENCHANTED_BOOK), nx + 4, ny + 4);
        }
        graphics.disableScissor();
        NineSlice.draw(graphics, MAP_FRAME, TAB_W - 4, 4, width - TAB_W, height - 8, 32, 6, false);
        closeButton(graphics, width - 22, 10, mouseX, mouseY);
        for (int i = 0; i < tabs.size(); i++) {
            int ty = tabY(i); // the open tab is wider and joins the frame
            if (i == tab) graphics.blit(TAB_ON, 2, ty, 0, 0, 28, TAB_H, 28, TAB_H);
            else graphics.blit(TAB_OFF, 2, ty, 0, 0, 24, TAB_H, 24, TAB_H);
            graphics.renderItem(ResearchPanel.tabIcon(tabs.get(i).key()), 8, ty + 6);
        }
        int hoveredTab = tabAt(mouseX, mouseY);
        if (hoveredTab >= 0) graphics.renderTooltip(font, ResearchPanel.tabName(tabs.get(hoveredTab).key()), mouseX, mouseY);
        if (hovered != null) graphics.renderTooltip(font, nameOf(hovered), mouseX, mouseY);
    }

    private int closeX, closeY;

    /** The X at the top right of the map and of a page; remembers where it is for the click. */
    private void closeButton(GuiGraphics graphics, int cx, int cy, int mouseX, int mouseY) {
        closeX = cx;
        closeY = cy;
        boolean over = isOverClose(mouseX, mouseY);
        graphics.blit(GuiArt.research(over ? "close_hover" : "close"), cx, cy, 0, 0, 10, 10, 10, 10);
    }

    private boolean isOverClose(double mouseX, double mouseY) {
        return mouseX >= closeX && mouseX < closeX + 10 && mouseY >= closeY && mouseY < closeY + 10;
    }

    private int tabY(int i) {
        return 12 + i * (TAB_H + 2);
    }

    private int tabAt(double mouseX, double mouseY) {
        if (reading || mouseX < 2 || mouseX >= TAB_W) return -1;
        for (int i = 0; i < tabs.size(); i++) if (mouseY >= tabY(i) && mouseY < tabY(i) + TAB_H) return i;
        return -1;
    }

    private static Component nameOf(ResearchMap.Node node) {
        int runes = Definitions.runes().size();
        if (node.page() < runes) return Component.translatable(Rune.values().get(node.page()).langKey());
        return Component.translatable(Definitions.enchants().get(node.page() - runes).nameKey());
    }

    private ResearchMap.Node nodeAt(double mouseX, double mouseY) {
        if (reading || mouseX < TAB_W - 2) return null;
        for (ResearchMap.Node node : nodes) {
            if (Math.abs(mouseX - (originX() + node.x())) <= NODE / 2.0 && Math.abs(mouseY - (originY() + node.y())) <= NODE / 2.0) return node;
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isOverClose(mouseX, mouseY)) {
            if (reading) reading = false; else onClose(); // on a page X returns to the map, on the map it closes the handbook
            return true;
        }
        if (reading) {
            if (button == 1) reading = false;
            return true;
        }
        dragged = 0;
        int clicked = tabAt(mouseX, mouseY);
        if (button == 0 && clicked >= 0) {
            tab = clicked;
            nodes = tabs.get(clicked).nodes();
            panX = 0;
            panY = 0;
            dragged = LIMIT; // a click on the tabs never opens a node
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (reading || button != 0) return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        dragged += Math.abs(dx) + Math.abs(dy);
        panX = Math.max(-LIMIT, Math.min(LIMIT, panX + dx));
        panY = Math.max(-LIMIT, Math.min(LIMIT, panY + dy));
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!reading && button == 0 && dragged < 4) {
            ResearchMap.Node node = nodeAt(mouseX, mouseY);
            if (node != null) {
                panel.setPage(node.page());
                reading = true;
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (reading && keyCode == 256) { // Esc goes back to the map
            reading = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
