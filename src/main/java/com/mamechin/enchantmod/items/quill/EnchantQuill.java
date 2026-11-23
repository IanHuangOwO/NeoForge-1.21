package com.mamechin.enchantmod.items.quill;

import java.util.Optional;

import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.blocks.altar.AltarBlock;
import com.mamechin.enchantmod.items.books.Books;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;

/** Base for every quill tier. Behavior shared by all quills goes here. */
public abstract class EnchantQuill extends Item {
    /**
     * What the quill does to a book: the vanilla enchanting power it rolls with (the "levels" of an enchanting table) and the
     * experience levels it costs the player.
     */
    public record BookRite(int power, int xpLevels) {}

    private final BookRite bookRite;

    protected EnchantQuill(Properties properties, BookRite bookRite) {
        super(properties);
        this.bookRite = bookRite;
    }

    /** Vanilla skips a block's click when the player sneaks with an item in hand; the altar needs shift and a quill to scan. */
    @Override
    public boolean doesSneakBypassUse(ItemStack stack, LevelReader level, BlockPos pos, Player player) {
        return level.getBlockState(pos).getBlock() instanceof AltarBlock;
    }

    /** The rite for this stack, from {@code quills.json} by item id, else the tier class's numbers; the one place to read a custom enchant on the quill when there is one. */
    public BookRite bookRite(ItemStack quill) {
        Balance.Quill q = Balance.current().quill(BuiltInRegistries.ITEM.getKey(this).toString(), new Balance.Quill(bookRite.power(), bookRite.xpLevels()));
        return new BookRite(q.power(), q.xpLevels());
    }

    /** Spend one use of the given stack. Breaks it at 0 durability. */
    public void wear(ItemStack stack, LivingEntity user, EquipmentSlot slot) {
        stack.hurtAndBreak(1, user, slot);
    }

    /**
     * Quill in the main hand, a plain book of any tier in the other: spends experience and turns one book into that tier's enchanted
     * book, rolled like the vanilla table and trimmed to the tier's level cap.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack quill = player.getItemInHand(hand);
        ItemStack book = player.getOffhandItem();
        if (hand != InteractionHand.MAIN_HAND || !Books.isPlainBook(book)) return InteractionResultHolder.pass(quill);
        BookRite rite = bookRite(quill);
        boolean free = player.hasInfiniteMaterials();
        if (!free && player.experienceLevel < rite.xpLevels()) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.enchantmod.quill.need_xp", rite.xpLevels()), true);
            return InteractionResultHolder.fail(quill);
        }
        if (!level.isClientSide) {
            Optional<HolderSet.Named<Enchantment>> table = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.IN_ENCHANTING_TABLE);
            ItemStack single = book.copyWithCount(1);
            var rolled = EnchantmentHelper.selectEnchantment(player.getRandom(), single, rite.power(),
                    table.map(HolderSet::stream).orElseGet(() -> level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().map(h -> (Holder<Enchantment>) h)));
            ItemStack result = rolled.isEmpty() ? single : single.getItem().applyEnchantments(single, rolled); // each tier's book turns into its own enchanted book
            Books.trim(result);
            if (!free) {
                book.shrink(1);
                player.giveExperienceLevels(-rite.xpLevels());
            }
            if (!player.getInventory().add(result)) player.drop(result, false);
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        return InteractionResultHolder.sidedSuccess(quill, level.isClientSide);
    }
}
