package com.mamechin.enchantmod.altar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.mamechin.enchantmod.balance.Balance;

class AltarPowerTest {
    @Test
    void booksAreTakenFromTheFrontUntilTheCostIsPaid() {
        assertEquals(0, AltarPower.booksNeeded(new int[] {5, 3}, 0));
        assertEquals(1, AltarPower.booksNeeded(new int[] {5, 3}, 5));
        assertEquals(2, AltarPower.booksNeeded(new int[] {5, 3}, 6));
        assertEquals(2, AltarPower.booksNeeded(new int[] {5, 3}, 8));
        assertEquals(-1, AltarPower.booksNeeded(new int[] {5, 3}, 9));
        assertEquals(-1, AltarPower.booksNeeded(new int[] {}, 1));
    }

    @Test
    void cylinderIsRoundAndLeavesTheAltarOut() {
        List<int[]> cells = AltarPower.cylinder(new Balance.Area(4, 0, 2));
        for (int[] o : cells) assertTrue(o[0] * o[0] + o[2] * o[2] <= 16 && o[1] >= 0 && o[1] <= 2);
        assertEquals(49 * 3 - 1, cells.size()); // 49 columns within radius 4
        assertTrue(cells.stream().noneMatch(o -> o[0] == 0 && o[1] == 0 && o[2] == 0));
    }
}
