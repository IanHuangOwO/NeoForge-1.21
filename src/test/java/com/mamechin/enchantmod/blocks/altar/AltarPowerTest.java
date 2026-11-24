package com.mamechin.enchantmod.blocks.altar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        Balance.Area area = new Balance.Area(4, 0, 2);
        int count = 0;
        for (int dx = -6; dx <= 6; dx++)
            for (int dy = -2; dy <= 4; dy++)
                for (int dz = -6; dz <= 6; dz++)
                    if (AltarPower.inArea(area, dx, dy, dz)) {
                        assertTrue(dx * dx + dz * dz <= 16 && dy >= 0 && dy <= 2);
                        count++;
                    }
        assertEquals(49 * 3 - 1, count); // 49 columns within radius 4, the altar left out
        assertFalse(AltarPower.inArea(area, 0, 0, 0));
    }

    @Test
    void allotTakesOnlyWhatIsNeededInOrder() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {3, 2, 0}, AltarPower.allot(new int[] {3, 4, 5}, 5));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {0, 4, 1}, AltarPower.allot(new int[] {0, 4, 5}, 5));
        org.junit.jupiter.api.Assertions.assertNull(AltarPower.allot(new int[] {1, 2}, 5));
    }
}
