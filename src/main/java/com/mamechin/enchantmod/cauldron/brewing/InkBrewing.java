package com.mamechin.enchantmod.cauldron.brewing;

import java.util.List;
import java.util.function.Supplier;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Ingredient set (1 lapis + 1 ink) to inkwell tier. shortcut: hardcoded list, make it a data recipe type when addons need it. */
public final class InkBrewing {
    public record Recipe(Item ink, Supplier<? extends Item> inkwell, int minHeat) {}

    public static final List<Recipe> RECIPES = List.of(
            new Recipe(Items.INK_SAC, ModItems.LAPIS_INKWELL, 1),
            new Recipe(Items.GLOW_INK_SAC, ModItems.GLOWING_LAPIS_INKWELL, 3));

    public static boolean isIngredient(ItemStack stack) {
        return stack.is(Items.LAPIS_LAZULI) || RECIPES.stream().anyMatch(r -> stack.is(r.ink()));
    }

    /** First recipe whose lapis and ink are both in the inventory, or null. */
    public static Recipe match(IItemHandler inv) {
        for (Recipe r : RECIPES) {
            if (has(inv, r)) return r;
        }
        return null;
    }

    public static boolean has(IItemHandler inv, Recipe r) {
        return count(inv, Items.LAPIS_LAZULI) >= 1 && count(inv, r.ink()) >= 1;
    }

    /** The recipe that produces the given inkwell id, or null. */
    public static Recipe find(ResourceLocation inkwellId) {
        for (Recipe r : RECIPES) {
            if (BuiltInRegistries.ITEM.getKey(r.inkwell().get()).equals(inkwellId)) return r;
        }
        return null;
    }

    /** Takes 1 lapis and 1 of the recipe's ink out of the inventory. */
    public static void consume(IItemHandler inv, Recipe r) {
        take(inv, Items.LAPIS_LAZULI);
        take(inv, r.ink());
    }

    private static int count(IItemHandler inv, Item item) {
        int n = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            if (inv.getStackInSlot(i).is(item)) n += inv.getStackInSlot(i).getCount();
        }
        return n;
    }

    private static void take(IItemHandler inv, Item item) {
        for (int i = 0; i < inv.getSlots(); i++) {
            if (inv.getStackInSlot(i).is(item)) {
                inv.extractItem(i, 1, false);
                return;
            }
        }
    }

    private InkBrewing() {}
}
