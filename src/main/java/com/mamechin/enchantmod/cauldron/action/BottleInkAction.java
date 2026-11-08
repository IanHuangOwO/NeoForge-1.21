package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CauldronTier0BlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronContent;
import com.mamechin.enchantmod.fluid.InkTiers;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Glass bottle on finished ink becomes that tier's inkwell, filled with as many uses as the ink allows. */
public class BottleInkAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronTier0BlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.is(Items.GLASS_BOTTLE) || c.getContent() != CauldronContent.INK
                || c.getAmountMl() < CauldronTier0BlockEntity.INK_ML_PER_USE) return false;
        if (!c.getLevel().isClientSide) {
            ItemStack inkwell = new ItemStack(InkTiers.inkwellFor(c.getFluid()));
            int uses = Math.min(inkwell.getMaxDamage(), c.getAmountMl() / CauldronTier0BlockEntity.INK_ML_PER_USE);
            inkwell.setDamageValue(inkwell.getMaxDamage() - uses);
            CauldronActions.swapHeld(player, hand, stack, inkwell);
            c.setAmountMl(c.getAmountMl() - uses * CauldronTier0BlockEntity.INK_ML_PER_USE);
            CauldronActions.play(c, SoundEvents.BOTTLE_FILL);
        }
        return true;
    }
}
