package com.mamechin.enchantmod.cauldron;

import com.mamechin.enchantmod.cauldron.action.CauldronAction;
import com.mamechin.enchantmod.cauldron.action.CauldronActions;
import com.mamechin.enchantmod.cauldron.brewing.CauldronRecipes;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Thin shell: click behavior lives in cauldron/action, state in the block entity, recipes in cauldron/brewing. */
public class CalciteCauldronBlock extends AbstractCauldronBlock implements EntityBlock {
    public static final MapCodec<CalciteCauldronBlock> CODEC = simpleCodec(CalciteCauldronBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    public static final EnumProperty<CauldronContent> CONTENT = EnumProperty.create("content", CauldronContent.class);

    // Required by the vanilla base class, but unused: all clicks go through CauldronActions.
    private static final CauldronInteraction.InteractionMap NO_INTERACTIONS = CauldronInteraction.newInteractionMap("calcite");

    public CalciteCauldronBlock(Properties properties) {
        super(properties, NO_INTERACTIONS);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(CONTENT, CauldronContent.WATER));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, CONTENT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CalciteCauldronBlockEntity(pos, state);
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == 3;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        int lvl = state.getValue(LEVEL);
        return lvl == 0 ? 0.0 : (6.0 + lvl * 3.0) / 16.0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron) {
            for (CauldronAction action : CauldronActions.ACTIONS) {
                if (action.useItem(cauldron, stack, player, hand)) {
                    return ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron) {
            for (CauldronAction action : CauldronActions.ACTIONS) {
                if (action.useEmptyHand(cauldron, player)) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }
            }
        }
        return InteractionResult.PASS;
    }

    /** Thrown items fall in and are stored if the inventory accepts them. */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        // cheapest checks first: side, item type, then the cached "has room" flag
        if (level.isClientSide || !(entity instanceof ItemEntity item) || !CauldronRecipes.accepts(level, item.getItem())
                || !(level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron) || !cauldron.hasRoom()) {
            return;
        }
        ItemStack remainder = ItemHandlerHelper.insertItem(cauldron.inventory, item.getItem(), false);
        if (remainder.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remainder);
        }
    }

    /** Breaking spills the water as a source block; ink has no fluid block yet, so it is lost. Items drop in onRemove. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        boolean hasWater = level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity c
                && c.getContent() == CauldronContent.WATER && c.getAmountMl() > 0;
        BlockState replacement = hasWater ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
        return level.setBlock(pos, replacement, level.isClientSide ? 11 : 3);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CalciteCauldronBlockEntity cauldron) {
            IItemHandler inv = cauldron.inventory;
            for (int i = 0; i < inv.getSlots(); i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inv.getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CALCITE_CAULDRON.get()) return null;
        return (l, p, s, be) -> ((CalciteCauldronBlockEntity) be).serverTick();
    }
}
