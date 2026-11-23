package com.mamechin.enchantmod.blocks.cauldron.action;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.blocks.cauldron.CauldronContent;
import com.mamechin.enchantmod.items.inkwell.InkTiers;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Glass bottle on finished ink becomes that tier's inkwell, filled with as many uses as the ink allows. */
public class BottleInkAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!stack.is(Items.GLASS_BOTTLE) || c.getContent() != CauldronContent.INK
                || c.getAmountMl() < CauldronBlockEntity.INK_ML_PER_USE) return false;
        if (!c.getLevel().isClientSide) {
            ItemStack inkwell = new ItemStack(InkTiers.inkwellFor(c.getFluid()));
            int uses = Math.min(inkwell.getMaxDamage(), c.getAmountMl() / CauldronBlockEntity.INK_ML_PER_USE);
            inkwell.setDamageValue(inkwell.getMaxDamage() - uses);
            CauldronActions.swapHeld(player, hand, stack, inkwell);
            c.setAmountMl(c.getAmountMl() - uses * CauldronBlockEntity.INK_ML_PER_USE);
            CauldronActions.play(c, SoundEvents.BOTTLE_FILL);
        }
        return true;
    }
}
