package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CauldronTier0BlockEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * One click behavior of the calcite cauldron. Both methods return true when the action applies (the click is
 * consumed); mutate state only on the server, but still return true on the client so the click is predicted.
 */
public interface CauldronAction {
    default boolean useItem(CauldronTier0BlockEntity cauldron, ItemStack stack, Player player, InteractionHand hand) {
        return false;
    }

    default boolean useEmptyHand(CauldronTier0BlockEntity cauldron, Player player) {
        return false;
    }
}
