package com.mamechin.enchantmod.altar;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * Reads every data pack's {@code data/enchantmod/enchantmod/altar_power.json} on data load and /reload into {@link AltarPowerTable}.
 * Server side only: the client is sent the altar's power as a number.
 */
public final class AltarPowerLoader extends SimplePreparableReloadListener<List<String>> {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "enchantmod/altar_power.json");

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new AltarPowerLoader());
    }

    @Override
    protected List<String> prepare(ResourceManager manager, ProfilerFiller profiler) {
        List<String> files = new ArrayList<>();
        for (Resource resource : manager.getResourceStack(FILE)) { // lowest priority pack first
            try (BufferedReader reader = resource.openAsReader()) {
                files.add(String.join("\n", reader.lines().toList()));
            } catch (IOException ex) {
                EnchantMod.LOGGER.warn("Altar power: could not read {}: {}", resource.sourcePackId(), ex.getMessage());
            }
        }
        return files;
    }

    @Override
    protected void apply(List<String> files, ResourceManager manager, ProfilerFiller profiler) {
        AltarPowerTable table = AltarPowerTable.parse(files, message -> EnchantMod.LOGGER.warn("Altar power: {}", message));
        AltarPowerTable.install(table);
        EnchantMod.LOGGER.info("Altar power: {} rarities, {} enchantments", table.rarities().size(), table.enchantments().size());
    }
}
