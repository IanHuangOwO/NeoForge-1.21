package com.mamechin.enchantmod.item.inkwell;

import java.util.function.Supplier;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/** Base for every inkwell tier. Behavior shared by all inkwells goes here. */
public abstract class EnchantInkwellItem extends Item {
    private final Supplier<? extends Fluid> inkFluid;

    protected EnchantInkwellItem(Properties properties, Supplier<? extends Fluid> inkFluid) {
        super(properties);
        this.inkFluid = inkFluid;
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
