package com.mamechin.enchantmod.scribing.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TemplateStoreTest {
    @Test
    void growsInStepsOfNineFromTheBase() {
        assertEquals(9, TemplateStore.slots(0, 0));
        assertEquals(9, TemplateStore.slots(9, 3));
        assertEquals(18, TemplateStore.slots(18, 0));
        assertEquals(27, TemplateStore.slots(20, 0));
    }

    @Test
    void neverDropsBelowTheSlotsInUse() {
        assertEquals(27, TemplateStore.slots(9, 19));
    }
}
