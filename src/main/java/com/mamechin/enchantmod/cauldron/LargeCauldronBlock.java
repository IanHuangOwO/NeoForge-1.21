package com.mamechin.enchantmod.cauldron;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.mamechin.enchantmod.block.ModBlocks;

/**
 * The core of a formed large cauldron: the same behaviour as the small one (clicks, brewing, storage) with the sizes of
 * CauldronSize.LARGE. Only the floor is solid; the walls are the surrounding LargeCauldronPartBlocks.
 */
public class LargeCauldronBlock extends SmallCauldronBlock {
    public static final MapCodec<LargeCauldronBlock> CODEC = simpleCodec(LargeCauldronBlock::new);
    private static final VoxelShape FLOOR = Shapes.box(0, 0, 0, 1, 4.0 / 16.0, 1);

    public LargeCauldronBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CauldronBlockEntity(ModBlockEntities.LARGE_CAULDRON.get(), CauldronSize.LARGE, pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FLOOR;
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return FLOOR;
    }

    /** A removed core takes the whole structure with it; a swap with the small cauldron is handled by LargeCauldronStructure. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!(newState.getBlock() instanceof SmallCauldronBlock) && !level.isClientSide) {
            LargeCauldronStructure.revert(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
