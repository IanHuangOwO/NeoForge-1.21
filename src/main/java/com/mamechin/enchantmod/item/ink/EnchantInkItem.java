package com.mamechin.enchantmod.item.ink;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Base for every ink tier. Behavior shared by all inks goes here. */
public abstract class EnchantInkItem extends Item {
    protected EnchantInkItem(Properties properties) {
        super(properties);
    }

    /** Spend one use of the given stack. Breaks it at 0 durability. */
    public void wear(ItemStack stack, LivingEntity user, EquipmentSlot slot) {
        stack.hurtAndBreak(1, user, slot);
    }
}
