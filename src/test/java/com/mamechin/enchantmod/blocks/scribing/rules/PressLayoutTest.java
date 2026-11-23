package com.mamechin.enchantmod.blocks.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class PressLayoutTest {
    private static List<Integer> open(int capacity) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < PressLayout.RING; i++) if (PressLayout.isOpen(i, capacity)) list.add(i);
        return list;
    }

    @Test
    void tiersOpenTheTopThenATriangleThenAll() {
        assertEquals(List.of(), open(0));
        assertEquals(List.of(0), open(1));
        assertEquals(List.of(0, 2, 4), open(3));
        assertEquals(List.of(0, 1, 2, 3, 4, 5), open(6));
    }

    @Test
    void anUnevenCapacityFillsFromTheTop() {
        assertEquals(List.of(0, 1, 2, 3), open(4));
    }
}
