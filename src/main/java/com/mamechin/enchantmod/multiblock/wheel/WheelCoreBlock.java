package com.mamechin.enchantmod.multiblock.wheel;

import com.mamechin.enchantmod.multiblock.Multiblock;
import com.mamechin.enchantmod.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import com.mamechin.enchantmod.multiblock.GuidedCoreBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

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
 * frame there and not part of another wheel, the wheel forms. It holds nothing: an altar whose tier 0 area holds the core links it on
 * a scan, wherever it stands and whichever way it faces; the altar keeps the cube and the item, and the wheel shows the altar's cube
 * and spins with it (tier 1 altar, tier 2 with a glass sphere).
 */
public class WheelCoreBlock extends Block implements EntityBlock {
    public static final MapCodec<WheelCoreBlock> CODEC = simpleCodec(WheelCoreBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = Multiblock.FORMED;

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

    /** A quill on the unformed core shows or hides the build guide. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return GuidedCoreBlockEntity.toggle(held, state.getValue(FORMED), level, pos, player);
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
     * smooth basalt over a gold block. These are swapped for the invisible {@link WheelPillarBlock} so the pillar model can be drawn in
     * their place.
     */
    static void tryForm(Level level, BlockPos core) {
        BlockState state = level.getBlockState(core);
        if (!(state.getBlock() instanceof WheelCoreBlock) || state.getValue(FORMED)) return;
        var bodies = bodies(core, state.getValue(FACING));
        var pillars = pillars(core, state.getValue(FACING));
        for (BlockPos pos : pillars) if (!level.getBlockState(pos).is(WheelPillarBlock.original(isPillarBase(core, pos)))) return;
        for (BlockPos pos : bodies) {
            BlockState body = level.getBlockState(pos);
            if (!(body.getBlock() instanceof WheelBodyBlock) || body.getValue(FORMED)) return;
        }
        for (BlockPos pos : bodies) level.setBlock(pos, level.getBlockState(pos).setValue(FORMED, true), 3);
        for (BlockPos pos : pillars) level.setBlock(pos, ModBlocks.WHEEL_PILLAR.get().defaultBlockState().setValue(WheelPillarBlock.BASE, isPillarBase(core, pos)), 3);
        level.setBlock(core, state.setValue(FORMED, true), 3);
        if (level.getBlockEntity(core) instanceof GuidedCoreBlockEntity guided) guided.setGuide(false);
    }

    /**
     * Releases a formed wheel: the core (if still there) first, so the pillar and body blocks changed after it find no owner and do
     * not release it again; then the bodies, and the pillars turn back into smooth basalt and gold. Only a formed core owns its parts.
     */
    static void unform(Level level, BlockPos core, Direction facing) {
        BlockState state = level.getBlockState(core);
        if (state.getBlock() instanceof WheelCoreBlock) level.setBlock(core, state.setValue(FORMED, false), 3);
        for (BlockPos pos : bodies(core, facing)) {
            BlockState body = level.getBlockState(pos);
            if (body.getBlock() instanceof WheelBodyBlock && body.getValue(FORMED)) level.setBlock(pos, body.setValue(FORMED, false), 3);
        }
        for (BlockPos pos : pillars(core, facing)) {
            BlockState pillar = level.getBlockState(pos);
            if (pillar.getBlock() instanceof WheelPillarBlock) level.setBlock(pos, WheelPillarBlock.original(pillar.getValue(WheelPillarBlock.BASE)).defaultBlockState(), 3);
        }
    }

    /** The bottom block of each pillar is a gold block, the rest smooth basalt. */
    public static boolean isPillarBase(BlockPos core, BlockPos pillar) {
        return pillar.getY() == core.getY() - WheelLayout.RADIUS;
    }

    public static List<BlockPos> pillars(BlockPos core, Direction facing) {
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

    public static List<BlockPos> bodies(BlockPos core, Direction facing) {
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
