package com.mamechin.enchantmod.scribing.research;

import java.util.Arrays;

import com.mamechin.enchantmod.scribing.HandbookStoreMenu;
import com.mamechin.enchantmod.scribing.ModDataComponents;
import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.TemplateSearch;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The template store: a small chest for enchant templates, drawn with the handbook map's frame, background and slot art. The
 * search box dims every template whose name, and whose enchants' names, do not contain the text.
 * shortcut: a store of more than six rows is taller than most screens, add a scrollbar when stores grow past 54 slots.
 */
public class HandbookStoreScreen extends AbstractContainerScreen<HandbookStoreMenu> {
    private static final int TEXT = 0xFFE2D4AC;
    private static final ResourceLocation BACKGROUND = GuiArt.research("map_bg"), FRAME = GuiArt.research("map_frame"),
            SLOT = GuiArt.gui("palette_slot"), GHOST = GuiArt.gui("slot_template"), FIELD = GuiArt.gui("rune_cell");

    private EditBox search;
    private final boolean[] dimmed;
    private final ItemStack[] seen; // what each dimmed flag was worked out for
    private String seenQuery = "";

    public HandbookStoreScreen(HandbookStoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = HandbookStoreMenu.inventoryY(menu.rows()) + 84;
        inventoryLabelY = HandbookStoreMenu.inventoryY(menu.rows()) - 11;
        dimmed = new boolean[menu.storeSlots()];
        seen = new ItemStack[menu.storeSlots()];
        Arrays.fill(seen, ItemStack.EMPTY);
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 66, topPos + 6, 100, 10, Component.translatable("gui.enchantmod.research.search"));
        search.setBordered(false); // the field is drawn in renderBg to match the other screens
        search.setTextColor(TEXT);
        search.setHint(Component.translatable("gui.enchantmod.research.search").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(search);
    }

    /** Works the dim flags out again for the slots whose item, or the search text, changed. */
    @Override
    protected void containerTick() {
        super.containerTick();
        String query = search.getValue();
        for (int i = 0; i < dimmed.length; i++) {
            ItemStack stack = menu.slots.get(i).getItem();
            if (ItemStack.matches(stack, seen[i]) && query.equals(seenQuery)) continue;
            seen[i] = stack.copy();
            Blueprint blueprint = stack.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY);
            dimmed[i] = !stack.isEmpty() && !TemplateSearch.matches(stack.getHoverName().getString(), blueprint, query, e -> Component.translatable(e.nameKey()).getString());
        }
        seenQuery = query;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        super.renderSlot(graphics, slot);
        if (slot.index < dimmed.length && dimmed[slot.index]) graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xC0000000);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        NineSlice.tile(graphics, BACKGROUND, leftPos + 2, topPos + 2, imageWidth - 4, imageHeight - 4, 0, 0, 16, 16, 16);
        NineSlice.draw(graphics, FRAME, leftPos, topPos, imageWidth, imageHeight, 32, 6, false);
        graphics.blit(FIELD, leftPos + 64, topPos + 4, 0, 0, 104, 14, 9, 9); // the search field
        for (Slot slot : menu.slots) {
            graphics.blit(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 18, 14, 14);
            if (slot.index < dimmed.length && !slot.hasItem()) graphics.blit(GHOST, leftPos + slot.x, topPos + slot.y, 0, 0, 16, 16, 16, 16);
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.isFocused() && keyCode != 256) { // typing must not trigger the inventory key
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
