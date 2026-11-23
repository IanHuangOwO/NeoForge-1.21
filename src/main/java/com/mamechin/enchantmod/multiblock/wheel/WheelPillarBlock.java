package com.mamechin.enchantmod.multiblock.wheel;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Smooth basalt, or the gold block at the bottom ({@link #BASE}), that has become a pillar of a formed spinning wheel. Invisible (the
 * core's renderer draws the pillar model); it has no item, drops what it was, and breaking it releases the wheel, which turns the
 * other pillar blocks back into what they were.
 */
public class WheelPillarBlock extends Block {
    public static final MapCodec<WheelPillarBlock> CODEC = simpleCodec(WheelPillarBlock::new);
    public static final BooleanProperty BASE = BooleanProperty.create("base");

    public WheelPillarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(BASE, false));
    }

    /** The block a pillar block was before the wheel formed. */
    public static Block original(boolean base) {
        return base ? Blocks.GOLD_BLOCK : Blocks.SMOOTH_BASALT;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BASE);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(original(state.getValue(BASE)));
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
