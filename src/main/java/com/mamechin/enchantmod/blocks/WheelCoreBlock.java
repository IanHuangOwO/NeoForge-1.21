package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.wheel.WheelCoreBlockEntity;
import com.mamechin.enchantmod.wheel.WheelLayout;
import com.mojang.serialization.MapCodec;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The middle of the spinning wheel. It faces the player who placed it; the wheel's frame blocks stand in the plane across that
 * facing ({@link WheelLayout}). With the smooth basalt pillars ({@link WheelLayout#pillars}) and every
 * frame there and not part of another wheel, the wheel forms. It holds nothing: the altar in front of it ({@link #altarPos}) keeps the
 * cube and the item, and the wheel only shows the altar's cube and spins with it (tier 2 altar).
 */
public class WheelCoreBlock extends Block implements EntityBlock {
    public static final MapCodec<WheelCoreBlock> CODEC = simpleCodec(WheelCoreBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = WheelBodyBlock.FORMED;

    public WheelCoreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FORMED, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WheelCoreBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(this)) tryForm(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && state.getValue(FORMED)) unform(level, pos, state.getValue(FACING));
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** Where the altar this wheel serves stands: on the bottom row, {@link WheelLayout#PEDESTAL_DISTANCE} blocks out in front. */
    public static BlockPos altarPos(BlockPos core, Direction facing) {
        return core.below(WheelLayout.RADIUS).relative(facing, WheelLayout.PEDESTAL_DISTANCE);
    }

    /** The formed wheel core whose altar spot is {@code altar}, or null. */
    public static BlockPos wheelFor(Level level, BlockPos altar) {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            BlockPos core = altar.above(WheelLayout.RADIUS).relative(facing, -WheelLayout.PEDESTAL_DISTANCE);
            BlockState state = level.getBlockState(core);
            if (state.getBlock() instanceof WheelCoreBlock && state.getValue(FORMED) && state.getValue(FACING) == facing) return core;
        }
        return null;
    }

    /** Placing smooth basalt calls no code of ours, so an unformed core tries to form once a second. A formed core needs no ticker. */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(FORMED)) return null;
        return (l, pos, s, be) -> {
            if (l.getGameTime() % 20 == 0) tryForm(l, pos);
        };
    }

    /**
     * Forms the wheel when the core is unformed, every body position holds an unformed body block and every pillar position holds
     * smooth basalt. The basalt is swapped for the invisible {@link WheelPillarBlock} so the pillar model can be drawn in its place.
     */
    static void tryForm(Level level, BlockPos core) {
        BlockState state = level.getBlockState(core);
        if (!(state.getBlock() instanceof WheelCoreBlock) || state.getValue(FORMED)) return;
        var bodies = bodies(core, state.getValue(FACING));
        var pillars = pillars(core, state.getValue(FACING));
        for (BlockPos pos : pillars) if (!level.getBlockState(pos).is(Blocks.SMOOTH_BASALT)) return;
        for (BlockPos pos : bodies) {
            BlockState body = level.getBlockState(pos);
            if (!(body.getBlock() instanceof WheelBodyBlock) || body.getValue(FORMED)) return;
        }
        for (BlockPos pos : bodies) level.setBlock(pos, level.getBlockState(pos).setValue(FORMED, true), 3);
        for (BlockPos pos : pillars) level.setBlock(pos, ModBlocks.WHEEL_PILLAR.get().defaultBlockState(), 3);
        level.setBlock(core, state.setValue(FORMED, true), 3);
    }

    /**
     * Releases a formed wheel: the core (if still there) first, so the pillar and body blocks changed after it find no owner and do
     * not release it again; then the bodies, and the pillars turn back into smooth basalt. Only a formed core owns its parts.
     */
    static void unform(Level level, BlockPos core, Direction facing) {
        BlockState state = level.getBlockState(core);
        if (state.getBlock() instanceof WheelCoreBlock) level.setBlock(core, state.setValue(FORMED, false), 3);
        for (BlockPos pos : bodies(core, facing)) {
            BlockState body = level.getBlockState(pos);
            if (body.getBlock() instanceof WheelBodyBlock && body.getValue(FORMED)) level.setBlock(pos, body.setValue(FORMED, false), 3);
        }
        for (BlockPos pos : pillars(core, facing)) {
            if (level.getBlockState(pos).getBlock() instanceof WheelPillarBlock) level.setBlock(pos, Blocks.SMOOTH_BASALT.defaultBlockState(), 3);
        }
    }

    static List<BlockPos> pillars(BlockPos core, Direction facing) {
        return WheelLayout.pillars(facing.getAxis() == Direction.Axis.X).stream().map(o -> core.offset(o[0], o[1], o[2])).toList();
    }

    /** The formed core that owns the pillar block at {@code pillar}, or null. */
    static BlockPos ownerOfPillar(Level level, BlockPos pillar) {
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST}) {
            for (int[] o : WheelLayout.pillars(facing.getAxis() == Direction.Axis.X)) {
                BlockPos pos = pillar.offset(-o[0], -o[1], -o[2]);
                BlockState state = level.getBlockState(pos);
                if (state.getBlock() instanceof WheelCoreBlock && state.getValue(FORMED) && pillars(pos, state.getValue(FACING)).contains(pillar)) return pos;
            }
        }
        return null;
    }

    /** The formed core that owns the body block at {@code body}, or null. */
    static BlockPos ownerOf(Level level, BlockPos body) {
        for (BlockPos pos : possibleCores(body)) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof WheelCoreBlock && state.getValue(FORMED) && isBody(pos, state.getValue(FACING), body)) return pos;
        }
        return null;
    }

    static List<BlockPos> bodies(BlockPos core, Direction facing) {
        return WheelLayout.bodies(facing.getAxis() == Direction.Axis.X).stream().map(o -> core.offset(o[0], o[1], o[2])).toList();
    }

    static boolean isBody(BlockPos core, Direction facing, BlockPos pos) {
        return WheelLayout.isBody(pos.getX() - core.getX(), pos.getY() - core.getY(), pos.getZ() - core.getZ(), facing.getAxis() == Direction.Axis.X);
    }

    /** Every position a core of either orientation could be at for {@code body} to be one of its bodies. */
    static List<BlockPos> possibleCores(BlockPos body) {
        return Stream.concat(bodies(body, Direction.NORTH).stream(), bodies(body, Direction.EAST).stream()).toList();
    }
}
