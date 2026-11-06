package com.mamechin.enchantmod.item.inkwell;

import com.mamechin.enchantmod.fluid.ModFluids;

public class LapisInkwellItem extends EnchantInkwellItem {
    public LapisInkwellItem(Properties properties) {
        super(properties.durability(64), ModFluids.LAPIS_INK.source());
    }
}
