package com.mamechin.enchantmod.item.inkwell;

import com.mamechin.enchantmod.fluid.ModFluids;

public class InkwellTier2Item extends EnchantInkwellItem {
    public InkwellTier2Item(Properties properties) {
        super(properties.durability(256), ModFluids.INK_TIER2.source());
    }
}
