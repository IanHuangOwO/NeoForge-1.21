package com.mamechin.enchantmod.items;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.ModBlocks;
import com.mamechin.enchantmod.items.cube.CubeTier0;
import com.mamechin.enchantmod.items.cube.CubeTier1;
import com.mamechin.enchantmod.items.cube.CubeTier2;
import com.mamechin.enchantmod.items.inkwell.InkwellTier1;
import com.mamechin.enchantmod.items.inkwell.InkwellTier0;
import com.mamechin.enchantmod.items.inkwell.InkwellTier2;
import com.mamechin.enchantmod.items.quill.QuillTier0;
import com.mamechin.enchantmod.items.quill.QuillTier1;
import com.mamechin.enchantmod.items.quill.QuillTier2;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnchantMod.MODID);

    // Add a tier = new class in item/quill or item/ink + one line here + texture/model/lang/recipe + a tag entry
    public static final DeferredItem<QuillTier0> QUILL_TIER0 = ITEMS.registerItem("quill_tier0", QuillTier0::new);
    public static final DeferredItem<InkwellTier0> INKWELL_TIER0 = ITEMS.registerItem("inkwell_tier0", InkwellTier0::new);
    public static final DeferredItem<QuillTier1> QUILL_TIER1 = ITEMS.registerItem("quill_tier1", QuillTier1::new);
    public static final DeferredItem<InkwellTier1> INKWELL_TIER1 = ITEMS.registerItem("inkwell_tier1", InkwellTier1::new);
    public static final DeferredItem<QuillTier2> QUILL_TIER2 = ITEMS.registerItem("quill_tier2", QuillTier2::new);
    public static final DeferredItem<InkwellTier2> INKWELL_TIER2 = ITEMS.registerItem("inkwell_tier2", InkwellTier2::new);
    public static final DeferredItem<CubeTier0> ENCHANT_CUBE_TIER0 = ITEMS.registerItem("enchant_cube_tier0", CubeTier0::new);
    public static final DeferredItem<CubeTier1> ENCHANT_CUBE_TIER1 = ITEMS.registerItem("enchant_cube_tier1", CubeTier1::new);
    public static final DeferredItem<CubeTier2> ENCHANT_CUBE_TIER2 = ITEMS.registerItem("enchant_cube_tier2", CubeTier2::new);
    public static final DeferredItem<BlockItem> TEMPLATE_SHELF = ITEMS.registerSimpleBlockItem("template_shelf", ModBlocks.TEMPLATE_SHELF);
    public static final DeferredItem<BlockItem> PRINTING_PRESS = ITEMS.registerSimpleBlockItem("printing_press", ModBlocks.PRINTING_PRESS);
    public static final DeferredItem<Item> ENCHANT_TEMPLATE = ITEMS.registerItem("enchant_template", EnchantTemplate::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> RESEARCH_HANDBOOK = ITEMS.registerItem("research_handbook", ResearchHandbook::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<BlockItem> SCRIBING_TABLE = ITEMS.registerSimpleBlockItem("scribing_table", ModBlocks.SCRIBING_TABLE);
    public static final DeferredItem<BlockItem> ALTAR = ITEMS.registerSimpleBlockItem("altar", ModBlocks.ALTAR);
    public static final DeferredItem<BlockItem> SMALL_CAULDRON = ITEMS.registerSimpleBlockItem("small_cauldron", ModBlocks.SMALL_CAULDRON);

    public static final DeferredItem<BlockItem> WHEEL_CORE = ITEMS.registerSimpleBlockItem("wheel_core", ModBlocks.WHEEL_CORE);
    public static final DeferredItem<BlockItem> WHEEL_BODY = ITEMS.registerSimpleBlockItem("wheel_body", ModBlocks.WHEEL_BODY);

    private ModItems() {}
}
