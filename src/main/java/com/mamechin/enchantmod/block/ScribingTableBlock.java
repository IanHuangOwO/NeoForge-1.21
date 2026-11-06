package com.mamechin.enchantmod.block;

import com.mojang.serialization.MapCodec;

import net.minecraft.world.level.block.Block;

public class ScribingTableBlock extends Block {
    public static final MapCodec<ScribingTableBlock> CODEC = simpleCodec(ScribingTableBlock::new);

    public ScribingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
