package com.mamechin.enchantmod.scribing.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ResearchMapTest {
    @Test
    void everyPageHasANodeAndNoneOverlap() {
        Set<Integer> pages = new HashSet<>();
        for (ResearchMap.Tab tab : ResearchMap.tabs()) {
            List<ResearchMap.Node> nodes = tab.nodes();
            for (int a = 0; a < nodes.size(); a++) {
                pages.add(nodes.get(a).page());
                for (int b = a + 1; b < nodes.size(); b++) {
                    double d = Math.hypot(nodes.get(a).x() - nodes.get(b).x(), nodes.get(a).y() - nodes.get(b).y());
                    assertTrue(d >= 24, tab.key() + ": nodes " + a + " and " + b + " are " + d + " apart");
                }
            }
        }
        assertEquals(50, pages.size());
    }
}
