package com.mamechin.enchantmod.items.yggdrasil;

import java.util.List;
import java.util.Map;

import com.mamechin.enchantmod.items.books.Books;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * The Yggdrasil's book refill (author, 2026-11-23): one enchant of an enchanted book goes up one level, and a plain book (an empty one,
 * or one the altar drained) gets one random enchant from the enchanting table's pool at level I.
 */
public final class Refill {
    private Refill() {}

    /**
     * A plain book of any tier turned into that tier's enchanted book holding one random enchanting-table enchant at level I, or
     * {@link ItemStack#EMPTY} when {@code book} is not a plain book or the pool is empty. The caller puts the result in the book's place.
     */
    public static ItemStack enchantPlain(ItemStack book, RegistryAccess registries, RandomSource random) {
        if (!Books.isPlainBook(book)) return ItemStack.EMPTY;
        var pool = registries.registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
        var pick = pool.flatMap(tag -> tag.getRandomElement(random));
        if (pick.isEmpty()) return ItemStack.EMPTY;
        ItemStack enchanted = book.transmuteCopy(Books.enchanted(Books.tier(book)), 1);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        stored.set(pick.get(), 1);
        enchanted.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        return enchanted;
    }

    /**
     * Raises one random enchant of {@code book}, in place, by one level, when the book is below its level cap ({@link Books#cap}) and
     * the enchant below its maximum. Plain books and full books are left alone. Returns whether a level was added.
     */
    public static boolean raise(ItemStack book, RandomSource random) {
        if (!Books.isEnchantedBook(book)) return false;
        ItemEnchantments stored = book.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored == null || stored.isEmpty()) return false;
        int cap = Books.cap(book);
        if (cap >= 0 && Books.totalLevels(stored) >= cap) return false;
        List<Holder<Enchantment>> open = stored.entrySet().stream().filter(e -> e.getIntValue() < e.getKey().value().getMaxLevel()).map(Map.Entry::getKey).toList();
        if (open.isEmpty()) return false;
        Holder<Enchantment> pick = open.get(random.nextInt(open.size()));
        ItemEnchantments.Mutable raised = new ItemEnchantments.Mutable(stored);
        raised.set(pick, stored.getLevel(pick) + 1);
        book.set(DataComponents.STORED_ENCHANTMENTS, raised.toImmutable());
        return true;
    }
}
