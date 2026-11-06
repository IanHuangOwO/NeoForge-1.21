package com.mamechin.enchantmod.fluid;

import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * One container-only fluid for every ink tier: the tier rides along as the ink_tier component on the FluidStack.
 * It has no world block, so it exists only inside tanks and buckets.
 */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, EnchantMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, EnchantMod.MODID);

    public static final Supplier<FluidType> ENCHANTING_INK_TYPE = FLUID_TYPES.register("enchanting_ink",
            () -> new FluidType(FluidType.Properties.create().descriptionId("fluid_type.enchantmod.enchanting_ink")));
    public static final Supplier<Fluid> ENCHANTING_INK = FLUIDS.register("enchanting_ink", () -> new BaseFlowingFluid.Source(ModFluids.PROPERTIES));
    public static final Supplier<Fluid> ENCHANTING_INK_FLOWING = FLUIDS.register("enchanting_ink_flowing", () -> new BaseFlowingFluid.Flowing(ModFluids.PROPERTIES));

    private static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(ENCHANTING_INK_TYPE, ENCHANTING_INK, ENCHANTING_INK_FLOWING)
            .bucket(ModItems.INK_BUCKET);

    public static FluidStack inkStack(ResourceLocation tier, int amount) {
        FluidStack stack = new FluidStack(ENCHANTING_INK.get(), amount);
        stack.set(ModDataComponents.INK_TIER.get(), tier);
        return stack;
    }

    private ModFluids() {}
}
