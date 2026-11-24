package com.mamechin.enchantmod.balance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The tunable numbers of the altar, the enchant cubes, the quills, the printing press, the cauldron heat sources and the book level cap, read from data packs (pure). Each lives in
 * {@code data/<namespace>/enchantmod/} at the path in {@link #FILES}; every pack's copy is merged key by key in load order (objects
 * merge, anything else is replaced), so a pack changes single values. This is a trust boundary: a wrong value is dropped with a
 * warning and the default (the shipped number) is used. The server sends the merged files to clients, which parse them the same way.
 */
public record Balance(Altar altar, Map<String, Cube> cubes, Map<String, Quill> quills, Press press, Map<String, Heat> heat, Books books, Yggdrasil yggdrasil) {
    /** File name to its path under {@code data/<namespace>/enchantmod/}, grouped by feature. */
    public static final Map<String, String> FILES = Map.of("altar", "altar/altar.json", "cubes", "items/cubes.json", "quills", "items/quills.json", "press", "press/press.json",
            "heat", "cauldron/heat.json", "books", "items/books.json",
            "yggdrasil", "items/yggdrasil.json");

    /** The scan cylinder of a tier: radius and the lowest and highest row, all relative to the altar. */
    public record Area(int radius, int minY, int maxY) {}

    /**
     * Spin timing in ticks, the scan area per tier (index = tier; a higher tier uses the last), the cost per enchant level the cube
     * can reach, how many books one shelf gives, how many allays one altar can bind (-1 = no limit), and the book weights: rarity name to power per level, enchantment id to rarity name.
     */
    public record Altar(int rampUp, int rampDown, int drainTicksPerBook, int costPerLevel, int booksPerShelf, int maxAllays, List<Area> scan,
            Map<String, Integer> rarities, Map<String, String> enchantments, String defaultRarity) {
        /** Power per level of the enchantment {@code id} ("namespace:path"): its rarity's weight, else the default rarity's, else 1. */
        public int weight(String id) {
            Integer w = rarities.get(enchantments.getOrDefault(id, defaultRarity));
            if (w == null) w = rarities.get(defaultRarity);
            return w == null ? 1 : w;
        }

        /** Power it takes to apply {@code id} at {@code level}: level x rarity weight x the cost per level (author, 2026-11-24: more than a book of it gives). */
        public int cost(String id, int level) {
            return level * weight(id) * costPerLevel;
        }

        public Area area(int tier) {
            return scan.get(Math.max(0, Math.min(tier, scan.size() - 1)));
        }
    }

    /** An enchant cube: how many templates the press presses into it (1 to 6), and the highest level of one enchantment it carries. */
    public record Cube(int capacity, int maxLevel) {}

    /** A quill's book rite: the vanilla enchanting power it rolls with and the experience levels it costs. */
    public record Quill(int power, int xpLevels) {}

    /** A block that heats a cauldron standing on it: heat tier, and whether a block with a LIT property must be lit. */
    public record Heat(int tier, boolean requiresLit) {}

    /**
     * The book level cap (author, 2026-11-22): whether it is on, and the most enchant levels, summed over all its enchants, a book of
     * each tier can hold (index = tier; a higher tier uses the last).
     */
    public record Books(boolean limitEnabled, List<Integer> maxLevels) {
        /** The cap of a book of {@code tier}, or -1 when the limit is off. */
        public int cap(int tier) {
            return limitEnabled ? maxLevels.get(Math.max(0, Math.min(tier, maxLevels.size() - 1))) : -1;
        }
    }

    /**
     * A Yggdrasil growth stage (author, 2026-11-23): its name (lang key {@code item.enchantmod.yggdrasil.<name>}), the age it starts at,
     * the XP points each year of growth costs, the XP points one refilled enchant level costs, and the seconds between refill checks.
     */
    public record Stage(String name, int age, int yearCost, int xpPerLevel, int checkSeconds) {}

    /** The Yggdrasil: game ticks per year of age, and its stages in rising age, the first at 0. */
    public record Yggdrasil(int ticksPerYear, List<Stage> stages) {
        /** The index of the stage a tree of {@code age} years is in. */
        public int index(int age) {
            int i = 0;
            while (i + 1 < stages.size() && stages.get(i + 1).age() <= age) i++;
            return i;
        }

        public Stage stage(int age) {
            return stages.get(index(age));
        }
    }

    /** Ticks the press takes per template (the stroke animation is played to fit). */
    public record Press(int ticksPerTemplate) {}

    public static final Altar DEFAULT_ALTAR = new Altar(60, 40, 100, 2, 1, -1,
            List.of(new Area(8, -13, 13), new Area(16, -15, 15), new Area(32, -17, 17)), Map.of(), Map.of(), "");
    public static final Press DEFAULT_PRESS = new Press(20);
    public static final Books DEFAULT_BOOKS = new Books(true, List.of(2, 4, 16));
    public static final Yggdrasil DEFAULT_YGGDRASIL = new Yggdrasil(24000, List.of(new Stage("seed", 0, 7, 50, 120), new Stage("sprout", 10, 10, 40, 90),
            new Stage("sapling", 50, 15, 30, 60), new Stage("young_tree", 150, 25, 22, 40), new Stage("elder_tree", 400, 40, 15, 25),
            new Stage("yggdrasil", 1000, 60, 10, 2)));
    public static final Balance DEFAULT = new Balance(DEFAULT_ALTAR, Map.of(), Map.of(), DEFAULT_PRESS, Map.of(), DEFAULT_BOOKS, DEFAULT_YGGDRASIL);

    private static volatile Balance current = DEFAULT;

    public static Balance current() {
        return current;
    }

    public static void install(Balance balance) {
        current = balance;
    }

    /** The cube's numbers from cubes.json, or {@code fallback} (the item's own) when it is not listed. */
    public Cube cube(String itemId, Cube fallback) {
        return cubes.getOrDefault(itemId, fallback);
    }

    public Quill quill(String itemId, Quill fallback) {
        return quills.getOrDefault(itemId, fallback);
    }

    /** Merges the copies of one file, lowest priority first, into one JSON text; a copy that is not a JSON object is skipped. */
    public static String merge(List<String> copies, Consumer<String> warn) {
        JsonObject out = new JsonObject();
        for (String text : copies) {
            try {
                mergeInto(out, JsonParser.parseString(text).getAsJsonObject());
            } catch (RuntimeException ex) {
                warn.accept("a copy was ignored: " + ex.getMessage());
            }
        }
        return out.toString();
    }

    private static void mergeInto(JsonObject into, JsonObject from) {
        for (Map.Entry<String, JsonElement> e : from.entrySet()) {
            JsonElement old = into.get(e.getKey());
            if (old != null && old.isJsonObject() && e.getValue().isJsonObject()) mergeInto(old.getAsJsonObject(), e.getValue().getAsJsonObject());
            else into.add(e.getKey(), e.getValue().deepCopy());
        }
    }

    /** Parses the merged files (name to JSON text, names as the keys of {@link #FILES}); a missing file or value keeps its default. */
    public static Balance parse(Map<String, String> files, Consumer<String> warn) {
        return new Balance(altar(object(files.get("altar"), "altar", warn), warn), cubes(object(files.get("cubes"), "cubes", warn), warn),
                quills(object(files.get("quills"), "quills", warn), warn), press(object(files.get("press"), "press", warn), warn),
                heat(object(files.get("heat"), "heat", warn), warn), books(object(files.get("books"), "books", warn), warn),
                yggdrasil(object(files.get("yggdrasil"), "yggdrasil", warn), warn));
    }

    private static JsonObject object(String text, String name, Consumer<String> warn) {
        if (text == null) return new JsonObject();
        try {
            return JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException ex) {
            warn.accept(name + ".json ignored: " + ex.getMessage());
            return new JsonObject();
        }
    }

    /** An int field of {@code o}, at least {@code min}, or {@code fallback} (with a warning when present but wrong). */
    private static int integer(JsonObject o, String key, int fallback, int min, String where, Consumer<String> warn) {
        if (o == null || !o.has(key)) return fallback;
        try {
            int v = o.get(key).getAsInt();
            if (v >= min) return v;
        } catch (RuntimeException ignored) {
        }
        warn.accept(where + "." + key + " ignored: needs a whole number of " + min + " or more");
        return fallback;
    }

    private static JsonObject child(JsonObject o, String key, String where, Consumer<String> warn) {
        if (!o.has(key)) return null;
        if (o.get(key).isJsonObject()) return o.getAsJsonObject(key);
        warn.accept(where + "." + key + " ignored: needs an object");
        return null;
    }

    private static Altar altar(JsonObject o, Consumer<String> warn) {
        Altar d = DEFAULT_ALTAR;
        JsonObject spin = child(o, "spin", "altar", warn), cost = child(o, "cost", "altar", warn);
        int rampUp = integer(spin, "ramp_up_ticks", d.rampUp(), 1, "altar.spin", warn);
        int rampDown = integer(spin, "ramp_down_ticks", d.rampDown(), 1, "altar.spin", warn);
        int drainTicksPerBook = integer(o, "drain_ticks_per_book", d.drainTicksPerBook(), 1, "altar", warn);
        int costPerLevel = integer(cost, "power_per_level", d.costPerLevel(), 0, "altar.cost", warn);
        int booksPerShelf = integer(o, "books_per_shelf", d.booksPerShelf(), 1, "altar", warn);
        int maxAllays = integer(o, "max_allays", d.maxAllays(), -1, "altar", warn);

        List<Area> scan = d.scan();
        if (o.has("scan")) {
            try {
                List<Area> list = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("scan")) {
                    JsonObject a = e.getAsJsonObject();
                    int radius = integer(a, "radius", -1, 0, "altar.scan", warn);
                    if (radius < 0 || !a.has("min_y") || !a.has("max_y")) throw new IllegalArgumentException("each tier needs radius, min_y and max_y");
                    int minY = a.get("min_y").getAsInt(), maxY = a.get("max_y").getAsInt();
                    if (maxY < minY) throw new IllegalArgumentException("max_y below min_y");
                    list.add(new Area(Math.min(radius, 32), minY, maxY)); // radius capped at 32: the scan reads the block entities of the chunks the area touches
                }
                if (list.isEmpty()) throw new IllegalArgumentException("empty");
                scan = List.copyOf(list);
            } catch (RuntimeException ex) {
                warn.accept("altar.scan ignored: " + ex.getMessage());
            }
        }

        Map<String, Integer> rarities = new HashMap<>();
        JsonObject r = child(o, "rarities", "altar", warn);
        if (r != null) {
            for (String name : r.keySet()) {
                int w = integer(r, name, -1, 0, "altar.rarities", warn);
                if (w >= 0) rarities.put(name, w);
            }
        }
        String defaultRarity = "";
        if (o.has("default_rarity")) {
            try {
                defaultRarity = o.get("default_rarity").getAsString();
            } catch (RuntimeException ex) {
                warn.accept("altar.default_rarity ignored: needs a rarity name");
            }
        }
        Map<String, String> enchantments = new HashMap<>();
        JsonObject en = child(o, "enchantments", "altar", warn);
        if (en != null) {
            for (Map.Entry<String, JsonElement> e : en.entrySet()) {
                String id = e.getKey().contains(":") ? e.getKey() : "minecraft:" + e.getKey();
                try {
                    String rarity = e.getValue().getAsString();
                    if (!rarities.containsKey(rarity)) warn.accept("altar.enchantments." + id + " has unknown rarity " + rarity + "; it uses the default");
                    enchantments.put(id, rarity);
                } catch (RuntimeException ex) {
                    warn.accept("altar.enchantments." + id + " ignored: needs a rarity name");
                }
            }
        }
        if (!defaultRarity.isEmpty() && !rarities.containsKey(defaultRarity)) warn.accept("altar.default_rarity " + defaultRarity + " is unknown; unlisted enchantments are worth 1");
        return new Altar(rampUp, rampDown, drainTicksPerBook, costPerLevel, booksPerShelf, maxAllays, scan, Map.copyOf(rarities), Map.copyOf(enchantments), defaultRarity);
    }

    private static Map<String, Cube> cubes(JsonObject o, Consumer<String> warn) {
        Map<String, Cube> out = new HashMap<>();
        for (String id : o.keySet()) {
            JsonObject c = child(o, id, "cubes", warn);
            if (c == null) continue;
            int capacity = integer(c, "capacity", -1, 1, "cubes." + id, warn), maxLevel = integer(c, "max_level", -1, 1, "cubes." + id, warn);
            if (capacity < 0 || maxLevel < 0) {
                warn.accept("cubes." + id + " ignored: needs capacity and max_level");
                continue;
            }
            out.put(id, new Cube(Math.min(capacity, 6), maxLevel)); // the press has six template slots
        }
        return Map.copyOf(out);
    }

    private static Map<String, Quill> quills(JsonObject o, Consumer<String> warn) {
        Map<String, Quill> out = new HashMap<>();
        for (String id : o.keySet()) {
            JsonObject q = child(o, id, "quills", warn);
            if (q == null) continue;
            int power = integer(q, "power", -1, 1, "quills." + id, warn), xp = integer(q, "xp_levels", -1, 0, "quills." + id, warn);
            if (power < 0 || xp < 0) {
                warn.accept("quills." + id + " ignored: needs power and xp_levels");
                continue;
            }
            out.put(id, new Quill(power, xp));
        }
        return Map.copyOf(out);
    }

    private static Map<String, Heat> heat(JsonObject o, Consumer<String> warn) {
        Map<String, Heat> out = new HashMap<>();
        for (String id : o.keySet()) {
            JsonObject h = child(o, id, "heat", warn);
            if (h == null) continue;
            int tier = integer(h, "tier", -1, 1, "heat." + id, warn);
            if (tier < 0) {
                warn.accept("heat." + id + " ignored: needs a tier");
                continue;
            }
            boolean lit = false;
            if (h.has("requires_lit")) {
                try {
                    lit = h.get("requires_lit").getAsBoolean();
                } catch (RuntimeException ex) {
                    warn.accept("heat." + id + ".requires_lit ignored: needs true or false");
                }
            }
            out.put(id.contains(":") ? id : "minecraft:" + id, new Heat(tier, lit));
        }
        return Map.copyOf(out);
    }

    private static Books books(JsonObject o, Consumer<String> warn) {
        boolean enabled = DEFAULT_BOOKS.limitEnabled();
        if (o.has("limit_enabled")) {
            try {
                enabled = o.get("limit_enabled").getAsBoolean();
            } catch (RuntimeException ex) {
                warn.accept("books.limit_enabled ignored: needs true or false");
            }
        }
        List<Integer> caps = DEFAULT_BOOKS.maxLevels();
        if (o.has("max_levels")) {
            try {
                List<Integer> list = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("max_levels")) {
                    int v = e.getAsInt();
                    if (v < 1) throw new IllegalArgumentException("each cap needs to be 1 or more");
                    list.add(v);
                }
                if (list.isEmpty()) throw new IllegalArgumentException("empty");
                caps = List.copyOf(list);
            } catch (RuntimeException ex) {
                warn.accept("books.max_levels ignored: " + ex.getMessage());
            }
        }
        return new Books(enabled, caps);
    }

    private static Yggdrasil yggdrasil(JsonObject o, Consumer<String> warn) {
        Yggdrasil d = DEFAULT_YGGDRASIL;
        int ticksPerYear = integer(o, "ticks_per_year", d.ticksPerYear(), 1, "yggdrasil", warn);
        List<Stage> stages = d.stages();
        if (o.has("stages")) {
            try {
                List<Stage> list = new ArrayList<>();
                for (JsonElement e : o.getAsJsonArray("stages")) {
                    JsonObject s = e.getAsJsonObject();
                    Stage stage = new Stage(s.get("name").getAsString(), s.get("age").getAsInt(), s.get("year_cost").getAsInt(),
                            s.get("xp_per_level").getAsInt(), s.get("check_seconds").getAsInt());
                    if (stage.yearCost() < 0 || stage.xpPerLevel() < 0 || stage.checkSeconds() < 1) throw new IllegalArgumentException(stage.name() + " has a negative cost or a check under 1 second");
                    if (list.isEmpty() ? stage.age() != 0 : stage.age() <= list.get(list.size() - 1).age()) throw new IllegalArgumentException("ages must start at 0 and rise");
                    list.add(stage);
                }
                if (list.isEmpty()) throw new IllegalArgumentException("empty");
                stages = List.copyOf(list);
            } catch (RuntimeException ex) {
                warn.accept("yggdrasil.stages ignored: " + ex.getMessage());
            }
        }
        return new Yggdrasil(ticksPerYear, stages);
    }

    private static Press press(JsonObject o, Consumer<String> warn) {
        return new Press(integer(o, "ticks_per_template", DEFAULT_PRESS.ticksPerTemplate(), 1, "press", warn));
    }
}
