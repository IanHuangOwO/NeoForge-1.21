package com.mamechin.enchantmod.items.yggdrasil;

/** A player's experience in points (pure, no Minecraft), with vanilla's level curve. */
public final class Xp {
    private Xp() {}

    /** Points from {@code level} to the next level (vanilla {@code Player.getXpNeededForNextLevel}). */
    public static int needed(int level) {
        return level >= 30 ? 112 + (level - 30) * 9 : level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    /** All the points a player at {@code level} with {@code progress} (0 to 1) towards the next level holds. */
    public static int total(int level, float progress) {
        int points = Math.round(progress * needed(level));
        for (int l = 0; l < level; l++) points += needed(l);
        return points;
    }
}
