package com.mamechin.enchantmod.blocks;

import com.mamechin.enchantmod.items.EnchantTemplate;
import com.mamechin.enchantmod.shelf.TemplateShelfBlockEntity;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A shelf for six enchant templates standing side by side. Click a strip of the front face: a full strip gives its template
 * back, an empty strip takes the template in hand (like the chiseled bookshelf).
 */
public class TemplateShelfBlock extends Block implements EntityBlock {
    public static final MapCodec<TemplateShelfBlock> CODEC = simpleCodec(TemplateShelfBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** FULL[i] is true while slot i, counted from the viewer's right, holds a template. */
    public static final BooleanProperty[] FULL = new BooleanProperty[TemplateShelfBlockEntity.SLOTS];

    static {
        for (int i = 0; i < FULL.length; i++) FULL[i] = BooleanProperty.create("slot_" + i);
    }

    public TemplateShelfBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH);
        for (BooleanProperty full : FULL) state = state.setValue(full, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        builder.add(FULL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TemplateShelfBlockEntity(pos, state);
    }

    /** The strip of the front face that was hit, 0 to 5 from the viewer's right, or -1 when another face was hit. */
    private static int slotAt(BlockState state, BlockHitResult hit) {
        Direction facing = state.getValue(FACING);
        if (hit.getDirection() != facing) return -1;
        Vec3 rel = hit.getLocation().subtract(hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
        double across = switch (facing.getCounterClockWise()) { // the viewer's right
            case EAST -> rel.x;
            case WEST -> 1 - rel.x;
            case SOUTH -> rel.z;
            default -> 1 - rel.z;
        };
        // the model puts slot 0 at its lowest x, which is the viewer's right, and its six slots fill the open part, 1 to 15 px
        double px = (1 - across) * 16;
        return Mth.clamp((int) Math.floor((px - 1) / (14.0 / TemplateShelfBlockEntity.SLOTS)), 0, TemplateShelfBlockEntity.SLOTS - 1);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        int slot = slotAt(state, hit);
        if (slot < 0 || !(level.getBlockEntity(pos) instanceof TemplateShelfBlockEntity shelf)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!shelf.inventory().getStackInSlot(slot).isEmpty()) return take(level, player, shelf, slot) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.FAIL;
        if (!(held.getItem() instanceof EnchantTemplate)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            shelf.inventory().setStackInSlot(slot, held.copyWithCount(1));
            held.consume(1, player);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        int slot = slotAt(state, hit);
        if (slot < 0 || !(level.getBlockEntity(pos) instanceof TemplateShelfBlockEntity shelf) || shelf.inventory().getStackInSlot(slot).isEmpty()) return InteractionResult.PASS;
        take(level, player, shelf, slot);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean take(Level level, Player player, TemplateShelfBlockEntity shelf, int slot) {
        if (!level.isClientSide) {
            ItemStack stack = shelf.inventory().getStackInSlot(slot);
            shelf.inventory().setStackInSlot(slot, ItemStack.EMPTY);
            player.getInventory().placeItemBackInInventory(stack);
        }
        return true;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof TemplateShelfBlockEntity shelf) {
            shelf.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
