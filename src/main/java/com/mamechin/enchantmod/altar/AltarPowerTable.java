package com.mamechin.enchantmod.altar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * How much enchant power each enchantment level on a book is worth, read from {@code data/<namespace>/enchantmod/altar_power.json}
 * (pure). Every data pack's copy of that file is merged in load order, later packs overriding single entries:
 * <pre>
 * { "rarities": { "common": 1, "rare": 4 },          rarity name to power per level
 *   "default_rarity": "common",                         for enchantments not listed
 *   "enchantments": { "minecraft:mending": "rare" } }   enchantment id to rarity name
 * </pre>
 * This is a trust boundary: a wrong entry is dropped with a warning and the rest still loads. Without any file every level is worth 1.
 */
public record AltarPowerTable(Map<String, Integer> rarities, Map<String, String> enchantments, String defaultRarity) {
    public static final AltarPowerTable EMPTY = new AltarPowerTable(Map.of(), Map.of(), "");
    private static volatile AltarPowerTable current = EMPTY;

    public static AltarPowerTable current() {
        return current;
    }

    public static void install(AltarPowerTable table) {
        current = table;
    }

    /** Power per level of the enchantment {@code id} ("namespace:path"): its rarity's weight, else the default rarity's, else 1. */
    public int weight(String id) {
        Integer w = rarities.get(enchantments.getOrDefault(id, defaultRarity));
        if (w == null) w = rarities.get(defaultRarity);
        return w == null ? 1 : w;
    }

    /** Merges the files in load order (lowest priority first). */
    public static AltarPowerTable parse(List<String> files, Consumer<String> warn) {
        Map<String, Integer> rarities = new HashMap<>();
        Map<String, String> enchantments = new HashMap<>();
        String defaultRarity = "";
        for (String text : files) {
            JsonObject o;
            try {
                o = JsonParser.parseString(text).getAsJsonObject();
            } catch (RuntimeException ex) {
                warn.accept("altar_power.json ignored: " + ex.getMessage());
                continue;
            }
            if (o.has("rarities") && o.get("rarities").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("rarities").entrySet()) {
                    try {
                        int w = e.getValue().getAsInt();
                        if (w < 0) throw new IllegalArgumentException("negative");
                        rarities.put(e.getKey(), w);
                    } catch (RuntimeException ex) {
                        warn.accept("rarity " + e.getKey() + " ignored: needs a whole number of 0 or more");
                    }
                }
            }
            if (o.has("default_rarity")) {
                try {
                    defaultRarity = o.get("default_rarity").getAsString();
                } catch (RuntimeException ex) {
                    warn.accept("default_rarity ignored: needs a rarity name");
                }
            }
            if (o.has("enchantments") && o.get("enchantments").isJsonObject()) {
                for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("enchantments").entrySet()) {
                    try {
                        String id = e.getKey().contains(":") ? e.getKey() : "minecraft:" + e.getKey();
                        enchantments.put(id, e.getValue().getAsString());
                    } catch (RuntimeException ex) {
                        warn.accept("enchantment " + e.getKey() + " ignored: needs a rarity name");
                    }
                }
            }
        }
        for (Map.Entry<String, String> e : enchantments.entrySet()) {
            if (!rarities.containsKey(e.getValue())) warn.accept("enchantment " + e.getKey() + " has unknown rarity " + e.getValue() + "; it uses the default");
        }
        if (!defaultRarity.isEmpty() && !rarities.containsKey(defaultRarity)) warn.accept("default_rarity " + defaultRarity + " is unknown; unlisted enchantments are worth 1");
        return new AltarPowerTable(Map.copyOf(rarities), Map.copyOf(enchantments), defaultRarity);
    }
}
