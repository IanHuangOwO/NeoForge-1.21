package com.mamechin.enchantmod.block.scribing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads a blueprint and works out which enchants it would make and the exact chance of each level, with no game code so
 * it can be tested on its own.
 *
 * Rules (see docs/DESIGN.md): a group of core runes joined by lines that matches a row of the core table is an enchant;
 * each boost rune (Uruz) joined to it adds a level; Mannaz is needed from level 5 and Dagaz from level 10, one more
 * Dagaz for each further 10 levels, or the level stops below the threshold. Every line can break at the altar: the
 * chance rises with its length, and a link between two runes only breaks when all its parallel lines do.
 */
public final class EnchantForecast {
    public static final double BASE_BREAK = 0.04, BREAK_PER_CELL = 0.01, MAX_BREAK = 0.95;

    /** One enchant the blueprint forms. levels maps a level to its chance; broken is the chance the core itself fails. */
    public record Result(String enchant, Map<Integer, Double> levels, double broken) {}

    /** Core table: sorted rune ids joined by "+" to the vanilla enchantment id. A shortcut until the full table is designed. */
    private static final Map<String, String> CORES = new HashMap<>();

    /** One row of the core table, for the research handbook. */
    public record Core(String enchant, List<String> runes) {}

    private static final List<Core> TABLE = new ArrayList<>();

    /** The core table in display order. */
    public static List<Core> table() {
        return TABLE;
    }

    private static void core(String enchant, String... runes) {
        TABLE.add(new Core(enchant, List.of(runes)));
        List<String> sorted = new ArrayList<>(List.of(runes));
        sorted.sort(null);
        CORES.put(String.join("+", sorted), enchant);
    }

    static {
        core("efficiency", "berkana");
        core("unbreaking", "isa");
        core("fortune", "fehu");
        core("mending", "jera");
        core("silk_touch", "gebo", "isa");
        core("sharpness", "tiwaz");
        core("smite", "tiwaz", "sowilo");
        core("bane_of_arthropods", "tiwaz", "thurisaz");
        core("knockback", "thurisaz");
        core("fire_aspect", "kenaz");
        core("looting", "tiwaz", "fehu");
        core("sweeping_edge", "tiwaz", "ehwaz");
        core("protection", "algiz");
        core("fire_protection", "algiz", "kenaz");
        core("blast_protection", "algiz", "hagalaz");
        core("feather_falling", "algiz", "ansuz");
        core("respiration", "algiz", "laguz");
        core("thorns", "algiz", "thurisaz");
        core("depth_strider", "laguz", "ehwaz");
        core("frost_walker", "laguz", "isa");
        core("soul_speed", "ehwaz", "nauthiz");
        core("swift_sneak", "ehwaz", "perthro");
        core("power", "eihwaz");
        core("flame", "eihwaz", "kenaz");
        core("punch", "eihwaz", "thurisaz");
        core("infinity", "eihwaz", "jera");
    }

    /** Chance one line breaks, from its length in cells. */
    public static double breakChance(Blueprint.Line line) {
        return Math.min(MAX_BREAK, BASE_BREAK + BREAK_PER_CELL * line.path().size());
    }

    /** Highest level the stabilizers allow: below 5 without Mannaz, below 10 without Dagaz, then 10 more per Dagaz. */
    static int levelCap(int mannaz, int dagaz) {
        if (mannaz == 0) return 4;
        return dagaz == 0 ? 9 : 10 * dagaz + 9;
    }

    private static long key(int a, int b) {
        return ((long) Math.min(a, b) << 32) | Math.max(a, b);
    }

    /** Chance that exactly k of these independent events happen, for every k. */
    private static double[] countDistribution(List<Double> chances) {
        double[] dist = new double[chances.size() + 1];
        dist[0] = 1;
        int n = 0;
        for (double p : chances) {
            n++;
            for (int k = n; k >= 0; k--) dist[k] = dist[k] * (1 - p) + (k > 0 ? dist[k - 1] * p : 0);
        }
        return dist;
    }

    public static List<Result> forecast(Blueprint bp) {
        Map<Integer, String> runes = new TreeMap<>();
        for (Blueprint.RuneAt r : bp.runes()) runes.put(r.cell(), r.rune());

        // chance that every line of a link breaks, per pair of runes
        Map<Long, Double> allBroken = new HashMap<>();
        for (Blueprint.Line l : bp.lines()) allBroken.merge(key(l.a(), l.b()), breakChance(l), (x, y) -> x * y);

        // groups of core runes joined by links
        Map<Integer, Integer> parent = new HashMap<>();
        for (int cell : runes.keySet()) if (!isUtility(runes.get(cell))) parent.put(cell, cell);
        for (long k : allBroken.keySet()) {
            int a = (int) (k >> 32), b = (int) k;
            if (parent.containsKey(a) && parent.containsKey(b)) parent.put(find(parent, a), find(parent, b));
        }
        Map<Integer, List<Integer>> groups = new TreeMap<>();
        for (int cell : parent.keySet()) groups.computeIfAbsent(find(parent, cell), x -> new ArrayList<>()).add(cell);

        List<Result> results = new ArrayList<>();
        for (List<Integer> members : groups.values()) {
            List<String> ids = new ArrayList<>();
            for (int cell : members) ids.add(runes.get(cell));
            ids.sort(null);
            String enchant = CORES.get(String.join("+", ids));
            if (enchant == null) continue;

            double intact = 1; // every table row has at most two runes, so the core holds while their link does
            if (members.size() == 2) intact = 1 - allBroken.get(key(members.get(0), members.get(1)));

            List<Double> boosts = new ArrayList<>(), mannaz = new ArrayList<>(), dagaz = new ArrayList<>();
            for (Map.Entry<Integer, String> e : runes.entrySet()) {
                if (!isUtility(e.getValue())) continue;
                double allLost = 1;
                for (int member : members) allLost *= allBroken.getOrDefault(key(e.getKey(), member), 1.0);
                double attached = 1 - allLost;
                switch (e.getValue()) {
                    case "uruz" -> boosts.add(attached);
                    case "mannaz" -> mannaz.add(attached);
                    default -> dagaz.add(attached);
                }
            }
            double[] pb = countDistribution(boosts), pm = countDistribution(mannaz), pd = countDistribution(dagaz);
            Map<Integer, Double> levels = new TreeMap<>();
            for (int b = 0; b < pb.length; b++) {
                for (int m = 0; m < pm.length; m++) {
                    for (int d = 0; d < pd.length; d++) {
                        int level = Math.min(1 + b, levelCap(m, d));
                        levels.merge(level, intact * pb[b] * pm[m] * pd[d], Double::sum);
                    }
                }
            }
            results.add(new Result(enchant, levels, 1 - intact));
        }
        return results;
    }

    private static boolean isUtility(String runeId) {
        Rune rune = Rune.byId(runeId);
        return rune != null && rune.isUtility();
    }

    private static int find(Map<Integer, Integer> parent, int x) {
        while (parent.get(x) != x) x = parent.get(x);
        return x;
    }

    private EnchantForecast() {}
}
