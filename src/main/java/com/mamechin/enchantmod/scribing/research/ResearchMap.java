package com.mamechin.enchantmod.scribing.research;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mamechin.enchantmod.scribing.rules.Definitions;
import com.mamechin.enchantmod.scribing.rules.Enchant;
import com.mamechin.enchantmod.scribing.rules.EnchantGroups;
import com.mamechin.enchantmod.scribing.rules.Rune;

/**
 * Where each knowledge node sits on the research handbook's maps, one map per side tab: a Runes tab, then one tab per mod
 * that adds enchants (so far only vanilla). On an enchant tab each enchant sits inside a ring of the runes it needs; the
 * all-purpose enchants are in the middle and enchants for one kind of tool are clustered around them. Pure and
 * deterministic, so the maps look the same every time. A node's page is its global index: runes first, then enchants in
 * table order, the same as {@link ResearchPanel}'s pages. A rune can have a node on several tabs, all opening the same page.
 */
public final class ResearchMap {
    public static final int MIN_GAP = 30;

    /** links: indices, within the same tab's node list, of the rune nodes an enchant needs (empty for a rune). */
    public record Node(int page, int x, int y, List<Integer> links) {}

    /** key: the namespace of the mod ("minecraft" for vanilla), or "runes"; used for the tab's name and icon. */
    public record Tab(String key, List<Node> nodes) {}

    private static List<Tab> cache;
    private static int cacheVersion = -1; // the version of the definitions the cache was built from

    private ResearchMap() {}

    public static synchronized List<Tab> tabs() {
        Definitions.Snapshot snapshot = Definitions.current();
        if (cache == null || cacheVersion != snapshot.version()) {
            cache = build(snapshot.runes(), snapshot.enchants());
            cacheVersion = snapshot.version();
        }
        return cache;
    }

    private static List<Tab> build(List<Rune> allRunes, List<Enchant> table) {
        int runeCount = allRunes.size();
        List<Tab> tabs = new ArrayList<>();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < runeCount; i++) all.add(i);
        tabs.add(new Tab("runes", layout(all, List.of(), table, allRunes)));
        Map<String, List<Integer>> byMod = new LinkedHashMap<>();
        for (int j = 0; j < table.size(); j++) byMod.computeIfAbsent(table.get(j).namespace(), k -> new ArrayList<>()).add(j);
        byMod.forEach((mod, cores) -> {
            List<Integer> runes = new ArrayList<>();
            for (int j : cores) for (String id : table.get(j).runes()) if (!runes.contains(allRunes.indexOf(Rune.byId(id)))) runes.add(allRunes.indexOf(Rune.byId(id)));
            tabs.add(new Tab(mod, layout(runes, cores, table, allRunes)));
        });
        return tabs;
    }

    /** Where an enchant starts: the middle if all-purpose, else in the direction of its tool group. */
    private static double[] target(Enchant enchant) {
        int k = EnchantGroups.CATEGORY_ORDER.indexOf(enchant.category()) - 1; // tools = 0 ... bows = 3, general = -1
        if (k < 0) return new double[] {0, 0};
        double angle = Math.PI / 4 + k * Math.PI / 2;
        return new double[] {Math.cos(angle) * 95, Math.sin(angle) * 95};
    }

    /** Rune nodes (fixed, on a ring) first, then the enchant nodes (pushed apart inside the ring). */
    private static List<Node> layout(List<Integer> runeList, List<Integer> cores, List<Enchant> table, List<Rune> allRunes) {
        int runeCount = allRunes.size();
        Random random = new Random(7);
        List<Integer> runes = new ArrayList<>(runeList);
        if (!cores.isEmpty()) {
            // order the runes round the ring by where the enchants that use them sit, so the lines stay short
            double[] angle = new double[runeCount];
            for (int r : runes) {
                double sx = 0, sy = 0;
                for (int j : cores) {
                    if (!table.get(j).runes().contains(allRunes.get(r).id())) continue;
                    double[] t = target(table.get(j));
                    sx += t[0];
                    sy += t[1];
                }
                angle[r] = Math.atan2(sy, sx);
            }
            runes.sort((a, b) -> Double.compare(angle[a], angle[b]));
        }
        int total = runes.size() + cores.size();
        double[] x = new double[total], y = new double[total];
        double ring = Math.max(cores.isEmpty() ? 125 : 200, 7 * runes.size());
        for (int i = 0; i < runes.size(); i++) {
            double a = 2 * Math.PI * i / runes.size();
            double radius = ring + random.nextInt(25) - 12;
            x[i] = Math.cos(a) * radius;
            y[i] = Math.sin(a) * radius;
        }
        List<List<Integer>> links = new ArrayList<>();
        for (int j = 0; j < cores.size(); j++) {
            List<Integer> mine = new ArrayList<>();
            for (String id : table.get(cores.get(j)).runes()) mine.add(runes.indexOf(allRunes.indexOf(Rune.byId(id))));
            links.add(mine);
            double[] t = target(table.get(cores.get(j)));
            x[runes.size() + j] = t[0] + random.nextInt(31) - 15;
            y[runes.size() + j] = t[1] + random.nextInt(31) - 15;
        }
        // push enchant nodes apart; the rune ring stays put
        for (int pass = 0; pass < 300; pass++) {
            for (int a = 0; a < total; a++) {
                for (int b = a + 1; b < total; b++) {
                    if (b < runes.size()) continue; // two fixed runes
                    double dx = x[b] - x[a], dy = y[b] - y[a], d = Math.hypot(dx, dy);
                    if (d >= MIN_GAP) continue;
                    if (d < 0.01) { dx = 1; dy = 0; d = 1; }
                    double push = (MIN_GAP - d) / 2 + 0.5;
                    boolean aFixed = a < runes.size();
                    x[b] += dx / d * (aFixed ? 2 * push : push);
                    y[b] += dy / d * (aFixed ? 2 * push : push);
                    if (!aFixed) {
                        x[a] -= dx / d * push;
                        y[a] -= dy / d * push;
                    }
                }
            }
        }
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int page = i < runes.size() ? runes.get(i) : runeCount + cores.get(i - runes.size());
            nodes.add(new Node(page, (int) Math.round(x[i]), (int) Math.round(y[i]), i < runes.size() ? List.of() : links.get(i - runes.size())));
        }
        return nodes;
    }
}
