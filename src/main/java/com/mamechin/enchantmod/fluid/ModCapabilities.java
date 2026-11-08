package com.mamechin.enchantmod.fluid;

import com.mamechin.enchantmod.cauldron.ModBlockEntities;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ModBlockEntities.CAULDRON_TIER0.get(), (cauldron, side) -> cauldron.fluidHandler);
    }

    private ModCapabilities() {}
}
