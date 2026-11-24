package com.mamechin.enchantmod.blocks.altar;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AltarSpinTest {
    @Test
    void higherLevelsTakeLongerAndHigherTiersFaster() {
        assertEquals(60, AltarSpin.chargeTicks(1, 60), "one book: 3 s");
        assertEquals(180, AltarSpin.chargeTicks(3, 60), "three books: 9 s");
        assertEquals(1, AltarSpin.chargeTicks(0, 60), "never zero ticks");
    }

    @Test
    void speedRisesHoldsAndFalls() {
        assertEquals(0.5F, AltarSpin.speed(AltarSpin.WARMUP, 30, 60));
        assertEquals(1, AltarSpin.speed(AltarSpin.WARMUP, 90, 60));
        assertEquals(1, AltarSpin.speed(AltarSpin.CHARGE, 5, 100));
        assertEquals(0.25F, AltarSpin.speed(AltarSpin.COOLDOWN, 30, 40));
        assertEquals(0, AltarSpin.speed(AltarSpin.IDLE, 10, 10));
    }
}
