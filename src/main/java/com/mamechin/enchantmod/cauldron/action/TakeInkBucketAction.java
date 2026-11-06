package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronContent;
import com.mamechin.enchantmod.fluid.InkBucketItem;
import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Empty vanilla bucket on a full cauldron of ink. Handled here, before FluidContainerAction, because the
 * vanilla bucket wrapper would hand back a bucket without the ink tier component.
 */
public class TakeInkBucketAction implements CauldronAction {
    @Override
    public boolean useItem(CalciteCauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.is(Items.BUCKET) || c.getContent() != CauldronContent.INK
                || c.getAmountMl() < CalciteCauldronBlockEntity.CAPACITY_ML) return false;
        if (!c.getLevel().isClientSide) {
            CauldronActions.swapHeld(player, hand, stack, InkBucketItem.filled(ModItems.INK_BUCKET.get(), c.getInkwellId()));
            c.setAmountMl(0);
            CauldronActions.play(c, SoundEvents.BUCKET_FILL);
        }
        return true;
    }
}
