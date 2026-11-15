package com.mamechin.enchantmod.press.client;

import com.mamechin.enchantmod.press.PrintingPressMenu;
import com.mamechin.enchantmod.scribing.research.GuiArt;
import com.mamechin.enchantmod.scribing.research.NineSlice;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The printing press screen, drawn with the handbook map's frame, background and slot art; slots the cube does not open are not drawn. */
public class PrintingPressScreen extends AbstractContainerScreen<PrintingPressMenu> {
    private static final int TEXT = 0xFFE2D4AC;
    private static final ResourceLocation BACKGROUND = GuiArt.research("map_bg"), FRAME = GuiArt.research("map_frame"),
            SLOT = GuiArt.gui("palette_slot"), GHOST = GuiArt.gui("slot_template");

    public PrintingPressScreen(PrintingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 134;
        inventoryLabelY = 40;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        NineSlice.tile(graphics, BACKGROUND, leftPos + 2, topPos + 2, imageWidth - 4, imageHeight - 4, 0, 0, 16, 16, 16);
        NineSlice.draw(graphics, FRAME, leftPos, topPos, imageWidth, imageHeight, 32, 6, false);
        for (Slot slot : menu.slots) {
            if (!slot.isActive()) continue;
            graphics.blit(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 18, 14, 14);
            if (slot.index >= 1 && slot.index <= 6 && !slot.hasItem()) graphics.blit(GHOST, leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 7, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
