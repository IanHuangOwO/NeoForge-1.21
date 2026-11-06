package com.mamechin.enchantmod.item;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;
import com.mamechin.enchantmod.item.ink.GlowingLapisInkItem;
import com.mamechin.enchantmod.item.ink.LapisInkItem;
import com.mamechin.enchantmod.item.quill.CopperQuillItem;
import com.mamechin.enchantmod.item.quill.GoldenQuillItem;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnchantMod.MODID);

    // Add a tier = new class in item/quill or item/ink + one line here + texture/model/lang/recipe + a tag entry
    public static final DeferredItem<CopperQuillItem> COPPER_QUILL = ITEMS.registerItem("copper_quill", CopperQuillItem::new);
    public static final DeferredItem<LapisInkItem> LAPIS_INK = ITEMS.registerItem("lapis_ink", LapisInkItem::new);
    public static final DeferredItem<GoldenQuillItem> GOLDEN_QUILL = ITEMS.registerItem("golden_quill", GoldenQuillItem::new);
    public static final DeferredItem<GlowingLapisInkItem> GLOWING_LAPIS_INK = ITEMS.registerItem("glowing_lapis_ink", GlowingLapisInkItem::new);
    public static final DeferredItem<BlockItem> SCRIBING_TABLE = ITEMS.registerSimpleBlockItem("scribing_table", ModBlocks.SCRIBING_TABLE);
    public static final DeferredItem<BlockItem> CALCITE_CAULDRON = ITEMS.registerSimpleBlockItem("calcite_cauldron", ModBlocks.CALCITE_CAULDRON);

    private ModItems() {}
}
