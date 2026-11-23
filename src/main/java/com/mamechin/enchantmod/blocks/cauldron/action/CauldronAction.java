package com.mamechin.enchantmod.blocks.cauldron.action;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One click behavior of the cauldron. Both methods return true when the action applies (the click is
 * consumed); mutate state only on the server, but still return true on the client so the click is predicted.
 */
public interface CauldronAction {
    default boolean useItem(CauldronBlockEntity cauldron, ItemStack stack, Player player, InteractionHand hand) {
        return false;
    }

    default boolean useEmptyHand(CauldronBlockEntity cauldron, Player player) {
        return false;
    }
}
