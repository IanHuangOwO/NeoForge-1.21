package com.mamechin.enchantmod.scribing.rules;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Turns the rune, enchant and forecast files into definitions. This is the trust boundary for data packs: an entry that is
 * wrong in any way is dropped with a warning and the rest still load, so one bad file cannot break the mod. Pure, no Minecraft.
 */
public final class DefinitionParser {
    /** What parse returns; Definitions.install turns it into the current set. */
    public record Parsed(List<Rune> runes, List<Enchant> enchants, ForecastRules rules, Definitions.Raw raw, Map<String, Rune> runeById, Map<String, Enchant> byRunes, Map<String, TemplateLimits> templates) {}

    private DefinitionParser() {}

    public static Parsed parse(Definitions.Raw raw, Consumer<String> warn) {
        List<Rune> runes = new ArrayList<>();
        Map<String, Rune> byId = new HashMap<>();
        for (Map.Entry<String, String> e : new TreeMap<>(raw.runes()).entrySet()) {
            try {
                Rune rune = rune(e.getKey(), JsonParser.parseString(e.getValue()).getAsJsonObject());
                runes.add(rune);
                byId.put(rune.id(), rune);
            } catch (RuntimeException ex) {
                warn.accept("rune " + e.getKey() + " ignored: " + ex.getMessage());
            }
        }
        runes.sort(Comparator.comparingInt(Rune::order).thenComparing(Rune::id));

        List<Enchant> enchants = new ArrayList<>();
        for (Map.Entry<String, String> e : new TreeMap<>(raw.enchants()).entrySet()) {
            try {
                enchants.add(enchant(e.getKey(), JsonParser.parseString(e.getValue()).getAsJsonObject(), byId));
            } catch (RuntimeException ex) {
                warn.accept("enchant " + e.getKey() + " ignored: " + ex.getMessage());
            }
        }
        enchants.sort(Comparator.comparingInt(Enchant::order).thenComparing(Enchant::id));
        Map<String, Enchant> byRunes = new HashMap<>();
        List<Enchant> kept = new ArrayList<>();
        for (Enchant enchant : enchants) {
            String key = String.join("+", enchant.runes());
            if (byRunes.containsKey(key)) { // the earlier row in handbook order keeps the runes
                warn.accept("enchant " + enchant.id() + " ignored: " + byRunes.get(key).id() + " already uses the same runes");
                continue;
            }
            byRunes.put(key, enchant);
            kept.add(enchant);
        }

        Map<String, TemplateLimits> templates = new HashMap<>();
        for (Map.Entry<String, String> e : new TreeMap<>(raw.templates()).entrySet()) {
            try {
                templates.put(e.getKey(), template(e.getKey(), JsonParser.parseString(e.getValue()).getAsJsonObject()));
            } catch (RuntimeException ex) {
                warn.accept("template " + e.getKey() + " ignored: " + ex.getMessage());
            }
        }

        ForecastRules rules = ForecastRules.DEFAULT;
        if (raw.forecast() != null) {
            try {
                rules = rules(JsonParser.parseString(raw.forecast()).getAsJsonObject());
            } catch (RuntimeException ex) {
                warn.accept("forecast.json ignored: " + ex.getMessage());
            }
        }
        return new Parsed(List.copyOf(runes), List.copyOf(kept), rules, raw, Map.copyOf(byId), Map.copyOf(byRunes), Map.copyOf(templates));
    }

    private static Rune rune(String id, JsonObject o) {
        String glyph = string(o, "glyph");
        if (glyph.codePointCount(0, glyph.length()) != 1) throw new IllegalArgumentException("glyph must be exactly one character");
        int size = integer(o, "size", -1);
        if (size < 1 || size > 3) throw new IllegalArgumentException("size must be 1, 2 or 3");
        Rune.Role role;
        try {
            role = Rune.Role.valueOf(string(o, "role").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("role must be core, boost or stabilizer");
        }
        int boost = integer(o, "levels", 1), unlock = integer(o, "unlock_level", 0), extra = integer(o, "extra_levels", 0);
        if (role == Rune.Role.BOOST && boost < 1) throw new IllegalArgumentException("levels must be at least 1");
        if (role == Rune.Role.STABILIZER && (unlock < 2 || extra < 0)) throw new IllegalArgumentException("unlock_level must be at least 2 and extra_levels not negative");
        return new Rune(id, glyph, size, role, integer(o, "order", 0), boost, unlock, extra);
    }

    private static Enchant enchant(String id, JsonObject o, Map<String, Rune> runes) {
        String enchantment = string(o, "enchantment");
        if (enchantment.indexOf(':') < 0) enchantment = "minecraft:" + enchantment;
        if (!enchantment.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) throw new IllegalArgumentException("enchantment is not a valid id: " + enchantment);
        JsonArray array = o.has("runes") ? o.getAsJsonArray("runes") : new JsonArray();
        Set<String> ids = new HashSet<>();
        for (JsonElement el : array) {
            String runeId = Definitions.canonical(el.getAsString());
            Rune rune = runes.get(runeId);
            if (rune == null) throw new IllegalArgumentException("unknown rune " + runeId);
            if (rune.isUtility()) throw new IllegalArgumentException(runeId + " is not a core rune");
            ids.add(runeId);
        }
        if (ids.size() != array.size() || ids.isEmpty() || ids.size() > 3) throw new IllegalArgumentException("needs one to three different runes");
        List<String> sorted = new ArrayList<>(ids);
        sorted.sort(null);
        String category = o.has("category") ? o.get("category").getAsString() : "general";
        return new Enchant(id, enchantment, List.copyOf(sorted), EnchantGroups.CATEGORY_ORDER.contains(category) ? category : "general", integer(o, "order", 0));
    }

    private static TemplateLimits template(String item, JsonObject o) {
        int width = integer(o, "width", BlueprintRules.WIDTH), height = integer(o, "height", BlueprintRules.HEIGHT), maxLevel = integer(o, "max_level", 0);
        if (width < 1 || width > BlueprintRules.WIDTH || height < 1 || height > BlueprintRules.HEIGHT) {
            throw new IllegalArgumentException("width must be 1 to " + BlueprintRules.WIDTH + " and height 1 to " + BlueprintRules.HEIGHT);
        }
        if (maxLevel < 0) throw new IllegalArgumentException("max_level must be 0 (no cap) or more");
        return new TemplateLimits(item, width, height, maxLevel == 0 ? Integer.MAX_VALUE : maxLevel);
    }

    private static ForecastRules rules(JsonObject o) {
        ForecastRules d = ForecastRules.DEFAULT;
        if (!o.has("line_break")) return d;
        JsonObject b = o.getAsJsonObject("line_break");
        double base = number(b, "base", d.base()), perCell = number(b, "per_cell", d.perCell()), max = number(b, "max", d.max());
        if (base < 0 || perCell < 0 || max < 0 || max > 1) throw new IllegalArgumentException("line_break numbers must be 0 or more and max at most 1");
        return new ForecastRules(base, perCell, max);
    }

    private static String string(JsonObject o, String key) {
        if (!o.has(key)) throw new IllegalArgumentException("missing " + key);
        return o.get(key).getAsString();
    }

    private static int integer(JsonObject o, String key, int fallback) {
        return o.has(key) ? o.get(key).getAsInt() : fallback;
    }

    private static double number(JsonObject o, String key, double fallback) {
        return o.has(key) ? o.get(key).getAsDouble() : fallback;
    }
}
