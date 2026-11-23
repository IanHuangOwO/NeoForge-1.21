package com.mamechin.enchantmod.blocks.cauldron.action;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.blocks.cauldron.CauldronContent;

import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

/** Water bottle adds BOTTLE_ML of water to plain water (never to ink). */
public class AddWaterBottleAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (!stack.is(Items.POTION) || contents == null || !contents.is(Potions.WATER)
                || c.getContent() != CauldronContent.WATER
                || c.getAmountMl() + CauldronBlockEntity.BOTTLE_ML > c.capacityMl()) return false;
        if (!c.getLevel().isClientSide) {
            CauldronActions.swapHeld(player, hand, stack, new ItemStack(Items.GLASS_BOTTLE));
            c.setAmountMl(c.getAmountMl() + CauldronBlockEntity.BOTTLE_ML);
            CauldronActions.play(c, SoundEvents.BOTTLE_EMPTY);
        }
        return true;
    }
}
