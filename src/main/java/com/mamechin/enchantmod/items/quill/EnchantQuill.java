package com.mamechin.enchantmod.items.quill;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Base for every quill tier. Behavior shared by all quills goes here. */
public abstract class EnchantQuill extends Item {
    protected EnchantQuill(Properties properties) {
        super(properties);
    }

    /** Spend one use of the given stack. Breaks it at 0 durability. */
    public void wear(ItemStack stack, LivingEntity user, EquipmentSlot slot) {
        stack.hurtAndBreak(1, user, slot);
    }
}
