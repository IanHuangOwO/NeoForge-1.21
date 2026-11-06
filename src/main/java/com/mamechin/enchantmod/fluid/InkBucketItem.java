package com.mamechin.enchantmod.fluid;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

/** Bucket of enchanting ink; the tier lives in the stored FluidStack, see ModDataComponents.FLUID_CONTENT. */
public class InkBucketItem extends Item {
    public InkBucketItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static ItemStack filled(net.minecraft.world.item.Item bucket, ResourceLocation tier) {
        ItemStack stack = new ItemStack(bucket);
        stack.set(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.copyOf(ModFluids.inkStack(tier, 1000)));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        FluidStack fluid = stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), SimpleFluidContent.EMPTY).copy();
        ResourceLocation tier = fluid.isEmpty() ? null : fluid.get(ModDataComponents.INK_TIER.get());
        Component tierName = tier == null ? Component.empty() : Component.translatable(Util.makeDescriptionId("ink_tier", tier));
        return Component.translatable("item.enchantmod.ink_bucket", tierName);
    }
}
