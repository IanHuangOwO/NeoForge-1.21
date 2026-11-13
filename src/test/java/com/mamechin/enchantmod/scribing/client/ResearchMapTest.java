package com.mamechin.enchantmod.scribing.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ResearchMapTest {
    @Test
    void fiftyNodesNoneOverlapping() {
        List<ResearchMap.Node> nodes = ResearchMap.nodes();
        assertEquals(50, nodes.size());
        for (int a = 0; a < nodes.size(); a++) {
            for (int b = a + 1; b < nodes.size(); b++) {
                double d = Math.hypot(nodes.get(a).x() - nodes.get(b).x(), nodes.get(a).y() - nodes.get(b).y());
                assertTrue(d >= 24, "nodes " + a + " and " + b + " are " + d + " apart");
            }
        }
    }
}
