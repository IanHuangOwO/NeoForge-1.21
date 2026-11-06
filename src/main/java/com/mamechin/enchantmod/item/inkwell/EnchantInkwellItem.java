package com.mamechin.enchantmod.item.inkwell;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Base for every inkwell tier. Behavior shared by all inkwells goes here. */
public abstract class EnchantInkwellItem extends Item {
    protected EnchantInkwellItem(Properties properties) {
        super(properties);
    }

    /** Spend one use of the given stack. Breaks it at 0 durability. */
    public void wear(ItemStack stack, LivingEntity user, EquipmentSlot slot) {
        stack.hurtAndBreak(1, user, slot);
    }
}
