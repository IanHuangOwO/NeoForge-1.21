package com.mamechin.enchantmod.items.books;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BookLimitTest {
    @Test
    void trimKeepsLevelsInOrderUntilTheCap() {
        assertArrayEquals(new int[] {2}, BookLimit.trim(new int[] {5}, 2), "Sharpness V becomes II");
        assertArrayEquals(new int[] {1, 1, 0}, BookLimit.trim(new int[] {1, 1, 1}, 2), "the third enchant is dropped");
        assertArrayEquals(new int[] {3, 1}, BookLimit.trim(new int[] {3, 2}, 4));
        assertArrayEquals(new int[] {5, 3}, BookLimit.trim(new int[] {5, 3}, 16), "under the cap: unchanged");
        assertArrayEquals(new int[] {5, 3}, BookLimit.trim(new int[] {5, 3}, -1), "limit off");
    }

    @Test
    void anvilLevelFollowsVanilla() {
        assertEquals(2, BookLimit.anvilLevel(1, 1, 5));
        assertEquals(3, BookLimit.anvilLevel(1, 3, 5));
        assertEquals(5, BookLimit.anvilLevel(5, 5, 5), "capped at the maximum");
        assertEquals(1, BookLimit.anvilLevel(0, 1, 1), "new enchant");
    }
}
