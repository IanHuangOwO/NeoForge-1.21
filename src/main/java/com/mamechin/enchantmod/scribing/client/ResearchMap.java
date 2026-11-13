package com.mamechin.enchantmod.scribing.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.mamechin.enchantmod.scribing.EnchantForecast;
import com.mamechin.enchantmod.scribing.Rune;

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

    /** Enchants for one kind of tool, each group clustered in its own direction; any enchant not listed is all-purpose. */
    private static final String[][] CLUSTERS = {
        {"efficiency", "fortune", "silk_touch"},
        {"sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge"},
        {"protection", "fire_protection", "blast_protection", "feather_falling", "respiration", "thorns", "depth_strider", "frost_walker", "soul_speed", "swift_sneak"},
        {"power", "flame", "punch", "infinity"},
    };

    private static final String[] CATEGORIES = {"tools", "weapons", "armor", "bows"}; // same order as CLUSTERS

    private static List<Tab> cache;

    private ResearchMap() {}

    public static synchronized List<Tab> tabs() {
        if (cache == null) cache = build();
        return cache;
    }

    /** Vanilla enchant ids are bare; another mod's would be "namespace:id". */
    private static String namespace(String enchant) {
        int colon = enchant.indexOf(':');
        return colon < 0 ? "minecraft" : enchant.substring(0, colon);
    }

    private static List<Tab> build() {
        int runeCount = Rune.values().length;
        List<EnchantForecast.Core> table = EnchantForecast.table();
        List<Tab> tabs = new ArrayList<>();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < runeCount; i++) all.add(i);
        tabs.add(new Tab("runes", layout(all, List.of(), table, runeCount)));
        Map<String, List<Integer>> byMod = new LinkedHashMap<>();
        for (int j = 0; j < table.size(); j++) byMod.computeIfAbsent(namespace(table.get(j).enchant()), k -> new ArrayList<>()).add(j);
        byMod.forEach((mod, cores) -> {
            List<Integer> runes = new ArrayList<>();
            for (int j : cores) for (String id : table.get(j).runes()) if (!runes.contains(Rune.byId(id).ordinal())) runes.add(Rune.byId(id).ordinal());
            tabs.add(new Tab(mod, layout(runes, cores, table, runeCount)));
        });
        return tabs;
    }

    /** The kind of tool an enchant is for: tools, weapons, armor or bows, else "general" (all-purpose, or a mod's enchant with no grouping set up). */
    public static String category(String enchant) {
        for (int k = 0; k < CLUSTERS.length; k++) {
            for (String id : CLUSTERS[k]) if (id.equals(enchant)) return CATEGORIES[k];
        }
        return "general";
    }

    /** Every category in display order. */
    public static final List<String> CATEGORY_ORDER = List.of("general", "tools", "weapons", "armor", "bows");

    /** Where an enchant starts: the middle if all-purpose, else in the direction of its tool group. */
    private static double[] target(String enchant) {
        for (int k = 0; k < CLUSTERS.length; k++) {
            for (String id : CLUSTERS[k]) {
                if (id.equals(enchant)) {
                    double angle = Math.PI / 4 + k * Math.PI / 2;
                    return new double[] {Math.cos(angle) * 95, Math.sin(angle) * 95};
                }
            }
        }
        return new double[] {0, 0};
    }

    /** Rune nodes (fixed, on a ring) first, then the enchant nodes (pushed apart inside the ring). */
    private static List<Node> layout(List<Integer> runeList, List<Integer> cores, List<EnchantForecast.Core> table, int runeCount) {
        Random random = new Random(7);
        List<Integer> runes = new ArrayList<>(runeList);
        if (!cores.isEmpty()) {
            // order the runes round the ring by where the enchants that use them sit, so the lines stay short
            double[] angle = new double[runeCount];
            for (int r : runes) {
                double sx = 0, sy = 0;
                for (int j : cores) {
                    if (!table.get(j).runes().contains(Rune.values()[r].id())) continue;
                    double[] t = target(table.get(j).enchant());
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
            for (String id : table.get(cores.get(j)).runes()) mine.add(runes.indexOf(Rune.byId(id).ordinal()));
            links.add(mine);
            double[] t = target(table.get(cores.get(j)).enchant());
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
