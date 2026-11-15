package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronSize;
import com.mamechin.enchantmod.cauldron.CauldronContent;
import com.mamechin.enchantmod.cauldron.action.CauldronAction;
import com.mamechin.enchantmod.cauldron.action.CauldronActions;
import com.mamechin.enchantmod.cauldron.brewing.CauldronRecipes;
import com.mamechin.enchantmod.cauldron.client.CauldronBubbles;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
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
public class CauldronTier0 extends AbstractCauldronBlock implements EntityBlock {
    public static final MapCodec<CauldronTier0> CODEC = simpleCodec(CauldronTier0::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 3);
    /** Cached "some heat source is directly below", kept current by updateShape; drives the bubbles. */
    public static final BooleanProperty HEATED = BooleanProperty.create("heated");
    public static final EnumProperty<CauldronContent> CONTENT = EnumProperty.create("content", CauldronContent.class);

    // Required by the vanilla base class, but unused: all clicks go through CauldronActions.
    private static final CauldronInteraction.InteractionMap NO_INTERACTIONS = CauldronInteraction.newInteractionMap("calcite");

    public CauldronTier0(Properties properties) {
        super(properties, NO_INTERACTIONS);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0).setValue(CONTENT, CauldronContent.WATER).setValue(HEATED, false));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, CONTENT, HEATED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return defaultBlockState().setValue(HEATED, CauldronHeat.tier(below) > 0);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            return state.setValue(HEATED, CauldronHeat.tier(neighborState) > 0);
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CauldronBlockEntity(ModBlockEntities.SMALL_CAULDRON.get(), CauldronSize.SMALL, pos, state);
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
        if (level.getBlockEntity(pos) instanceof CauldronBlockEntity cauldron) {
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
        if (level.getBlockEntity(pos) instanceof CauldronBlockEntity cauldron) {
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
                || !(level.getBlockEntity(pos) instanceof CauldronBlockEntity cauldron) || !cauldron.hasRoom()) {
            return;
        }
        ItemStack remainder = ItemHandlerHelper.insertItem(cauldron.inventory, item.getItem(), false);
        if (remainder.isEmpty()) {
            item.discard();
        } else {
            item.setItem(remainder);
        }
    }

    /** Breaking spills the stored liquid as a source block (water, liquid of enchanting or ink); items drop in onRemove. */
    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        BlockState replacement = Blocks.AIR.defaultBlockState();
        if (level.getBlockEntity(pos) instanceof CauldronBlockEntity c && c.getAmountMl() > 0) {
            replacement = c.getFluid().defaultFluidState().createLegacyBlock();
        }
        return level.setBlock(pos, replacement, level.isClientSide ? 11 : 3);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !(newState.getBlock() instanceof CauldronTier0) && level.getBlockEntity(pos) instanceof CauldronBlockEntity cauldron) {
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
        if (type != ModBlockEntities.SMALL_CAULDRON.get()) return null;
        // Only tick while there is something to do; the ticker is re-evaluated whenever the blockstate changes.
        if (level.isClientSide) {
            if (!state.getValue(HEATED) || state.getValue(LEVEL) == 0) return null;
            return (l, p, s, be) -> CauldronBubbles.tick(l, p, s);
        }
        if (state.getValue(CONTENT) != CauldronContent.BREWING) return null;
        return (l, p, s, be) -> ((CauldronBlockEntity) be).serverTick();
    }
}
