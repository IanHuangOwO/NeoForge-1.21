package com.mamechin.enchantmod.item;

import com.mamechin.enchantmod.block.scribing.Blueprint;
import com.mamechin.enchantmod.block.scribing.EnchantForecast;
import com.mamechin.enchantmod.block.scribing.ModDataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The enchant template: its tooltip lists the enchants the drawn blueprint makes and the chance of each level. */
public class EnchantTemplate extends Item {
    private static final int MAX_LEVEL_LINES = 8;

    public EnchantTemplate(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Blueprint blueprint = stack.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY);
        List<EnchantForecast.Result> results = EnchantForecast.forecast(blueprint);
        if (results.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.enchantmod.forecast.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (EnchantForecast.Result result : results) {
            tooltip.add(Component.translatable("enchantment.minecraft." + result.enchant()).withStyle(ChatFormatting.GOLD));
            // the likeliest levels, shown in level order
            List<Map.Entry<Integer, Double>> shown = new ArrayList<>(result.levels().entrySet());
            shown.removeIf(e -> e.getValue() < 0.005);
            shown.sort((x, y) -> Double.compare(y.getValue(), x.getValue()));
            if (shown.size() > MAX_LEVEL_LINES) shown = shown.subList(0, MAX_LEVEL_LINES);
            shown.sort(Map.Entry.comparingByKey());
            for (Map.Entry<Integer, Double> e : shown) {
                tooltip.add(Component.translatable("tooltip.enchantmod.forecast.level", e.getKey(), percent(e.getValue())).withStyle(ChatFormatting.GRAY));
            }
            if (result.broken() >= 0.005) {
                tooltip.add(Component.translatable("tooltip.enchantmod.forecast.fails", percent(result.broken())).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static String percent(double chance) {
        return chance < 0.01 ? "<1%" : Math.round(chance * 100) + "%";
    }
}
