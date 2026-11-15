package com.mamechin.enchantmod.scribing;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers every payload under one registrar. */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        EditBlueprintPayload.register(registrar);
        DefinitionsPayload.register(registrar);
    }
}
