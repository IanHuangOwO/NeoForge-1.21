package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnchantMod.MODID);

    public static final DeferredBlock<ScribingTableBlock> SCRIBING_TABLE = BLOCKS.registerBlock("scribing_table",
            ScribingTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).noOcclusion().lightLevel(state -> 5));

    public static final DeferredBlock<SmallCauldronBlock> SMALL_CAULDRON = BLOCKS.registerBlock("small_cauldron",
            SmallCauldronBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).sound(SoundType.CALCITE));

    public static final DeferredBlock<AltarBlock> ALTAR = BLOCKS.registerBlock("altar",
            AltarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE));

    public static final DeferredBlock<TemplateShelfBlock> TEMPLATE_SHELF = BLOCKS.registerBlock("template_shelf",
            TemplateShelfBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_BOOKSHELF));

    public static final DeferredBlock<PrintingPressBlock> PRINTING_PRESS = BLOCKS.registerBlock("printing_press",
            PrintingPressBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).noOcclusion());

    private ModBlocks() {}
}
