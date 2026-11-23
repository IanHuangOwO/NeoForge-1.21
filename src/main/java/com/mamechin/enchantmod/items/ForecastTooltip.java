package com.mamechin.enchantmod.items;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.blocks.scribing.rules.CubeContents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/** The tooltip lines for enchants with the chance of each level, shared by the enchant template and the enchant cube. */
public final class ForecastTooltip {
    private static final int MAX_LEVEL_LINES = 8;

    private ForecastTooltip() {}

    public static void add(List<Component> tooltip, CubeContents contents) {
        if (contents.entries().isEmpty()) {
            tooltip.add(Component.translatable("tooltip.enchantmod.forecast.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (CubeContents.Entry entry : contents.entries()) {
            tooltip.add(Component.translatable("enchantment." + entry.enchantment().replace(':', '.')).withStyle(ChatFormatting.GOLD));
            // the likeliest levels, shown in level order
            List<CubeContents.Chance> shown = new ArrayList<>(entry.chances());
            shown.removeIf(c -> c.chance() < 0.005);
            shown.sort((x, y) -> Double.compare(y.chance(), x.chance()));
            if (shown.size() > MAX_LEVEL_LINES) shown = shown.subList(0, MAX_LEVEL_LINES);
            shown.sort((x, y) -> Integer.compare(x.level(), y.level()));
            for (CubeContents.Chance c : shown) {
                tooltip.add(Component.translatable("tooltip.enchantmod.forecast.level", c.level(), percent(c.chance())).withStyle(ChatFormatting.GRAY));
            }
            if (entry.failChance() >= 0.005) {
                tooltip.add(Component.translatable("tooltip.enchantmod.forecast.fails", percent(entry.failChance())).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static String percent(double chance) {
        return chance < 0.01 ? "<1%" : Math.round(chance * 100) + "%";
    }
}
