package com.mamechin.enchantmod.item;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;
import com.mamechin.enchantmod.item.inkwell.InkwellTier1Item;
import com.mamechin.enchantmod.item.inkwell.InkwellTier0Item;
import com.mamechin.enchantmod.item.inkwell.InkwellTier2Item;
import com.mamechin.enchantmod.item.quill.QuillTier0Item;
import com.mamechin.enchantmod.item.quill.QuillTier1Item;
import com.mamechin.enchantmod.item.quill.QuillTier2Item;

import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnchantMod.MODID);

    // Add a tier = new class in item/quill or item/ink + one line here + texture/model/lang/recipe + a tag entry
    public static final DeferredItem<QuillTier0Item> QUILL_TIER0 = ITEMS.registerItem("quill_tier0", QuillTier0Item::new);
    public static final DeferredItem<InkwellTier0Item> INKWELL_TIER0 = ITEMS.registerItem("inkwell_tier0", InkwellTier0Item::new);
    public static final DeferredItem<QuillTier1Item> QUILL_TIER1 = ITEMS.registerItem("quill_tier1", QuillTier1Item::new);
    public static final DeferredItem<InkwellTier1Item> INKWELL_TIER1 = ITEMS.registerItem("inkwell_tier1", InkwellTier1Item::new);
    public static final DeferredItem<QuillTier2Item> QUILL_TIER2 = ITEMS.registerItem("quill_tier2", QuillTier2Item::new);
    public static final DeferredItem<InkwellTier2Item> INKWELL_TIER2 = ITEMS.registerItem("inkwell_tier2", InkwellTier2Item::new);
    public static final DeferredItem<BlockItem> SCRIBING_TABLE = ITEMS.registerSimpleBlockItem("scribing_table", ModBlocks.SCRIBING_TABLE);
    public static final DeferredItem<BlockItem> SMALL_CAULDRON = ITEMS.registerSimpleBlockItem("small_cauldron", ModBlocks.SMALL_CAULDRON);

    private ModItems() {}
}
