package com.mamechin.enchantmod.enchanting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
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
 * Reads the runes, enchant rows and forecast numbers from data packs (data/namespace/enchantmod/scribing/rune, /enchant and forecast.json)
 * whenever data loads or /reload runs, installs them in Definitions, and sends the raw files to every client that gets the data.
 */
public final class DefinitionLoader extends SimplePreparableReloadListener<Definitions.Raw> {
    private static final String RUNES = "enchantmod/scribing/rune", ENCHANTS = "enchantmod/scribing/enchant";
    private static final ResourceLocation FORECAST = ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "enchantmod/scribing/forecast.json");

    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DefinitionLoader());
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        DefinitionsPayload payload = new DefinitionsPayload(Definitions.current().raw());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }

    @Override
    protected Definitions.Raw prepare(ResourceManager manager, ProfilerFiller profiler) {
        String forecast = manager.getResource(FORECAST).map(DefinitionLoader::read).orElse(null);
        return new Definitions.Raw(read(manager, RUNES), read(manager, ENCHANTS), forecast);
    }

    @Override
    protected void apply(Definitions.Raw raw, ResourceManager manager, ProfilerFiller profiler) {
        install(raw, "server");
    }

    /** Parses and installs the files, logging anything that was dropped; the client calls this too, with what the server sent. */
    public static void install(Definitions.Raw raw, String side) {
        Definitions.Snapshot snapshot = Definitions.install(DefinitionParser.parse(raw, message -> EnchantMod.LOGGER.warn("Enchant definitions: {}", message)));
        EnchantMod.LOGGER.info("Enchant definitions ({}): {} runes, {} enchants", side, snapshot.runes().size(), snapshot.enchants().size());
    }

    /** Every .json under the folder in every namespace, by id "namespace:name". */
    private static Map<String, String> read(ResourceManager manager, String folder) {
        Map<String, String> out = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : manager.listResources(folder, id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();
            String text = read(e.getValue());
            if (text != null) out.put(e.getKey().getNamespace() + ":" + path.substring(folder.length() + 1, path.length() - ".json".length()), text);
        }
        return out;
    }

    private static String read(Resource resource) {
        try (BufferedReader reader = resource.openAsReader()) {
            return String.join("\n", reader.lines().toList());
        } catch (IOException ex) {
            EnchantMod.LOGGER.warn("Enchant definitions: could not read {}: {}", resource.sourcePackId(), ex.getMessage());
            return null;
        }
    }
}
