package com.mamechin.enchantmod.multiblock.sphere;

import com.mamechin.enchantmod.registry.ModBlocks;
import com.mamechin.enchantmod.registry.ModMenus;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The glass sphere: one slot for a bound Yggdrasil; the screen reads the tree's numbers from the stack. */
public class SphereMenu extends AbstractContainerMenu {
    public static final int TREE_X = 26, TREE_Y = 35, INVENTORY_Y = 84;

    private final ContainerLevelAccess access;

    /** Client side: the block entity is read from the position the server sent. */
    public SphereMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, playerInventory.player.level().getBlockEntity(buffer.readBlockPos()) instanceof SphereCoreBlockEntity core ? core.tree() : new ItemStackHandler(1),
                ContainerLevelAccess.NULL);
    }

    public SphereMenu(int id, Inventory playerInventory, ItemStackHandler tree, ContainerLevelAccess access) {
        super(ModMenus.SPHERE.get(), id);
        this.access = access;
        addSlot(new SlotItemHandler(tree, 0, TREE_X, TREE_Y));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(playerInventory, col, 8 + col * 18, INVENTORY_Y + 58));
    }

    public ItemStack tree() {
        return slots.get(0).getItem();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index == 0 ? moveItemStackTo(stack, 1, slots.size(), true) : moveItemStackTo(stack, 0, 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SPHERE_CORE.get());
    }
}
