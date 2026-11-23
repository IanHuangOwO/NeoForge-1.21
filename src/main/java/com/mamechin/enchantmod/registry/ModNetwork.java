package com.mamechin.enchantmod.registry;

import com.mamechin.enchantmod.blocks.scribing.DefinitionsPayload;
import com.mamechin.enchantmod.blocks.scribing.EditBlueprintPayload;
import com.mamechin.enchantmod.blocks.scribing.RenameTemplatePayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers every payload under one registrar. */
public final class ModNetwork {
    private ModNetwork() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        EditBlueprintPayload.register(registrar);
        DefinitionsPayload.register(registrar);
        RenameTemplatePayload.register(registrar);
        com.mamechin.enchantmod.balance.BalancePayload.register(registrar);
    }
}
