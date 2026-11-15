package com.mamechin.enchantmod.block.cauldron.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Data-map value: how hot a block is for the cauldron. requiresLit only matters for blocks with a LIT property. */
public record HeatSource(int tier, boolean requiresLit) {
    public static final Codec<HeatSource> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("tier").forGetter(HeatSource::tier),
            Codec.BOOL.optionalFieldOf("requires_lit", false).forGetter(HeatSource::requiresLit)
    ).apply(i, HeatSource::new));
}
