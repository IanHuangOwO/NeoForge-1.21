package com.mamechin.enchantmod.blocks.altar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AltarSpinTest {
    @Test
    void shippedDefaultsAreThreeSecondsUpOneSecondPerLevelTwoDown() {
        int duration = AltarSpin.duration(5);
        assertEquals(60 + 100 + 40, duration);
        assertEquals(160, AltarSpin.enchantAt(duration));
        assertEquals(80, AltarSpin.drainEnd(1));
        assertEquals(0, AltarSpin.speed(0, duration));
        assertEquals(0.5F, AltarSpin.speed(30, duration));
        assertEquals(1, AltarSpin.speed(60, duration));
        assertEquals(1, AltarSpin.speed(160, duration));
        assertEquals(0.5F, AltarSpin.speed(180, duration));
        assertEquals(0, AltarSpin.speed(duration, duration));
        assertEquals(0, AltarSpin.speed(-5, duration));
    }
}
