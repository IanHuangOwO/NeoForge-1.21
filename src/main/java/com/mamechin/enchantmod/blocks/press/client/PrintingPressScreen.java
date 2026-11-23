package com.mamechin.enchantmod.blocks.press.client;

import com.mamechin.enchantmod.blocks.press.PrintingPressMenu;
import com.mamechin.enchantmod.client.gui.GuiArt;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** The printing press screen: the drawn panel with its ring of slots; slots the cube does not open get a riveted shutter. */
public class PrintingPressScreen extends AbstractContainerScreen<PrintingPressMenu> {
    private static final int TEXT = 0xFFE2D4AC;
    private static final ResourceLocation BACKGROUND = GuiArt.gui("press/printing_press"), LOCKED = GuiArt.gui("press/press_slot_locked"), GHOST = GuiArt.gui("scribing/slot_template");

    public PrintingPressScreen(PrintingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = PrintingPressMenu.INVENTORY_Y + 82;
        inventoryLabelY = PrintingPressMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        for (Slot slot : menu.slots) {
            if (slot.index >= 1 && slot.index <= 6 && !slot.hasItem() && menu.isOpen(slot.index - 1)) graphics.blit(GHOST, leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
        }
    }

    /** The template slots the cube does not open (all of them while there is no cube) are shuttered. */
    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (slot.index >= 1 && slot.index <= 6 && !menu.isOpen(slot.index - 1)) graphics.blit(LOCKED, slot.x - 1, slot.y - 1, 0, 0, 18, 18, 18, 18);
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
