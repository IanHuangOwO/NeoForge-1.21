package com.mamechin.enchantmod.items.books;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/** Trims every enchanted book in any generated loot (chests, fishing, mobs) to its level cap. */
public class TrimBooksModifier extends LootModifier {
    public static final MapCodec<TrimBooksModifier> CODEC = RecordCodecBuilder.mapCodec(inst -> codecStart(inst).apply(inst, TrimBooksModifier::new));

    public TrimBooksModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        loot.forEach(Books::trim);
        return loot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
