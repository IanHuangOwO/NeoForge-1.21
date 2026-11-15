package com.mamechin.enchantmod.press;

import com.mamechin.enchantmod.blocks.ModBlocks;
import com.mamechin.enchantmod.scribing.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The printing press: a cube slot and six template slots, of which the cube's tier opens the first few. */
public class PrintingPressMenu extends AbstractContainerMenu {
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

        addSlot(new SlotItemHandler(inventory, PrintingPressBlockEntity.CUBE, 8, 20));
        for (int i = 1; i <= PrintingPressBlockEntity.TEMPLATES; i++) {
            final int index = i;
            addSlot(new SlotItemHandler(inventory, i, 44 + (i - 1) * 18, 20) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return index <= capacity() && super.mayPlace(stack);
                }

                /** A slot the cube does not open is hidden, unless a template is still in it so it can be taken out. */
                @Override
                public boolean isActive() {
                    return index <= capacity() || hasItem();
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 52 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, 110));
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
