package com.mamechin.enchantmod.multiblock.sphere;

import com.mamechin.enchantmod.registry.ModBlocks;
import com.mamechin.enchantmod.multiblock.wheel.WheelBodyBlock;
import com.mojang.serialization.MapCodec;
import com.mamechin.enchantmod.multiblock.GuidedCoreBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The centre of the glass sphere (with a spinning wheel, the tier 2 altar). The sphere forms when every shell cell of
 * {@link SphereLayout} holds glass and every ring cell waxed copper; they are swapped for the invisible {@link SpherePartBlock} and the
 * core's renderer draws the whole sphere. Breaking the core or any part releases it. No facing: the sphere looks the same every way.
 */
public class SphereCoreBlock extends Block implements EntityBlock {
    public static final MapCodec<SphereCoreBlock> CODEC = simpleCodec(SphereCoreBlock::new);
    public static final BooleanProperty FORMED = WheelBodyBlock.FORMED;

    public SphereCoreBlock(Properties properties) {
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SphereCoreBlockEntity(pos, state);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof SphereCoreBlockEntity sphere) sphere.dropTree();
            if (state.getValue(FORMED)) unform(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** A quill on the unformed core shows or hides the build guide. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return GuidedCoreBlockEntity.toggle(held, state.getValue(FORMED), level, pos, player);
    }

    /** The formed core opens the tree slot (the core sits inside the glass, so the parts open it too: {@link SpherePartBlock}). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return openMenu(level, pos, player);
    }

    static InteractionResult openMenu(Level level, BlockPos core, Player player) {
        BlockState state = level.getBlockState(core);
        if (!(state.getBlock() instanceof SphereCoreBlock) || !state.getValue(FORMED) || !(level.getBlockEntity(core) instanceof SphereCoreBlockEntity sphere)) return InteractionResult.PASS;
        if (!level.isClientSide) player.openMenu(sphere, core);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Placing glass or copper calls no code of ours, so an unformed core tries to form once a second on the server. On the client a
     * formed core runs its animation clock.
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return state.getValue(FORMED) ? (l, pos, s, be) -> {
            if (be instanceof SphereCoreBlockEntity sphere) sphere.clientTick();
        } : null;
        if (state.getValue(FORMED)) return (l, pos, s, be) -> {
            if (be instanceof SphereCoreBlockEntity sphere) sphere.serverTick();
        };
        return (l, pos, s, be) -> {
            if (l.getGameTime() % 20 == 0) tryForm(l, pos);
        };
    }

    static void tryForm(Level level, BlockPos core) {
        BlockState state = level.getBlockState(core);
        if (!(state.getBlock() instanceof SphereCoreBlock) || state.getValue(FORMED)) return;
        for (boolean ring : new boolean[] {false, true}) {
            for (BlockPos pos : parts(core, ring)) if (!level.getBlockState(pos).is(SpherePartBlock.original(ring))) return;
        }
        for (boolean ring : new boolean[] {false, true}) {
            BlockState part = ModBlocks.SPHERE_PART.get().defaultBlockState().setValue(SpherePartBlock.RING, ring);
            for (BlockPos pos : parts(core, ring)) level.setBlock(pos, part, 3);
        }
        level.setBlock(core, state.setValue(FORMED, true), 3);
        if (level.getBlockEntity(core) instanceof GuidedCoreBlockEntity guided) guided.setGuide(false);
    }

    /** Releases a formed sphere: the core first, so the parts changed after it find no owner, then the parts turn back into glass and copper. */
    static void unform(Level level, BlockPos core) {
        if (level.getBlockEntity(core) instanceof SphereCoreBlockEntity sphere) sphere.dropTree(); // the tree only lives in a formed sphere
        BlockState state = level.getBlockState(core);
        if (state.getBlock() instanceof SphereCoreBlock) level.setBlock(core, state.setValue(FORMED, false), 3);
        for (boolean ring : new boolean[] {false, true}) {
            for (BlockPos pos : parts(core, ring)) {
                if (level.getBlockState(pos).getBlock() instanceof SpherePartBlock) level.setBlock(pos, SpherePartBlock.original(ring).defaultBlockState(), 3);
            }
        }
    }

    public static List<BlockPos> parts(BlockPos core, boolean ring) {
        return (ring ? SphereLayout.RINGS : SphereLayout.SHELL).stream().map(o -> core.offset(o[0], o[1], o[2])).toList();
    }

    /** The formed core that owns the part at {@code part} (a ring cell if {@code ring}), or null. */
    static BlockPos ownerOf(Level level, BlockPos part, boolean ring) {
        for (int[] o : ring ? SphereLayout.RINGS : SphereLayout.SHELL) {
            BlockPos pos = part.offset(-o[0], -o[1], -o[2]);
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof SphereCoreBlock && state.getValue(FORMED)) return pos;
        }
        return null;
    }
}
