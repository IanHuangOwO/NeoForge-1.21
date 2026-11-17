package com.mamechin.enchantmod.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Smooth basalt that has become a pillar of a formed spinning wheel. Invisible (the core's renderer draws the pillar model); it has
 * no item, drops smooth basalt, and breaking it releases the wheel, which turns the other pillar blocks back into smooth basalt.
 */
public class WheelPillarBlock extends Block {
    public static final MapCodec<WheelPillarBlock> CODEC = simpleCodec(WheelPillarBlock::new);

    public WheelPillarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.SMOOTH_BASALT);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos core = WheelCoreBlock.ownerOfPillar(level, pos);
            if (core != null) WheelCoreBlock.unform(level, core, level.getBlockState(core).getValue(WheelCoreBlock.FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
