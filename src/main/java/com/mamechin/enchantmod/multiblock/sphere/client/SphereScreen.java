package com.mamechin.enchantmod.multiblock.sphere.client;

import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.registry.ModDataComponents;
import com.mamechin.enchantmod.multiblock.sphere.SphereMenu;
import com.mamechin.enchantmod.items.yggdrasil.TreeData;
import com.mamechin.enchantmod.items.yggdrasil.YggdrasilItem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The glass sphere screen: the tree slot and a readout of the tree in it (owner, age, stage, refill cost and pace). Drawn with plain
 * fills in vanilla's grey until it gets its own art.
 */
public class SphereScreen extends AbstractContainerScreen<SphereMenu> {
    private static final int PANEL = 0xFFC6C6C6, LIGHT = 0xFFFFFFFF, DARK = 0xFF555555, SLOT = 0xFF8B8B8B, TEXT = 0xFF404040;

    public SphereScreen(SphereMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = SphereMenu.INVENTORY_Y + 82;
        inventoryLabelY = SphereMenu.INVENTORY_Y - 11;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, DARK);
        graphics.fill(x, y, x + imageWidth - 1, y + imageHeight - 1, LIGHT);
        graphics.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        for (Slot slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            graphics.fill(sx, sy, sx + 18, sy + 18, LIGHT);
            graphics.fill(sx, sy, sx + 17, sy + 17, DARK);
            graphics.fill(sx + 1, sy + 1, sx + 17, sy + 17, SLOT);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, TEXT, false);
        graphics.drawString(font, playerInventoryTitle, 8, inventoryLabelY, TEXT, false);
        int x = SphereMenu.TREE_X + 26, y = 20;
        ItemStack stack = menu.tree();
        TreeData data = stack.get(ModDataComponents.TREE);
        if (data == null) {
            graphics.drawWordWrap(font, Component.translatable("container.enchantmod.sphere.empty"), x, y, imageWidth - x - 8, TEXT);
            return;
        }
        Balance.Stage stage = YggdrasilItem.stage(stack);
        Component[] lines = {
                stack.getHoverName(),
                Component.translatable("item.enchantmod.yggdrasil.owner", data.ownerName()),
                Component.translatable("item.enchantmod.yggdrasil.age", data.age(), data.growth() * 100 / Balance.current().yggdrasil().ticksPerYear()),
                Component.translatable("container.enchantmod.sphere.refill", stage.xpPerLevel()),
                Component.translatable("container.enchantmod.sphere.check", stage.checkSeconds())};
        for (Component line : lines) {
            graphics.drawString(font, line, x, y, TEXT, false);
            y += 11;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
