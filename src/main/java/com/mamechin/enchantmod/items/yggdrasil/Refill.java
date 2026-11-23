package com.mamechin.enchantmod.items.yggdrasil;

import java.util.List;
import java.util.Map;

import com.mamechin.enchantmod.items.books.Books;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** The Yggdrasil's book refill (author, 2026-11-23): one enchant of an enchanted book goes up one level. */
public final class Refill {
    private Refill() {}

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
