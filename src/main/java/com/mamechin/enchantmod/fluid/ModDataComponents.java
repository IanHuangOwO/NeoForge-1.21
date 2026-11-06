package com.mamechin.enchantmod.fluid;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, EnchantMod.MODID);

    /** The inkwell id (the tier) an enchanting-ink fluid stack belongs to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ResourceLocation>> INK_TIER = COMPONENTS.registerComponentType(
            "ink_tier", b -> b.persistent(ResourceLocation.CODEC).networkSynchronized(ResourceLocation.STREAM_CODEC));

    /** Fluid stored in an ink bucket (a whole FluidStack, so the tier component survives). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID_CONTENT = COMPONENTS.registerComponentType(
            "fluid_content", b -> b.persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC));

    private ModDataComponents() {}
}
