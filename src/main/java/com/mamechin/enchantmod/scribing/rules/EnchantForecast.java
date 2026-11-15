package com.mamechin.enchantmod.scribing.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Reads a blueprint and works out which enchants it would make and the exact chance of each level, with no game code so
 * it can be tested on its own. The runes, the enchant rows and the numbers come from Definitions (JSON files).
 *
 * Rules (see docs/DESIGN.md): a group of core runes joined by lines that matches an enchant row is an enchant; each boost
 * rune joined to it adds levels; a stabilizer rune unlocks the levels from its unlock level up, and each copy after the
 * first allows more. Every line can break at the altar: the chance rises with its length, and a link between two runes
 * only breaks when all its parallel lines do. The core holds while its runes stay connected.
 */
public final class EnchantForecast {
    /** One enchant the blueprint forms. levels maps a level to its chance; broken is the chance the core itself fails. */
    public record Result(Enchant enchant, Map<Integer, Double> levels, double broken) {}

    /** Chance one line breaks, from its length in cells. */
    public static double breakChance(Blueprint.Line line) {
        ForecastRules rules = Definitions.rules();
        return Math.min(rules.max(), rules.base() + rules.perCell() * line.path().size());
    }

    /** Highest level the stabilizers allow, given how many of each (by rune id) are joined; unlimited when none gates. */
    static int levelCap(Map<String, Integer> joined) {
        int cap = Integer.MAX_VALUE;
        for (Rune rune : Definitions.runes()) {
            if (rune.role() != Rune.Role.STABILIZER) continue;
            int count = joined.getOrDefault(rune.id(), 0);
            if (count == 0) cap = Math.min(cap, rune.unlockLevel() - 1);
            else if (rune.extraLevels() > 0) cap = Math.min(cap, rune.unlockLevel() - 1 + rune.extraLevels() * count);
        }
        return cap;
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
        Map<Integer, Rune> runes = new TreeMap<>();
        for (Blueprint.RuneAt r : bp.runes()) {
            Rune rune = Rune.byId(r.rune());
            if (rune != null) runes.put(r.cell(), rune);
        }

        // chance that every line of a link breaks, per pair of runes
        Map<Long, Double> allBroken = new HashMap<>();
        for (Blueprint.Line l : bp.lines()) allBroken.merge(key(l.a(), l.b()), breakChance(l), (x, y) -> x * y);

        // groups of core runes joined by links
        Map<Integer, Integer> parent = new HashMap<>();
        for (Map.Entry<Integer, Rune> e : runes.entrySet()) if (!e.getValue().isUtility()) parent.put(e.getKey(), e.getKey());
        for (long k : allBroken.keySet()) {
            int a = (int) (k >> 32), b = (int) k;
            if (parent.containsKey(a) && parent.containsKey(b)) parent.put(find(parent, a), find(parent, b));
        }
        Map<Integer, List<Integer>> groups = new TreeMap<>();
        for (int cell : parent.keySet()) groups.computeIfAbsent(find(parent, cell), x -> new ArrayList<>()).add(cell);

        List<Result> results = new ArrayList<>();
        for (List<Integer> members : groups.values()) {
            List<String> ids = new ArrayList<>();
            for (int cell : members) ids.add(runes.get(cell).id());
            ids.sort(null);
            Enchant enchant = Definitions.enchantFor(ids);
            if (enchant == null) continue;

            double intact = connected(members, allBroken);

            // how many of each utility rune stay joined to the core: a distribution per rune kind
            Map<String, List<Double>> attached = new HashMap<>();
            for (Map.Entry<Integer, Rune> e : runes.entrySet()) {
                if (!e.getValue().isUtility()) continue;
                double allLost = 1;
                for (int member : members) allLost *= allBroken.getOrDefault(key(e.getKey(), member), 1.0);
                attached.computeIfAbsent(e.getValue().id(), x -> new ArrayList<>()).add(1 - allLost);
            }
            List<Rune> kinds = new ArrayList<>();
            List<double[]> dists = new ArrayList<>();
            for (Rune rune : Definitions.runes()) {
                if (!rune.isUtility()) continue;
                kinds.add(rune);
                dists.add(countDistribution(attached.getOrDefault(rune.id(), List.of()))); // a kind that is absent is {1}: none joined
            }
            Map<Integer, Double> levels = new TreeMap<>();
            walk(kinds, dists, 0, intact, new int[kinds.size()], levels);
            results.add(new Result(enchant, levels, 1 - intact));
        }
        return results;
    }

    /** Adds the chance of every combination of joined utility runes to the level it gives. */
    private static void walk(List<Rune> kinds, List<double[]> dists, int i, double chance, int[] counts, Map<Integer, Double> levels) {
        if (chance == 0) return;
        if (i == kinds.size()) {
            int level = 1;
            Map<String, Integer> joined = new HashMap<>();
            for (int k = 0; k < kinds.size(); k++) {
                joined.put(kinds.get(k).id(), counts[k]);
                if (kinds.get(k).role() == Rune.Role.BOOST) level += counts[k] * kinds.get(k).boostLevels();
            }
            levels.merge(Math.min(level, levelCap(joined)), chance, Double::sum);
            return;
        }
        for (int c = 0; c < dists.get(i).length; c++) {
            counts[i] = c;
            walk(kinds, dists, i + 1, chance * dists.get(i)[c], counts, levels);
        }
    }

    /** Chance that the runes of a group are still joined once links have broken (a group has at most three runes, so at most three links). */
    private static double connected(List<Integer> members, Map<Long, Double> allBroken) {
        if (members.size() == 1) return 1;
        List<long[]> links = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            for (int j = i + 1; j < members.size(); j++) {
                long k = key(members.get(i), members.get(j));
                if (allBroken.containsKey(k)) links.add(new long[] {i, j, k});
            }
        }
        double total = 0;
        for (int mask = 0; mask < 1 << links.size(); mask++) {
            double p = 1;
            int[] p2 = new int[members.size()];
            for (int i = 0; i < p2.length; i++) p2[i] = i;
            for (int l = 0; l < links.size(); l++) {
                double broken = allBroken.get((long) links.get(l)[2]);
                boolean survives = (mask >> l & 1) == 1;
                p *= survives ? 1 - broken : broken;
                if (survives) p2[root(p2, (int) links.get(l)[0])] = root(p2, (int) links.get(l)[1]);
            }
            boolean all = true;
            for (int i = 1; i < p2.length; i++) all &= root(p2, i) == root(p2, 0);
            if (all) total += p;
        }
        return total;
    }

    private static int root(int[] parent, int x) {
        while (parent[x] != x) x = parent[x];
        return x;
    }

    private static int find(Map<Integer, Integer> parent, int x) {
        while (parent.get(x) != x) x = parent.get(x);
        return x;
    }

    private EnchantForecast() {}
}
