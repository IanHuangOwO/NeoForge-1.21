package com.mamechin.enchantmod.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * One of the 44 frame blocks of the spinning wheel (ring and beams, `WheelLayout`). Placing one may complete a wheel; breaking a formed one releases the
 * whole wheel.
 */
public class WheelBodyBlock extends Block {
    public static final MapCodec<WheelBodyBlock> CODEC = simpleCodec(WheelBodyBlock::new);
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public WheelBodyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide || oldState.is(this)) return;
        for (BlockPos core : WheelCoreBlock.possibleCores(pos)) {
            BlockState s = level.getBlockState(core);
            if (s.getBlock() instanceof WheelCoreBlock && WheelCoreBlock.isBody(core, s.getValue(WheelCoreBlock.FACING), pos)) WheelCoreBlock.tryForm(level, core);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && state.getValue(FORMED)) {
            BlockPos core = WheelCoreBlock.ownerOf(level, pos);
            if (core != null) WheelCoreBlock.unform(level, core, level.getBlockState(core).getValue(WheelCoreBlock.FACING));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
