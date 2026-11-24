package com.mamechin.enchantmod.blocks.altar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Where a chiseled bookshelf's book slot is in the world: the middle of the slot on the shelf's front face, just outside it. */
public final class ShelfSlot {
    private ShelfSlot() {}

    public static Vec3 front(BlockPos pos, BlockState shelf, int slot) {
        Direction facing = shelf.hasProperty(HorizontalDirectionalBlock.FACING) ? shelf.getValue(HorizontalDirectionalBlock.FACING) : Direction.NORTH;
        // vanilla's slot layout seen from the front: columns split at 0.375 and 0.6875, the top row is slots 0 to 2
        float u = new float[] {0.1875F, 0.53125F, 0.84375F}[Math.floorMod(slot, 3)], v = slot < 3 ? 0.75F : 0.25F;
        double x = pos.getX(), y = pos.getY() + v, z = pos.getZ();
        switch (facing) {
            case NORTH -> { x += 1 - u; z -= 0.02; }
            case SOUTH -> { x += u; z += 1.02; }
            case WEST -> { z += u; x -= 0.02; }
            default -> { z += 1 - u; x += 1.02; }
        }
        return new Vec3(x, y, z);
    }
}
