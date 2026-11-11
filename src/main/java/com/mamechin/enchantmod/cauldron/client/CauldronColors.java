package com.mamechin.enchantmod.cauldron.client;

import java.util.Map;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * Colour (0xRRGGBB) of each fluid as shown inside the cauldron. The brew gradient fades from the base fluid's colour
 * to the result fluid's colour, so edit the values below to change it. Keep them close to the fluid's liquid texture
 * in assets/enchantmod/textures/block (cauldron_tier0_*.png), otherwise the liquid visibly jumps when a brew ends.
 * A fluid that is not listed falls back to its placed-fluid tint.
 */
public final class CauldronColors {
    private static final Map<ResourceLocation, Integer> COLORS = Map.of(
            ResourceLocation.withDefaultNamespace("water"), 0x326ED7,
            ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "liquid_of_enchanting"), 0x2D64B8,
            ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "ink_tier0"), 0x122482,
            ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "ink_tier1"), 0x0A1440,
            ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "ink_tier2"), 0x050A24);

    public static int of(ResourceLocation fluidId) {
        Integer color = COLORS.get(fluidId);
        if (color != null) return color;
        return IClientFluidTypeExtensions.of(BuiltInRegistries.FLUID.get(fluidId)).getTintColor() & 0xFFFFFF;
    }

    private CauldronColors() {}
}
