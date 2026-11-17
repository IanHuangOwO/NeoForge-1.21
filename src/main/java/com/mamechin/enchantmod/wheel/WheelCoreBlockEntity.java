package com.mamechin.enchantmod.wheel;

import com.mamechin.enchantmod.blocks.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Holds nothing; it exists so {@code WheelRenderer} can draw the wheel from the core. */
public class WheelCoreBlockEntity extends BlockEntity {
    public WheelCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHEEL_CORE.get(), pos, state);
    }
}
