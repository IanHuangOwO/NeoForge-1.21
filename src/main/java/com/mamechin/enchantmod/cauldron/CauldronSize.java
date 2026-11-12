package com.mamechin.enchantmod.cauldron;

/**
 * Everything that differs between the small and the large cauldron. Brewing logic is shared; only these numbers change.
 *
 * @param capacityMl  fluid storage
 * @param slots       ingredient slots
 * @param speed       brew speed multiplier (a recipe takes brewTicks / speed)
 * @param heatDepth   how many blocks below the cauldron its heat source sits
 * @param hoverHeight height above the block origin where stored ingredients float
 */
public record CauldronSize(int capacityMl, int slots, int speed, int heatDepth, float hoverHeight) {
    public static final CauldronSize SMALL = new CauldronSize(4000, 8, 1, 1, 1.5F);
    public static final CauldronSize LARGE = new CauldronSize(16000, 32, 2, 1, 2.5F);

    /** Liquid surface height for a LEVEL 0..3, in blocks above the core's base. The large bowl is the 3D model: floor, belly, neck. */
    public float surface(int level) {
        if (this == LARGE) return new float[] {-8, 0, 10, 26}[level] / 16.0F;
        return (level == 0 ? 4.0F : 6.0F + 3.0F * level) / 16.0F;
    }

    /** How far from the block centre bubbles may spawn, in blocks. */
    public double bubbleSpread() {
        return this == LARGE ? 0.8 : 0.3;
    }
}
