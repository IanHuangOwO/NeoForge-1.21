package com.mamechin.enchantmod.cauldron;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;


/**
 * One of the 20 leg and body blocks of a formed large cauldron. DX/DY/DZ is its place in the 3x3x3 cube (the core is 1,1,1),
 * which picks its model and locates the controller. Clicks go to the controller; losing any part reverts the structure.
 */
public class LargeCauldronPartBlock extends Block {
    public static final IntegerProperty DX = IntegerProperty.create("dx", 0, 2);
    public static final IntegerProperty DY = IntegerProperty.create("dy", 0, 2);
    public static final IntegerProperty DZ = IntegerProperty.create("dz", 0, 2);

    public LargeCauldronPartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(DX, 1).setValue(DY, 0).setValue(DZ, 1));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DX, DY, DZ);
    }

    private static BlockPos controllerPos(BlockPos pos, BlockState state) {
        return pos.offset(1 - state.getValue(DX), 1 - state.getValue(DY), 1 - state.getValue(DZ));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        BlockPos core = controllerPos(pos, state);
        BlockState controller = level.getBlockState(core);
        if (!(controller.getBlock() instanceof LargeCauldronBlock)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return controller.useItemOn(stack, level, player, hand, hit.withPosition(core));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockPos core = controllerPos(pos, state);
        BlockState controller = level.getBlockState(core);
        if (!(controller.getBlock() instanceof LargeCauldronBlock)) return InteractionResult.PASS;
        return controller.useWithoutItem(level, player, hit.withPosition(core));
    }

    /** The controller draws the whole 3D model, so parts draw nothing. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    /** Legs are short columns by the corner nearest the core (as in the model), the body blocks are full. */
    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(DY) != 0) return Shapes.block();
        double x = state.getValue(DX) == 0 ? 11.0 / 16.0 : 0, z = state.getValue(DZ) == 0 ? 11.0 / 16.0 : 0;
        return Shapes.box(x, 0, z, x + 5.0 / 16.0, 10.0 / 16.0, z + 5.0 / 16.0);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide) {
            BlockPos core = controllerPos(pos, state);
            if (level.getBlockState(core).getBlock() instanceof LargeCauldronBlock) {
                LargeCauldronStructure.dismantle(level, core);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
