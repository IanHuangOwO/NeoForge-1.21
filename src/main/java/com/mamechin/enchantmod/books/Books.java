package com.mamechin.enchantmod.books;

import com.mamechin.enchantmod.Config;
import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.items.ModItems;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The three book tiers (author, 2026-11-22): tier 0 is the vanilla book and enchanted book, tiers 1 and 2 are ours. Each enchanted
 * book stores its enchants in {@code STORED_ENCHANTMENTS} like the vanilla one; the level cap per tier is in {@code items/books.json}.
 */
public final class Books {
    private Books() {}

    public static Item plain(int tier) {
        return switch (tier) {
            case 1 -> ModItems.BOOK_TIER1.get();
            case 2 -> ModItems.BOOK_TIER2.get();
            default -> Items.BOOK;
        };
    }

    public static Item enchanted(int tier) {
        return switch (tier) {
            case 1 -> ModItems.ENCHANTED_BOOK_TIER1.get();
            case 2 -> ModItems.ENCHANTED_BOOK_TIER2.get();
            default -> Items.ENCHANTED_BOOK;
        };
    }

    /** The tier of a plain or enchanted book, or -1 for anything else. */
    public static int tier(ItemStack stack) {
        for (int tier = 0; tier <= 2; tier++) if (stack.is(plain(tier)) || stack.is(enchanted(tier))) return tier;
        return -1;
    }

    public static boolean isPlainBook(ItemStack stack) {
        int tier = tier(stack);
        return tier >= 0 && stack.is(plain(tier));
    }

    public static boolean isEnchantedBook(ItemStack stack) {
        int tier = tier(stack);
        return tier >= 0 && stack.is(enchanted(tier));
    }

    /** The level cap of an enchanted book, or -1 when it is not one or the limit is off (in the mod config or in {@code books.json}). */
    public static int cap(ItemStack stack) {
        if (!isEnchantedBook(stack) || (Config.SPEC.isLoaded() && !Config.BOOK_LIMIT_ENABLED.get())) return -1;
        return Balance.current().books().cap(tier(stack));
    }

    public static int totalLevels(ItemEnchantments enchants) {
        int sum = 0;
        for (var e : enchants.entrySet()) sum += e.getIntValue();
        return sum;
    }

    /** Cuts an enchanted book's stored enchants down to its cap, in place ({@link BookLimit#trim}). Anything else is left alone. */
    public static void trim(ItemStack stack) {
        int cap = cap(stack);
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (cap < 0 || stored == null || totalLevels(stored) <= cap) return;
        var entries = stored.entrySet().stream().toList();
        int[] levels = BookLimit.trim(entries.stream().mapToInt(e -> e.getIntValue()).toArray(), cap);
        ItemEnchantments.Mutable out = new ItemEnchantments.Mutable(stored);
        for (int i = 0; i < levels.length; i++) out.set(entries.get(i).getKey(), levels[i]); // 0 removes it
        stack.set(DataComponents.STORED_ENCHANTMENTS, out.toImmutable());
    }
}
