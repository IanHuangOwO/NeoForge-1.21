package com.mamechin.enchantmod.items.inkwell;

import com.mamechin.enchantmod.fluid.ModFluids;

public class InkwellTier1 extends EnchantInkwell {
    public InkwellTier1(Properties properties) {
        super(properties.durability(128), ModFluids.INK_TIER1.source());
    }
}
