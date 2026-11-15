package com.mamechin.enchantmod.scribing;

import com.mamechin.enchantmod.items.EnchantTemplate;
import com.mamechin.enchantmod.items.ModItems;

import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The template store of a research handbook: a small chest that only takes enchant templates. The contents live in the
 * handbook's data component and are written back on every change. The handbook itself cannot be picked up while it is open.
 */
public class HandbookStoreMenu extends AbstractContainerMenu {
    public static final int ROWS = 3, SLOTS = ROWS * 9;

    private final ItemStack book; // empty on the client, which only mirrors the slots
    private final ItemStackHandler store = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!book.isEmpty()) book.set(ModDataComponents.TEMPLATE_STORE.get(), ItemContainerContents.fromItems(stacks));
        }
    };

    public HandbookStoreMenu(int id, Inventory inventory) {
        this(id, inventory, ItemStack.EMPTY);
    }

    public HandbookStoreMenu(int id, Inventory inventory, ItemStack book) {
        super(ModMenus.HANDBOOK_STORE.get(), id);
        this.book = book;
        if (!book.isEmpty()) {
            NonNullList<ItemStack> stored = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
            book.getOrDefault(ModDataComponents.TEMPLATE_STORE.get(), ItemContainerContents.EMPTY).copyInto(stored);
            for (int i = 0; i < SLOTS; i++) store.setStackInSlot(i, stored.get(i));
        }
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(store, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof EnchantTemplate;
                    }
                });
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(locked(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(locked(inventory, col, 8 + col * 18, 142));
    }

    private Slot locked(Inventory inventory, int index, int x, int y) {
        return new Slot(inventory, index, x, y) {
            @Override
            public boolean mayPickup(Player player) {
                return getItem() != book;
            }
        };
    }

    /** The handbook in the inventory or the off hand, or an empty stack. */
    public static ItemStack handbook(Player player) {
        for (ItemStack stack : player.getInventory().items) if (stack.is(ModItems.RESEARCH_HANDBOOK.get())) return stack;
        for (ItemStack stack : player.getInventory().offhand) if (stack.is(ModItems.RESEARCH_HANDBOOK.get())) return stack;
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < SLOTS ? moveItemStackTo(stack, SLOTS, slots.size(), true) : moveItemStackTo(stack, 0, SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return book.isEmpty() || handbook(player) == book;
    }
}
