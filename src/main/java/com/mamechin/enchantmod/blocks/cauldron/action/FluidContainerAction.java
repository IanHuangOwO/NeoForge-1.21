package com.mamechin.enchantmod.blocks.cauldron.action;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Any fluid container (vanilla, modded or ink bucket, tank item) fills or drains the cauldron's tank. */
public class FluidContainerAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        // The client does not track the tank contents; it only predicts that a fluid container was clicked.
        if (c.getLevel().isClientSide) return stack.getCapability(Capabilities.FluidHandler.ITEM) != null;
        return FluidUtil.interactWithFluidHandler(player, hand, c.fluidHandler);
    }
}
