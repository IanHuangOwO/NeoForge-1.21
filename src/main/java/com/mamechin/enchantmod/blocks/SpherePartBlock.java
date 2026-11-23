package com.mamechin.enchantmod.blocks;

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
 * Glass of the shell, or waxed copper of a ring ({@link #RING}), that has become part of a formed glass sphere. Invisible (the core's
 * renderer draws the sphere); it has no item, drops what it was, and breaking it releases the sphere.
 */
public class SpherePartBlock extends Block {
    public static final MapCodec<SpherePartBlock> CODEC = simpleCodec(SpherePartBlock::new);
    public static final BooleanProperty RING = BooleanProperty.create("ring");

    public SpherePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RING, false));
    }

    /** The block a part was before the sphere formed. */
    public static Block original(boolean ring) {
        return ring ? Blocks.WAXED_COPPER_BLOCK : Blocks.GLASS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RING);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(original(state.getValue(RING)));
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos core = SphereCoreBlock.ownerOf(level, pos, state.getValue(RING));
            if (core != null) SphereCoreBlock.unform(level, core);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
