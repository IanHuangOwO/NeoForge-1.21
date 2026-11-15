package com.mamechin.enchantmod.blocks;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import com.mamechin.enchantmod.scribing.ScribingTableBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * A two-block-wide table. FACING is the side its front faces (towards the player who placed it); LEFT is the half that
 * was placed, RIGHT is the half added on the viewer's right. Breaking either half removes both; one item drops.
 */
public class ScribingTableBlock extends Block implements EntityBlock {
    public static final MapCodec<ScribingTableBlock> CODEC = simpleCodec(ScribingTableBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    /** True while an enchant template sits in the table; the model then shows the rune grid, otherwise a bare desk. */
    public static final BooleanProperty TEMPLATE = BooleanProperty.create("template");

    public enum Part implements StringRepresentable {
        LEFT("left"), RIGHT("right");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public ScribingTableBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.LEFT).setValue(TEMPLATE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, TEMPLATE);
    }

    /** Direction from this half to the other half. */
    public static Direction partnerDirection(BlockState state) {
        Direction toRight = state.getValue(FACING).getCounterClockWise(); // viewer's right when looking at the front
        return state.getValue(PART) == Part.LEFT ? toRight : toRight.getOpposite();
    }

    /** Only the left half keeps the contents; the right half forwards clicks to it. */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == Part.LEFT ? new ScribingTableBlockEntity(pos, state) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockPos left = state.getValue(PART) == Part.LEFT ? pos : pos.relative(partnerDirection(state));
            if (level.getBlockEntity(left) instanceof ScribingTableBlockEntity table) player.openMenu(table, left);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof ScribingTableBlockEntity table) {
            table.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Sets the template flag on both halves, given the left half's position and state. */
    public static void setTemplate(Level level, BlockPos left, BlockState state, boolean template) {
        if (state.getValue(TEMPLATE) == template) return;
        level.setBlock(left, state.setValue(TEMPLATE, template), 3);
        BlockPos right = left.relative(partnerDirection(state));
        BlockState other = level.getBlockState(right);
        if (other.is(state.getBlock())) level.setBlock(right, other.setValue(TEMPLATE, template), 3);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos other = context.getClickedPos().relative(facing.getCounterClockWise());
        Level level = context.getLevel();
        if (!level.getWorldBorder().isWithinBounds(other) || !level.getBlockState(other).canBeReplaced(context)) {
            return null; // no room for the second half
        }
        return defaultBlockState().setValue(FACING, facing).setValue(PART, Part.LEFT).setValue(TEMPLATE, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            level.setBlock(pos.relative(partnerDirection(state)), state.setValue(PART, Part.RIGHT).setValue(TEMPLATE, false), 3);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, 3);
        }
    }

    /** If the other half is gone, this one goes too (no drop: the half that was broken already dropped the item). */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
            LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == partnerDirection(state)) {
            boolean intact = neighborState.is(this)
                    && neighborState.getValue(FACING) == state.getValue(FACING)
                    && neighborState.getValue(PART) != state.getValue(PART);
            return intact ? state : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }
}
