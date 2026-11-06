package com.mamechin.enchantmod.cauldron;

import net.minecraft.util.StringRepresentable;

/** What the cauldron currently holds; drives the liquid model. */
public enum CauldronContent implements StringRepresentable {
    WATER("water"), BREWING("brewing"), INK("ink");

    private final String name;

    CauldronContent(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
