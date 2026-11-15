package com.mamechin.enchantmod.scribing;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.items.EnchantTemplate;
import com.mamechin.enchantmod.items.ModItems;
import com.mamechin.enchantmod.scribing.rules.TemplateStore;

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
 * The number of slots grows in steps of nine (see TemplateStore); the client is told it when the menu opens.
 */
public class HandbookStoreMenu extends AbstractContainerMenu {
    private final ItemStack book; // empty on the client, which only mirrors the slots
    private final ItemStackHandler store;
    private final int rows;

    /** What the handbook allows, before the slots in use are counted. Later: plus one step per level of an expanding enchantment. */
    static int capacity(ItemStack book) {
        return TemplateStore.BASE;
    }

    /** How many slots the store of this handbook shows. */
    public static int slotsFor(ItemStack book) {
        ItemContainerContents contents = book.getOrDefault(ModDataComponents.TEMPLATE_STORE.get(), ItemContainerContents.EMPTY);
        int used = 0;
        for (int i = 0; i < contents.getSlots(); i++) if (!contents.getStackInSlot(i).isEmpty()) used = i + 1;
        return TemplateStore.slots(capacity(book), used);
    }

    public HandbookStoreMenu(int id, Inventory inventory, ItemStack book) {
        this(id, inventory, book, slotsFor(book));
    }

    public HandbookStoreMenu(int id, Inventory inventory, ItemStack book, int slots) {
        super(ModMenus.HANDBOOK_STORE.get(), id);
        this.book = book;
        this.rows = slots / TemplateStore.STEP;
        this.store = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                if (book.isEmpty()) return;
                int used = 0;
                for (int i = 0; i < stacks.size(); i++) if (!stacks.get(i).isEmpty()) used = i + 1;
                List<ItemStack> kept = new ArrayList<>(stacks.subList(0, used)); // trailing empty slots are not stored
                book.set(ModDataComponents.TEMPLATE_STORE.get(), ItemContainerContents.fromItems(kept));
            }
        };
        ItemContainerContents contents = book.getOrDefault(ModDataComponents.TEMPLATE_STORE.get(), ItemContainerContents.EMPTY);
        for (int i = 0; i < Math.min(slots, contents.getSlots()); i++) store.setStackInSlot(i, contents.getStackInSlot(i).copy());

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(store, col + row * 9, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return stack.getItem() instanceof EnchantTemplate;
                    }
                });
            }
        }
        int inventoryY = inventoryY(rows);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(locked(inventory, col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(locked(inventory, col, 8 + col * 18, inventoryY + 58));
    }

    /** Top of the player inventory for a store of that many rows. */
    public static int inventoryY(int rows) {
        return 18 + rows * 18 + 14;
    }

    public int rows() {
        return rows;
    }

    public int storeSlots() {
        return rows * 9;
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
        int own = storeSlots();
        boolean moved = index < own ? moveItemStackTo(stack, own, slots.size(), true) : moveItemStackTo(stack, 0, own, false);
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
