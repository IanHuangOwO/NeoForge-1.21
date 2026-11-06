package com.mamechin.enchantmod.item.inkwell;

import com.mamechin.enchantmod.fluid.ModFluids;

public class GlowingLapisInkwellItem extends EnchantInkwellItem {
    public GlowingLapisInkwellItem(Properties properties) {
        super(properties.durability(128), ModFluids.GLOWING_LAPIS_INK.source());
    }
}
