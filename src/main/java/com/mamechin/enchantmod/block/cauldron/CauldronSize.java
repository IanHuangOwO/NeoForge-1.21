package com.mamechin.enchantmod.block.cauldron;

/**
 * The numbers that differ between cauldron sizes (only the small cauldron exists for now).
 *
 * @param capacityMl  fluid storage
 * @param slots       ingredient slots
 * @param speed       brew speed multiplier (a recipe takes brewTicks / speed)
 * @param heatDepth   how many blocks below the cauldron its heat source sits
 * @param hoverHeight height above the block origin where stored ingredients float
 */
public record CauldronSize(int capacityMl, int slots, int speed, int heatDepth, float hoverHeight) {
    public static final CauldronSize SMALL = new CauldronSize(4000, 8, 1, 1, 1.5F);

    /** Liquid surface height for a LEVEL 0..3, in blocks above the core's base. */
    public float surface(int level) {
        return (level == 0 ? 4.0F : 6.0F + 3.0F * level) / 16.0F;
    }

    /** How far from the block centre bubbles may spawn, in blocks. */
    public double bubbleSpread() {
        return 0.3;
    }
}
