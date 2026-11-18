package com.mamechin.enchantmod.balance;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Reads every data pack's copy of each {@link Balance#FILES} file ({@code data/enchantmod/enchantmod/<path>}) on data load and
 * /reload, merges and installs them, and sends the merged files to every client that gets the data.
 */
public final class BalanceLoader extends SimplePreparableReloadListener<Map<String, String>> {
    /** The merged files the server is using, as sent to clients. */
    private static volatile Map<String, String> merged = Map.of();

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new BalanceLoader());
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        BalancePayload payload = new BalancePayload(new HashMap<>(merged));
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @Override
    protected Map<String, String> prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<String, String> out = new HashMap<>();
        for (String name : Balance.FILES.keySet()) {
            List<String> copies = new ArrayList<>();
            for (Resource resource : manager.getResourceStack(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "enchantmod/" + Balance.FILES.get(name)))) {
                try (BufferedReader reader = resource.openAsReader()) {
                    copies.add(String.join("\n", reader.lines().toList()));
                } catch (IOException ex) {
                    EnchantMod.LOGGER.warn("Balance: could not read {} from {}: {}", name, resource.sourcePackId(), ex.getMessage());
                }
            }
            if (!copies.isEmpty()) out.put(name, Balance.merge(copies, message -> EnchantMod.LOGGER.warn("Balance {}: {}", name, message)));
        }
        return out;
    }

    @Override
    protected void apply(Map<String, String> files, ResourceManager manager, ProfilerFiller profiler) {
        merged = Map.copyOf(files);
        install(files, "server");
    }

    /** Parses and installs the merged files, logging anything dropped; the client calls this with what the server sent. */
    public static void install(Map<String, String> files, String side) {
        Balance balance = Balance.parse(files, message -> EnchantMod.LOGGER.warn("Balance: {}", message));
        Balance.install(balance);
        EnchantMod.LOGGER.info("Balance ({}): {} files, {} cubes, {} quills", side, files.size(), balance.cubes().size(), balance.quills().size());
    }
}
