package com.mamechin.enchantmod.altar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AltarSpinTest {
    @Test
    void rampsUpHoldsAndRampsDown() {
        assertEquals(0, AltarSpin.speed(0));
        assertEquals(0.5F, AltarSpin.speed(20));
        assertEquals(1, AltarSpin.speed(40));
        assertEquals(1, AltarSpin.speed(AltarSpin.ENCHANT_AT));
        assertEquals(0.5F, AltarSpin.speed(180));
        assertEquals(0, AltarSpin.speed(200));
        assertEquals(0, AltarSpin.speed(-5));
    }
}
