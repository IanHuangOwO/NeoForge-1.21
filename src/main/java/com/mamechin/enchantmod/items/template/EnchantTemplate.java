package com.mamechin.enchantmod.items.template;

import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.Definitions;
import com.mamechin.enchantmod.scribing.rules.EnchantForecast;
import com.mamechin.enchantmod.scribing.rules.TemplateLimits;
import com.mamechin.enchantmod.scribing.ModDataComponents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The enchant template: its tooltip lists the enchants the drawn blueprint makes and the chance of each level. */
/** Base for every enchant template tier. What a tier allows is in its JSON file, see Definitions.template. */
public abstract class EnchantTemplate extends Item {
    private static final int MAX_LEVEL_LINES = 8;

    protected EnchantTemplate(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** What this template allows, from its data file; the whole grid with no cap for anything else. */
    public static TemplateLimits tierOf(ItemStack stack) {
        return stack.isEmpty() ? TemplateLimits.FULL : Definitions.template(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        TemplateLimits tier = tierOf(stack);
        tooltip.add(Component.translatable("tooltip.enchantmod.template.area", tier.width(), tier.height()).withStyle(ChatFormatting.DARK_GRAY));
        if (tier.maxLevel() != Integer.MAX_VALUE) tooltip.add(Component.translatable("tooltip.enchantmod.template.max_level", tier.maxLevel()).withStyle(ChatFormatting.DARK_GRAY));
        Blueprint blueprint = stack.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY);
        List<EnchantForecast.Result> results = EnchantForecast.forecast(blueprint, tierOf(stack).maxLevel());
        if (results.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.enchantmod.forecast.none").withStyle(ChatFormatting.GRAY));
            return;
        }
        for (EnchantForecast.Result result : results) {
            tooltip.add(Component.translatable(result.enchant().nameKey()).withStyle(ChatFormatting.GOLD));
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
