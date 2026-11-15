package com.mamechin.enchantmod.item.inkwell;

import com.mamechin.enchantmod.fluid.ModFluids;

public class InkwellTier0 extends EnchantInkwell {
    public InkwellTier0(Properties properties) {
        super(properties.durability(64), ModFluids.INK_TIER0.source());
    }
}
