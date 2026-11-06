package com.mamechin.enchantmod.block;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnchantMod.MODID);

    public static final DeferredBlock<ScribingTableBlock> SCRIBING_TABLE = BLOCKS.registerBlock("scribing_table",
            ScribingTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE));

    public static final DeferredBlock<CalciteCauldronBlock> CALCITE_CAULDRON = BLOCKS.registerBlock("calcite_cauldron",
            CalciteCauldronBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON));

    private ModBlocks() {}
}
