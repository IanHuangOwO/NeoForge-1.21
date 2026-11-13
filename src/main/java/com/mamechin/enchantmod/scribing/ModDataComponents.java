package com.mamechin.enchantmod.scribing;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EnchantMod.MODID);

    /** The layout drawn on an enchant template. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Blueprint>> BLUEPRINT =
            COMPONENTS.registerComponentType("blueprint", b -> b.persistent(Blueprint.CODEC).networkSynchronized(Blueprint.STREAM_CODEC));

    private ModDataComponents() {}
}
