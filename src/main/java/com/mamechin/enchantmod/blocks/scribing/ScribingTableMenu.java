package com.mamechin.enchantmod.blocks.scribing;

import com.mamechin.enchantmod.registry.ModTags;
import com.mamechin.enchantmod.registry.ModDataComponents;
import com.mamechin.enchantmod.registry.ModMenus;
import com.mamechin.enchantmod.enchanting.Blueprint;
import com.mamechin.enchantmod.enchanting.BlueprintRules;
import java.util.List;

import com.mamechin.enchantmod.registry.ModBlocks;
import com.mamechin.enchantmod.registry.ModItems;
import com.mamechin.enchantmod.items.inkwell.EnchantInkwell;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The scribing table screen: three table slots, the player inventory, and the edits sent by the screen. */
public class ScribingTableMenu extends AbstractContainerMenu {

    public static final int OP_PLACE = 0, OP_ERASE = 1, OP_LINE = 2;

    private final ItemStackHandler inventory;
    private final ContainerLevelAccess access;

    /** Client side: the block entity is read from the position the server sent. */
    public ScribingTableMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, clientInventory(playerInventory, buffer), ContainerLevelAccess.NULL);
    }

    private static ItemStackHandler clientInventory(Inventory playerInventory, FriendlyByteBuf buffer) {
        var be = playerInventory.player.level().getBlockEntity(buffer.readBlockPos());
        return be instanceof ScribingTableBlockEntity table ? table.inventory() : new ItemStackHandler(3);
    }

    public ScribingTableMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access) {
        super(ModMenus.SCRIBING_TABLE.get(), id);
        this.inventory = inventory;
        this.access = access;

        addSlot(new SlotItemHandler(inventory, ScribingTableBlockEntity.INKWELL, 8, 18) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.INKWELLS);
            }
        });
        addSlot(new SlotItemHandler(inventory, ScribingTableBlockEntity.QUILL, 8, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.QUILLS);
            }
        });
        addSlot(new SlotItemHandler(inventory, ScribingTableBlockEntity.TEMPLATE, 8, 62) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.ENCHANT_TEMPLATE.get());
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 47 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 47 + col * 18, 198));
        }
    }

    /** What is drawn on the template in the table (empty when there is none). */
    public Blueprint blueprint() {
        return inventory.getStackInSlot(ScribingTableBlockEntity.TEMPLATE).getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY);
    }

    public ItemStack templateStack() {
        return inventory.getStackInSlot(ScribingTableBlockEntity.TEMPLATE);
    }

    /** Server side: names the template in the table; an empty name takes the name off. */
    public void rename(String name) {
        if (templateStack().isEmpty()) return;
        String clean = name.strip();
        ItemStack template = templateStack().copy();
        if (clean.isEmpty()) template.remove(DataComponents.CUSTOM_NAME);
        else template.set(DataComponents.CUSTOM_NAME, Component.literal(clean.substring(0, Math.min(clean.length(), RenameTemplatePayload.NAME_MAX))));
        inventory.setStackInSlot(ScribingTableBlockEntity.TEMPLATE, template);
        broadcastChanges();
    }

    /** Ink left in the inkwell in the table: how many more cells can be drawn. */
    public int ink() {
        return EnchantInkwell.remaining(inventory.getStackInSlot(ScribingTableBlockEntity.INKWELL));
    }

    /** Editing needs an inkwell, a quill and a template in the table; every cell drawn wears the inkwell down. */
    public boolean canEdit() {
        return !inventory.getStackInSlot(ScribingTableBlockEntity.INKWELL).isEmpty()
                && !inventory.getStackInSlot(ScribingTableBlockEntity.QUILL).isEmpty()
                && !inventory.getStackInSlot(ScribingTableBlockEntity.TEMPLATE).isEmpty();
    }

    /** Server side: validates and applies one edit sent by the screen. */
    public void applyEdit(int op, int cell, int cell2, String rune, List<Integer> path) {
        if (!canEdit()) return;
        Blueprint current = blueprint();
        Blueprint next = switch (op) {
            case OP_PLACE -> BlueprintRules.place(current, cell, rune);
            case OP_ERASE -> BlueprintRules.erase(current, cell);
            case OP_LINE -> BlueprintRules.addLine(current, cell, cell2, path);
            default -> null;
        };
        if (next == null) return;
        int cost = BlueprintRules.cost(current, next);
        if (cost > ink()) return; // not enough ink left; the inkwell never breaks, it just runs dry
        // write through the handler so the block entity is marked dirty and the edit is saved
        ItemStack template = inventory.getStackInSlot(ScribingTableBlockEntity.TEMPLATE).copy();
        template.set(ModDataComponents.BLUEPRINT.get(), next);
        inventory.setStackInSlot(ScribingTableBlockEntity.TEMPLATE, template);
        if (cost > 0) {
            ItemStack inkwell = inventory.getStackInSlot(ScribingTableBlockEntity.INKWELL).copy();
            EnchantInkwell.spend(inkwell, cost);
            inventory.setStackInSlot(ScribingTableBlockEntity.INKWELL, inkwell);
        }
        broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < 3 ? moveItemStackTo(stack, 3, slots.size(), true) : moveItemStackTo(stack, 0, 3, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModBlocks.SCRIBING_TABLE.get());
    }
}
