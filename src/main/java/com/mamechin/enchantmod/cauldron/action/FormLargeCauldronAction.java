package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronSize;
import com.mamechin.enchantmod.cauldron.LargeCauldronStructure;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** A quill used on a small cauldron that sits inside a complete smooth basalt body and legs turns the structure into a large cauldron. */
public class FormLargeCauldronAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (c.size() != CauldronSize.SMALL || c.isBrewing() || !stack.is(LargeCauldronStructure.FORMING_ITEMS)) return false;
        if (c.getLevel().isClientSide) return true;
        BlockPos bad = LargeCauldronStructure.firstMismatch(c.getLevel(), c.getBlockPos());
        if (bad != null) {
            player.displayClientMessage(Component.translatable("message.enchantmod.cauldron.bad_structure", bad.getX(), bad.getY(), bad.getZ()), true);
            return true;
        }
        LargeCauldronStructure.form(c.getLevel(), c.getBlockPos());
        CauldronActions.play(c, SoundEvents.BEACON_ACTIVATE);
        return true;
    }
}
