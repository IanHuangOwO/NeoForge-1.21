package com.mamechin.enchantmod.fluid;

import java.util.HashMap;
import java.util.Map;

import com.mamechin.enchantmod.item.inkwell.EnchantInkwellItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;

/** Lookup between an ink fluid and the inkwell item of the same tier; each inkwell class names its own fluid. */
public final class InkTiers {
    private static Map<Fluid, EnchantInkwellItem> byFluid;

    /** The ink fluid an inkwell item holds, or null when the item is not an inkwell. */
    public static Fluid fluidFor(Item inkwell) {
        return inkwell instanceof EnchantInkwellItem i ? i.getInkFluid() : null;
    }

    /** The inkwell for an ink fluid, or null. Built once, after all items are registered. */
    public static EnchantInkwellItem inkwellFor(Fluid fluid) {
        if (byFluid == null) {
            Map<Fluid, EnchantInkwellItem> map = new HashMap<>();
            for (Item item : BuiltInRegistries.ITEM) {
                if (item instanceof EnchantInkwellItem i) map.put(i.getInkFluid(), i);
            }
            byFluid = map;
        }
        return byFluid.get(fluid);
    }

    private InkTiers() {}
}
