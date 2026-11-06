package com.mamechin.enchantmod.cauldron.heat;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Heat tier of the block under a cauldron, read from data/enchantmod/data_maps/block/cauldron_heat.json.
 * Addons and datapacks add heat sources by adding entries there; no code needed.
 */
public final class CauldronHeat {
    public static final DataMapType<net.minecraft.world.level.block.Block, HeatSource> DATA_MAP = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "cauldron_heat"), Registries.BLOCK, HeatSource.CODEC).build();

    public static void register(RegisterDataMapTypesEvent event) {
        event.register(DATA_MAP);
    }

    /** Heat tier directly below the position; 0 when there is no (lit) heat source. */
    public static int tierBelow(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        HeatSource source = below.getBlockHolder().getData(DATA_MAP);
        if (source == null) return 0;
        if (source.requiresLit() && below.hasProperty(BlockStateProperties.LIT) && !below.getValue(BlockStateProperties.LIT)) return 0;
        return source.tier();
    }

    private CauldronHeat() {}
}
