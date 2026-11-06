package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronContent;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Empty bucket takes a full cauldron of plain water. */
public class TakeWaterBucketAction implements CauldronAction {
    @Override
    public boolean useItem(CalciteCauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.is(Items.BUCKET) || c.getContent() != CauldronContent.WATER
                || c.getAmountMl() != CalciteCauldronBlockEntity.CAPACITY_ML) return false;
        if (!c.getLevel().isClientSide) {
            CauldronActions.swapHeld(player, hand, stack, new ItemStack(Items.WATER_BUCKET));
            c.setAmountMl(0);
            CauldronActions.play(c, SoundEvents.BUCKET_FILL);
        }
        return true;
    }
}
