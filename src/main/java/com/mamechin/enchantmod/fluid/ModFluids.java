package com.mamechin.enchantmod.fluid;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;
import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** One ink fluid per tier. Adding a tier: one line here plus the textures/models/lang listed in InkFluid's assets. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, EnchantMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, EnchantMod.MODID);

    public static final InkFluid LAPIS_INK = InkFluid.register(EnchantMod.MODID, "lapis_ink", 0xFF1E3CB4,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);
    public static final InkFluid GLOWING_LAPIS_INK = InkFluid.register(EnchantMod.MODID, "glowing_lapis_ink", 0xFF46AAFF,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);

    private ModFluids() {}
}
