package com.mamechin.enchantmod.item.inkwell;

import com.mamechin.enchantmod.fluid.ModFluids;

public class InkwellTier1Item extends EnchantInkwellItem {
    public InkwellTier1Item(Properties properties) {
        super(properties.durability(128), ModFluids.INK_TIER1.source());
    }
}
