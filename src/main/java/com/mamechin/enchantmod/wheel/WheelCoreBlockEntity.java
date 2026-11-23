package com.mamechin.enchantmod.wheel;

import com.mamechin.enchantmod.blocks.ModBlockEntities;
import com.mamechin.enchantmod.guide.GuidedCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** Holds only the build guide and the altar link ({@link GuidedCoreBlockEntity}); it exists so {@code WheelRenderer} can draw the wheel from the core. */
public class WheelCoreBlockEntity extends GuidedCoreBlockEntity {
    public WheelCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WHEEL_CORE.get(), pos, state);
    }
}
