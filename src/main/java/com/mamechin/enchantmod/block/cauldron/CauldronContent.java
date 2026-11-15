package com.mamechin.enchantmod.block.cauldron;

import net.minecraft.util.StringRepresentable;

/** What the cauldron looks like it holds; drives the liquid model. */
public enum CauldronContent implements StringRepresentable {
    WATER("water"), ENCHANTING("enchanting"), BREWING("brewing"), INK("ink");

    private final String name;

    CauldronContent(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
