package com.mamechin.enchantmod.scribing.client;

import java.util.List;

import com.mamechin.enchantmod.item.ModItems;
import com.mamechin.enchantmod.scribing.EnchantForecast;
import com.mamechin.enchantmod.scribing.Rune;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The research tablet: a map the player drags around, with one node per rune and enchant. Clicking a node opens its page;
 * Esc or right click goes back to the map. Flat colours for now. Every node is shown until there is a progression system.
 */
public class ResearchScreen extends Screen {
    private static final int NODE = 22, LIMIT = 260, TAB_W = 30, TAB_H = 28;
    private static final int BACKDROP = 0xFF1E1628, FRAME = 0xFF8A5A2B, LINK = 0x99B8764A, NODE_FILL = 0xFF3A2D4A, NODE_HOVER = 0xFF5A4670, TAB_ON = 0xFF5A4670, TAB_TEXT = 0xFFE2D4AC; // TAB_W is the tab strip; tabs overlap the map frame by 2 px, as creative tabs do
    private static double panX, panY; // kept between openings

    private final ResearchPanel panel;
    private final List<ResearchMap.Tab> tabs = ResearchMap.tabs();
    private int tab;
    private List<ResearchMap.Node> nodes = tabs.get(0).nodes();
    private boolean reading;
    private double dragged;

    public ResearchScreen() {
        super(Component.translatable("item.enchantmod.research_tablet"));
        panel = new ResearchPanel(Minecraft.getInstance().font);
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
        if (reading) {
            int h = Math.min(height - 20, 200);
            panel.render(graphics, (width - ResearchPanel.WIDTH) / 2, (height - h) / 2, h);
            return;
        }
        graphics.fill(TAB_W - 4, 4, width - 4, height - 4, FRAME);
        graphics.fill(TAB_W - 2, 6, width - 6, height - 6, BACKDROP);
        graphics.enableScissor(TAB_W - 2, 6, width - 6, height - 6);
        int runes = Rune.values().length;
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
            graphics.fill(nx - 1, ny - 1, nx + NODE + 1, ny + NODE + 1, FRAME);
            graphics.fill(nx, ny, nx + NODE, ny + NODE, node == hovered ? NODE_HOVER : NODE_FILL);
            if (node.page() < runes) glyph(graphics, Rune.values()[node.page()], nx + NODE / 2.0F, ny + NODE / 2.0F);
            else graphics.renderItem(new ItemStack(Items.ENCHANTED_BOOK), nx + 3, ny + 3);
        }
        graphics.disableScissor();
        for (int i = 0; i < tabs.size(); i++) {
            int ty = tabY(i), right = i == tab ? TAB_W : TAB_W - 4; // the open tab joins the map
            graphics.fill(2, ty, right, ty + TAB_H, FRAME);
            graphics.fill(3, ty + 1, right - (i == tab ? 0 : 1), ty + TAB_H - 1, i == tab ? BACKDROP : NODE_FILL);
            graphics.renderItem(iconOf(tabs.get(i).key()), 7, ty + 6);
        }
        int hoveredTab = tabAt(mouseX, mouseY);
        if (hoveredTab >= 0) graphics.renderTooltip(font, Component.translatable("gui.enchantmod.research.tab." + tabs.get(hoveredTab).key()), mouseX, mouseY);
        if (hovered != null) graphics.renderTooltip(font, nameOf(hovered), mouseX, mouseY);
    }

    private int tabY(int i) {
        return 12 + i * (TAB_H + 2);
    }

    private int tabAt(double mouseX, double mouseY) {
        if (reading || mouseX < 2 || mouseX >= TAB_W) return -1;
        for (int i = 0; i < tabs.size(); i++) if (mouseY >= tabY(i) && mouseY < tabY(i) + TAB_H) return i;
        return -1;
    }

    private static ItemStack iconOf(String key) {
        return new ItemStack(switch (key) {
            case "tools" -> Items.DIAMOND_PICKAXE;
            case "weapons" -> Items.DIAMOND_SWORD;
            case "armor" -> Items.DIAMOND_CHESTPLATE;
            case "bows" -> Items.BOW;
            default -> ModItems.ENCHANT_TEMPLATE.get();
        });
    }

    private static Component nameOf(ResearchMap.Node node) {
        int runes = Rune.values().length;
        if (node.page() < runes) return Component.translatable("rune.enchantmod." + Rune.values()[node.page()].id());
        List<EnchantForecast.Core> table = EnchantForecast.table();
        return Component.translatable("enchantment.minecraft." + table.get(node.page() - runes).enchant());
    }

    private void glyph(GuiGraphics graphics, Rune rune, float cx, float cy) {
        String text = String.valueOf(rune.glyph());
        graphics.pose().pushPose();
        graphics.pose().translate(cx, cy, 0);
        graphics.pose().scale(1.5F, 1.5F, 1.0F);
        graphics.pose().translate(-font.width(text) / 2.0F, -4.0F, 0);
        graphics.drawString(font, text, 0, 0, ScribingTableScreen.glyphColor(rune), false);
        graphics.pose().popPose();
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
        if (reading) {
            if (button == 1) { reading = false; return true; }
            return panel.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
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
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return reading && panel.mouseScrolled(mouseX, mouseY, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
