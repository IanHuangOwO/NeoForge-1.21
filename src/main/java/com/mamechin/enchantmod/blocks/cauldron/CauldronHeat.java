package com.mamechin.enchantmod.blocks.cauldron;

import com.mamechin.enchantmod.balance.Balance;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Heat tier of the block under a cauldron, read from data/enchantmod/enchantmod/cauldron/heat.json (a balance file, {@link Balance}).
 * Addons and datapacks add heat sources by adding entries there; no code needed.
 */
public final class CauldronHeat {
    /** Heat tier of a single block state; 0 when it is no heat source (or an unlit one). */
    public static int tier(BlockState state) {
        Balance.Heat source = Balance.current().heat().get(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
        if (source == null) return 0;
        if (source.requiresLit() && state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT)) return 0;
        return source.tier();
    }

    /** Heat tier directly below the position; 0 when there is no (lit) heat source. */
    public static int tierBelow(Level level, BlockPos pos) {
        return tier(level.getBlockState(pos.below()));
    }

    private CauldronHeat() {}
}
