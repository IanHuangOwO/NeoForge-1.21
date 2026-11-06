package com.mamechin.enchantmod.fluid;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.block.ModBlocks;
import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** One enchant fluid per tier. Adding a tier: one line here plus the textures/models/lang listed in EnchantFluid's assets. */
public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, EnchantMod.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, EnchantMod.MODID);

    /** Base liquid: water + lapis in the cauldron. Every ink brews from it. */
    public static final EnchantFluid LIQUID_OF_ENCHANTING = EnchantFluid.register(EnchantMod.MODID, "liquid_of_enchanting", 0xFF8A5AE6,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);
    public static final EnchantFluid LAPIS_INK = EnchantFluid.register(EnchantMod.MODID, "lapis_ink", 0xFF12257A,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);
    public static final EnchantFluid GLOWING_LAPIS_INK = EnchantFluid.register(EnchantMod.MODID, "glowing_lapis_ink", 0xFF0A1440,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);

    private ModFluids() {}
}
