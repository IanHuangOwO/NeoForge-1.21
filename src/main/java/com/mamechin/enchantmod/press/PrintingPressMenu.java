package com.mamechin.enchantmod.press;

import com.mamechin.enchantmod.blocks.ModBlocks;
import com.mamechin.enchantmod.scribing.ModMenus;
import com.mamechin.enchantmod.scribing.rules.PressLayout;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The printing press: a cube slot in the middle and six template slots round it in a ring, of which the cube's tier opens some (PressLayout). */
public class PrintingPressMenu extends AbstractContainerMenu {
    public static final int CENTRE_X = 80, CENTRE_Y = 46, INVENTORY_Y = 114;
    /** Where each ring slot sits relative to the cube slot, clockwise from the top. */
    public static final int[][] RING_OFFSET = {{0, -34}, {29, -17}, {29, 17}, {0, 34}, {-29, 17}, {-29, -17}};

    private final ItemStackHandler inventory;
    private final ContainerLevelAccess access;

    /** Client side: the block entity is read from the position the server sent. */
    public PrintingPressMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, clientInventory(playerInventory, buffer), ContainerLevelAccess.NULL);
    }

    private static ItemStackHandler clientInventory(Inventory playerInventory, FriendlyByteBuf buffer) {
        var be = playerInventory.player.level().getBlockEntity(buffer.readBlockPos());
        return be instanceof PrintingPressBlockEntity press ? press.inventory() : new ItemStackHandler(1 + PrintingPressBlockEntity.TEMPLATES);
    }

    public PrintingPressMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access) {
        super(ModMenus.PRINTING_PRESS.get(), id);
        this.inventory = inventory;
        this.access = access;

        addSlot(new SlotItemHandler(inventory, PrintingPressBlockEntity.CUBE, CENTRE_X, CENTRE_Y));
        for (int i = 1; i <= PrintingPressBlockEntity.TEMPLATES; i++) {
            final int ring = i - 1;
            addSlot(new SlotItemHandler(inventory, i, CENTRE_X + RING_OFFSET[ring][0], CENTRE_Y + RING_OFFSET[ring][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isOpen(ring) && super.mayPlace(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, INVENTORY_Y + 58));
    }

    /** True when ring slot {@code ring} (0 = top, clockwise) is open for the cube in the press. */
    public boolean isOpen(int ring) {
        return PressLayout.isOpen(ring, capacity());
    }

    /** How many template slots the cube in the press opens. */
    public int capacity() {
        return PrintingPressBlockEntity.capacity(inventory.getStackInSlot(PrintingPressBlockEntity.CUBE));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int own = 1 + PrintingPressBlockEntity.TEMPLATES;
        boolean moved = index < own ? moveItemStackTo(stack, own, slots.size(), true) : moveItemStackTo(stack, 0, own, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.PRINTING_PRESS.get());
    }
}
