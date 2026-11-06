package com.mamechin.enchantmod.cauldron.brewing;

import java.util.List;
import java.util.stream.IntStream;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.items.IItemHandler;

/** Snapshot (no copies) of the cauldron's stored stacks, in slot order. */
public record CauldronInput(List<ItemStack> stacks) implements RecipeInput {
    public static CauldronInput of(IItemHandler inv) {
        return new CauldronInput(IntStream.range(0, inv.getSlots()).mapToObj(inv::getStackInSlot).toList());
    }

    @Override
    public ItemStack getItem(int index) {
        return stacks.get(index);
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
