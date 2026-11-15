package com.mamechin.enchantmod.items.inkwell;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Base for every inkwell tier. Behavior shared by all inkwells goes here. */
public abstract class EnchantInkwell extends Item {
    private final Supplier<? extends Fluid> inkFluid;

    protected EnchantInkwell(Properties properties, Supplier<? extends Fluid> inkFluid) {
        super(properties);
        this.inkFluid = inkFluid;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (stack.isDamageableItem()) {
            tooltip.add(Component.translatable("tooltip.enchantmod.ink", remaining(stack), stack.getMaxDamage()).withStyle(ChatFormatting.GRAY));
        }
    }

    /** The placeable ink fluid of this tier (what the cauldron holds when it makes this inkwell). */
    public Fluid getInkFluid() {
        return inkFluid.get();
    }

    /** Uses left in the stack; an item that cannot wear down never runs out. */
    public static int remaining(ItemStack stack) {
        return stack.isDamageableItem() ? stack.getMaxDamage() - stack.getDamageValue() : Integer.MAX_VALUE;
    }

    /** Spends ink for drawing: wears the stack down to 0 left but never breaks it. */
    public static void spend(ItemStack stack, int amount) {
        if (stack.isDamageableItem()) stack.setDamageValue(Math.min(stack.getMaxDamage(), stack.getDamageValue() + amount));
    }

    /** Spend one use of the given stack. Breaks it at 0 durability. */
    public void wear(ItemStack stack, LivingEntity user, EquipmentSlot slot) {
        stack.hurtAndBreak(1, user, slot);
    }
}
