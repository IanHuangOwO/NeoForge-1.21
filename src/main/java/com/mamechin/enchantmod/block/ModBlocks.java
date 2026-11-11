package com.mamechin.enchantmod.block;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.cauldron.SmallCauldronBlock;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import com.mamechin.enchantmod.cauldron.LargeCauldronBlock;
import com.mamechin.enchantmod.cauldron.LargeCauldronPartBlock;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnchantMod.MODID);

    public static final DeferredBlock<ScribingTableBlock> SCRIBING_TABLE = BLOCKS.registerBlock("scribing_table",
            ScribingTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE));

    public static final DeferredBlock<SmallCauldronBlock> SMALL_CAULDRON = BLOCKS.registerBlock("small_cauldron",
            SmallCauldronBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).sound(SoundType.CALCITE));

    /** Only exist as a formed structure (see LargeCauldronStructure); no items. */
    public static final DeferredBlock<LargeCauldronBlock> LARGE_CAULDRON = BLOCKS.registerBlock("large_cauldron",
            LargeCauldronBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_BASALT).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<LargeCauldronPartBlock> LARGE_CAULDRON_PART = BLOCKS.registerBlock("large_cauldron_part",
            LargeCauldronPartBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_BASALT).noOcclusion().pushReaction(PushReaction.BLOCK));

    private ModBlocks() {}
}
