package com.mamechin.enchantmod.scribing.research;

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
 * The template store: a small chest for enchant templates. The search box dims every template whose name, and whose enchants'
 * names, do not contain the text. Placeholder look: the vanilla chest texture.
 */
public class HandbookStoreScreen extends AbstractContainerScreen<HandbookStoreMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private EditBox search;
    private final boolean[] dimmed = new boolean[HandbookStoreMenu.SLOTS];
    private final ItemStack[] seen = new ItemStack[HandbookStoreMenu.SLOTS]; // what each dimmed flag was worked out for
    private String seenQuery = "";

    public HandbookStoreScreen(HandbookStoreMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 114 + HandbookStoreMenu.ROWS * 18;
        inventoryLabelY = imageHeight - 94;
        java.util.Arrays.fill(seen, ItemStack.EMPTY);
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 62, topPos + 4, 106, 11, Component.translatable("gui.enchantmod.research.search"));
        search.setHint(Component.translatable("gui.enchantmod.research.search").withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(search);
    }

    /** Works the dim flags out again for the slots whose item, or the search text, changed. */
    @Override
    protected void containerTick() {
        super.containerTick();
        String query = search.getValue();
        for (int i = 0; i < HandbookStoreMenu.SLOTS; i++) {
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
        if (slot.index < HandbookStoreMenu.SLOTS && dimmed[slot.index]) graphics.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0xC0000000);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int rows = HandbookStoreMenu.ROWS;
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, rows * 18 + 17);
        graphics.blit(TEXTURE, leftPos, topPos + rows * 18 + 17, 0, 126, imageWidth, 96);
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
