package com.mamechin.enchantmod.items.books;

import java.util.List;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * A plain book of tier 1 or 2. Like the vanilla book, the enchanting table and the quill can enchant it with any enchantment; it then
 * becomes the tier's enchanted book, trimmed to the tier's level cap.
 */
public class TierBook extends Item {
    private final int tier;

    public TierBook(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return stack.getCount() == 1;
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
        return true;
    }

    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
        return true;
    }

    @Override
    public ItemStack applyEnchantments(ItemStack stack, List<EnchantmentInstance> enchantments) {
        ItemStack out = stack.transmuteCopy(Books.enchanted(tier), 1);
        ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        for (EnchantmentInstance e : enchantments) stored.upgrade(e.enchantment, e.level);
        out.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
        Books.trim(out);
        return out;
    }
}
