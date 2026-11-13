package com.mamechin.enchantmod.scribing.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mamechin.enchantmod.scribing.EnchantForecast;
import com.mamechin.enchantmod.scribing.Rune;

/**
 * Where each knowledge node sits on the research tablet's maps, one map per side tab: the runes on one tab, then a tab each
 * for tools, weapons, armour and bows, where each enchant sits inside a ring of the runes it needs. Pure and deterministic,
 * so the maps look the same every time. A node's page is its global index: runes first, then enchants in table order, the
 * same as {@link ResearchPanel}'s pages. A rune can have a node on several tabs, all opening the same page.
 */
public final class ResearchMap {
    public static final int MIN_GAP = 30;

    /** links: indices, within the same tab's node list, of the rune nodes an enchant needs (empty for a rune). */
    public record Node(int page, int x, int y, List<Integer> links) {}

    /** key: the lang key suffix of the tab's name. */
    public record Tab(String key, List<Node> nodes) {}

    private static final String[][] GROUPS = {
        {"tools", "efficiency", "unbreaking", "fortune", "mending", "silk_touch"},
        {"weapons", "sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge"},
        {"armor", "protection", "fire_protection", "blast_protection", "feather_falling", "respiration", "thorns", "depth_strider", "frost_walker", "soul_speed", "swift_sneak"},
        {"bows", "power", "flame", "punch", "infinity"},
    };

    private static List<Tab> cache;

    private ResearchMap() {}

    public static synchronized List<Tab> tabs() {
        if (cache == null) cache = build();
        return cache;
    }

    private static List<Tab> build() {
        int runeCount = Rune.values().length;
        List<EnchantForecast.Core> table = EnchantForecast.table();
        List<Tab> tabs = new ArrayList<>();
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < runeCount; i++) all.add(i);
        tabs.add(new Tab("runes", layout(all, List.of(), table, runeCount)));
        for (String[] group : GROUPS) {
            List<Integer> cores = new ArrayList<>(), runes = new ArrayList<>();
            for (int g = 1; g < group.length; g++) {
                for (int j = 0; j < table.size(); j++) {
                    if (!table.get(j).enchant().equals(group[g])) continue;
                    cores.add(j);
                    for (String id : table.get(j).runes()) if (!runes.contains(Rune.byId(id).ordinal())) runes.add(Rune.byId(id).ordinal());
                }
            }
            tabs.add(new Tab(group[0], layout(runes, cores, table, runeCount)));
        }
        return tabs;
    }

    /** Rune nodes (fixed, on a ring) first, then the enchant nodes (pushed apart inside the ring). */
    private static List<Node> layout(List<Integer> runes, List<Integer> cores, List<EnchantForecast.Core> table, int runeCount) {
        Random random = new Random(7);
        int total = runes.size() + cores.size();
        double[] x = new double[total], y = new double[total];
        double ring = Math.max(125, 7 * runes.size());
        for (int i = 0; i < runes.size(); i++) {
            double angle = 2 * Math.PI * i / runes.size();
            double radius = ring + random.nextInt(25) - 12;
            x[i] = Math.cos(angle) * radius;
            y[i] = Math.sin(angle) * radius;
        }
        List<List<Integer>> links = new ArrayList<>();
        for (int j = 0; j < cores.size(); j++) {
            List<Integer> mine = new ArrayList<>();
            double cx = 0, cy = 0;
            for (String id : table.get(cores.get(j)).runes()) {
                int r = runes.indexOf(Rune.byId(id).ordinal());
                mine.add(r);
                cx += x[r];
                cy += y[r];
            }
            links.add(mine);
            int n = runes.size() + j;
            x[n] = cx / mine.size() * 0.5 + random.nextInt(31) - 15;
            y[n] = cy / mine.size() * 0.5 + random.nextInt(31) - 15;
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
