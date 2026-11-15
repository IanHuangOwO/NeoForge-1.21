package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.items.cube.EnchantCube;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The altar. Right click with an enchant cube or any item to put it on (one of each), with an empty hand to take the cube,
 * then the item, back off, and with a quill to enchant.
 */
public class AltarBlock extends Block implements EntityBlock {
    public static final MapCodec<AltarBlock> CODEC = simpleCodec(AltarBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public AltarBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AltarBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (held.is(AltarBlockEntity.QUILLS)) {
            if (!level.isClientSide) altar.enchant(player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        int slot = held.getItem() instanceof EnchantCube ? AltarBlockEntity.CUBE : AltarBlockEntity.ITEM;
        if (!altar.inventory().getStackInSlot(slot).isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            altar.inventory().setStackInSlot(slot, held.copyWithCount(1));
            held.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AltarBlockEntity altar)) return InteractionResult.PASS;
        int slot = altar.inventory().getStackInSlot(AltarBlockEntity.CUBE).isEmpty() ? AltarBlockEntity.ITEM : AltarBlockEntity.CUBE;
        ItemStack stack = altar.inventory().getStackInSlot(slot);
        if (stack.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            altar.inventory().setStackInSlot(slot, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(stack);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof AltarBlockEntity altar) {
            altar.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
