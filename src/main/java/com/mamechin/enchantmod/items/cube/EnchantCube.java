package com.mamechin.enchantmod.items.cube;

import java.util.List;

import com.mamechin.enchantmod.items.ForecastTooltip;
import com.mamechin.enchantmod.scribing.ModDataComponents;
import com.mamechin.enchantmod.scribing.rules.CubeContents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Base for every enchant cube tier. A cube holds the enchants pressed into it (CubeContents); the tier sets how many templates the printing press may press into it. */
public abstract class EnchantCube extends Item {
    private final int capacity, maxLevel;

    protected EnchantCube(Properties properties, int capacity, int maxLevel) {
        super(properties.stacksTo(1));
        this.capacity = capacity;
        this.maxLevel = maxLevel;
    }

    /** The highest level of one enchantment the cube can carry; pressing caps the sum of the templates' levels at this. */
    public int maxLevel() {
        return maxLevel;
    }

    /** How many templates can be pressed into this cube at once. */
    public int capacity() {
        return capacity;
    }

    /** True once enchants are pressed into the cube. */
    public static boolean isPressed(ItemStack stack) {
        return !stack.getOrDefault(ModDataComponents.CUBE_CONTENTS.get(), CubeContents.EMPTY).entries().isEmpty();
    }

    /** A pressed cube may go into the grindstone, which wipes it (CubeGrindstone). */
    @Override
    public boolean canGrindstoneRepair(ItemStack stack) {
        return isPressed(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.enchantmod.cube.capacity", capacity).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.enchantmod.cube.max_level", maxLevel).withStyle(ChatFormatting.DARK_GRAY));
        ForecastTooltip.add(tooltip, stack.getOrDefault(ModDataComponents.CUBE_CONTENTS.get(), CubeContents.EMPTY));
    }
}
