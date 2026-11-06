package com.mamechin.enchantmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Cauldron shape with no vanilla fluid interactions; custom behavior goes here. */
public class CalciteCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<CalciteCauldronBlock> CODEC = simpleCodec(CalciteCauldronBlock::new);
    private static final CauldronInteraction.InteractionMap NO_INTERACTIONS = CauldronInteraction.newInteractionMap("calcite");

    public CalciteCauldronBlock(Properties properties) {
        super(properties, NO_INTERACTIONS);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return false;
    }
}
