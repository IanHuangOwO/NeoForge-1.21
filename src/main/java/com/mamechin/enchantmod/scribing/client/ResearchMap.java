package com.mamechin.enchantmod.scribing.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.mamechin.enchantmod.scribing.EnchantForecast;
import com.mamechin.enchantmod.scribing.Rune;

/**
 * Where each knowledge node sits on the research tablet's map: the 24 runes on an outer ring, each enchant inside, near
 * the runes it needs. Pure and deterministic, so the map looks the same every time. A node's page is its index here:
 * runes first, then enchants, the same order as {@link ResearchPanel}'s pages.
 */
public final class ResearchMap {
    public static final int RING = 170, MIN_GAP = 30;

    /** page: the page it opens; runeNodes: the rune nodes an enchant links to (empty for a rune). */
    public record Node(int page, int x, int y, List<Integer> links) {}

    private static List<Node> cache;

    private ResearchMap() {}

    public static synchronized List<Node> nodes() {
        if (cache == null) cache = build();
        return cache;
    }

    private static List<Node> build() {
        Random random = new Random(7);
        Rune[] runes = Rune.values();
        List<EnchantForecast.Core> cores = EnchantForecast.table();
        int total = runes.length + cores.size();
        double[] x = new double[total], y = new double[total];

        for (int i = 0; i < runes.length; i++) {
            double angle = 2 * Math.PI * i / runes.length;
            double radius = RING + random.nextInt(25) - 12;
            x[i] = Math.cos(angle) * radius;
            y[i] = Math.sin(angle) * radius;
        }
        List<List<Integer>> links = new ArrayList<>();
        for (int j = 0; j < cores.size(); j++) {
            List<Integer> mine = new ArrayList<>();
            double cx = 0, cy = 0;
            for (String id : cores.get(j).runes()) {
                int r = Rune.byId(id).ordinal();
                mine.add(r);
                cx += x[r];
                cy += y[r];
            }
            links.add(mine);
            int n = runes.length + j;
            x[n] = cx / mine.size() * 0.5 + random.nextInt(31) - 15;
            y[n] = cy / mine.size() * 0.5 + random.nextInt(31) - 15;
        }
        // push enchant nodes apart; the rune ring stays put
        for (int pass = 0; pass < 200; pass++) {
            for (int a = 0; a < total; a++) {
                for (int b = a + 1; b < total; b++) {
                    if (b < runes.length) continue; // two fixed runes
                    double dx = x[b] - x[a], dy = y[b] - y[a], d = Math.hypot(dx, dy);
                    if (d >= MIN_GAP) continue;
                    if (d < 0.01) { dx = 1; dy = 0; d = 1; }
                    double push = (MIN_GAP - d) / 2 + 0.5;
                    boolean aFixed = a < runes.length;
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
            nodes.add(new Node(i, (int) Math.round(x[i]), (int) Math.round(y[i]), i < runes.length ? List.of() : links.get(i - runes.length)));
        }
        return nodes;
    }
}
