package com.mamechin.enchantmod.items.inkwell;

import com.mamechin.enchantmod.registry.ModFluids;

public class InkwellTier2 extends EnchantInkwell {
    public InkwellTier2(Properties properties) {
        super(properties.durability(256), ModFluids.INK_TIER2.source());
    }
}
