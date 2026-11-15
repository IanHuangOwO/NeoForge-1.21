package com.mamechin.enchantmod.fluid;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.ModBlocks;
import com.mamechin.enchantmod.items.ModItems;

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
    public static final EnchantFluid LIQUID_OF_ENCHANTING = EnchantFluid.register(EnchantMod.MODID, "liquid_of_enchanting", 0xFF2D66B8,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);
    public static final EnchantFluid INK_TIER0 = EnchantFluid.register(EnchantMod.MODID, "ink_tier0", 0xFF12257A,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);
    public static final EnchantFluid INK_TIER1 = EnchantFluid.register(EnchantMod.MODID, "ink_tier1", 0xFF0A1440,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);

    public static final EnchantFluid INK_TIER2 = EnchantFluid.register(EnchantMod.MODID, "ink_tier2", 0xFF050A24,
            FLUID_TYPES, FLUIDS, ModBlocks.BLOCKS, ModItems.ITEMS);

    private ModFluids() {}
}
