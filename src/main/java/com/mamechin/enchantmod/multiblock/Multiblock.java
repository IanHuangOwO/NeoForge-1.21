package com.mamechin.enchantmod.multiblock;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** What the multiblocks (spinning wheel, Armillary sphere) share. */
public final class Multiblock {
    /** True on a core and its parts while the structure is formed. */
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private Multiblock() {}
}
