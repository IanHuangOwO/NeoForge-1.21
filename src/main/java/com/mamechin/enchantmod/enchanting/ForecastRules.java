package com.mamechin.enchantmod.enchanting;

/** How likely a line is to break at the altar: base + perCell per cell of its length, at most max. */
public record ForecastRules(double base, double perCell, double max) {
    public static final ForecastRules DEFAULT = new ForecastRules(0.04, 0.01, 0.95);
}
