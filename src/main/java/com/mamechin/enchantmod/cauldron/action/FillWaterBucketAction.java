package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Water bucket into an empty cauldron. */
public class FillWaterBucketAction implements CauldronAction {
    @Override
    public boolean useItem(CalciteCauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.is(Items.WATER_BUCKET) || c.getAmountMl() != 0) return false;
        if (!c.getLevel().isClientSide) {
            CauldronActions.swapHeld(player, hand, stack, new ItemStack(Items.BUCKET));
            c.setAmountMl(CalciteCauldronBlockEntity.CAPACITY_ML);
            CauldronActions.play(c, SoundEvents.BUCKET_EMPTY);
        }
        return true;
    }
}
