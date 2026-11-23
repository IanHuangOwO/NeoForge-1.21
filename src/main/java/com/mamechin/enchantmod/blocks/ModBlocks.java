package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnchantMod.MODID);

    public static final DeferredBlock<ScribingTableBlock> SCRIBING_TABLE = BLOCKS.registerBlock("scribing_table",
            ScribingTableBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CRAFTING_TABLE).noOcclusion().lightLevel(state -> 5));

    public static final DeferredBlock<CauldronTier0> SMALL_CAULDRON = BLOCKS.registerBlock("small_cauldron",
            CauldronTier0::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CAULDRON).sound(SoundType.CALCITE));

    public static final DeferredBlock<AltarBlock> ALTAR = BLOCKS.registerBlock("altar",
            AltarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).noOcclusion());

    public static final DeferredBlock<TemplateShelfBlock> TEMPLATE_SHELF = BLOCKS.registerBlock("template_shelf",
            TemplateShelfBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.CHISELED_BOOKSHELF).noOcclusion());

    public static final DeferredBlock<PrintingPressBlock> PRINTING_PRESS = BLOCKS.registerBlock("printing_press",
            PrintingPressBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.ANVIL).noOcclusion());

    /** A formed wheel or sphere core gives a faint light, the enchanting table's 7 like the altar (author, 2026-11-23). */
    private static final java.util.function.ToIntFunction<net.minecraft.world.level.block.state.BlockState> FORMED_GLOW = state -> state.getValue(WheelBodyBlock.FORMED) ? 7 : 0;

    public static final DeferredBlock<WheelCoreBlock> WHEEL_CORE = BLOCKS.registerBlock("wheel_core",
            WheelCoreBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).noOcclusion().lightLevel(FORMED_GLOW).pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<WheelBodyBlock> WHEEL_BODY = BLOCKS.registerBlock("wheel_body",
            WheelBodyBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).noOcclusion().pushReaction(PushReaction.BLOCK));

    /** Smooth basalt inside a formed wheel; no item, drops smooth basalt. */
    public static final DeferredBlock<WheelPillarBlock> WHEEL_PILLAR = BLOCKS.registerBlock("wheel_pillar",
            WheelPillarBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SMOOTH_BASALT).noOcclusion().pushReaction(PushReaction.BLOCK));

    public static final DeferredBlock<SphereCoreBlock> SPHERE_CORE = BLOCKS.registerBlock("sphere_core",
            SphereCoreBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK).noOcclusion().lightLevel(FORMED_GLOW).pushReaction(PushReaction.BLOCK));

    /** Glass or waxed copper inside a formed glass sphere; no item, drops what it was. */
    public static final DeferredBlock<SpherePartBlock> SPHERE_PART = BLOCKS.registerBlock("sphere_part",
            SpherePartBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).noOcclusion().pushReaction(PushReaction.BLOCK));

    private ModBlocks() {}
}
