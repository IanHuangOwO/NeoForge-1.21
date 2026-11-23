package com.mamechin.enchantmod.blocks.cauldron.action;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.blocks.cauldron.CauldronContent;
import com.mamechin.enchantmod.items.inkwell.EnchantInkwell;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A damaged inkwell of the cauldron's own tier is refilled from the stored ink. */
public class RefillInkwellAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!(stack.getItem() instanceof EnchantInkwell inkwell) || !stack.isDamaged()
                || c.getContent() != CauldronContent.INK
                || c.getAmountMl() < CauldronBlockEntity.INK_ML_PER_USE
                || inkwell.getInkFluid() != c.getFluid()) return false;
        if (!c.getLevel().isClientSide) {
            int uses = Math.min(stack.getDamageValue(), c.getAmountMl() / CauldronBlockEntity.INK_ML_PER_USE);
            ItemStack refilled = stack.copy();
            refilled.setDamageValue(stack.getDamageValue() - uses);
            CauldronActions.swapHeld(player, hand, stack, refilled);
            c.setAmountMl(c.getAmountMl() - uses * CauldronBlockEntity.INK_ML_PER_USE);
            CauldronActions.play(c, SoundEvents.BOTTLE_FILL);
        }
        return true;
    }
}
