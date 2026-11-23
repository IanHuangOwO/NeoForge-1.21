package com.mamechin.enchantmod.items.template;

import java.util.List;

import com.mamechin.enchantmod.registry.ModDataComponents;
import com.mamechin.enchantmod.enchanting.Blueprint;
import com.mamechin.enchantmod.enchanting.CubeContents;
import com.mamechin.enchantmod.enchanting.EnchantForecast;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** The enchant template: its tooltip lists the enchants the drawn blueprint makes and the chance of each level. */
public class EnchantTemplate extends Item {
    public EnchantTemplate(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        Blueprint blueprint = stack.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY);
        ForecastTooltip.add(tooltip, CubeContents.combine(EnchantForecast.forecast(blueprint)));
    }
}
