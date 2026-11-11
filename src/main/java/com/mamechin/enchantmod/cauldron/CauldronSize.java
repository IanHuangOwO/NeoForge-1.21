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
}
