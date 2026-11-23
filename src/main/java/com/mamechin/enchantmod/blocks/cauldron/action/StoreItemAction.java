package com.mamechin.enchantmod.blocks.cauldron.action;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.blocks.cauldron.brewing.CauldronRecipes;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** Right click with an ingredient: puts one of it into the cauldron (a brew needs each ingredient once per 1000 mB). */
public class StoreItemAction implements CauldronAction {
    @Override
    public boolean useItem(CauldronBlockEntity c, ItemStack stack, Player player, InteractionHand hand) {
        if (!CauldronRecipes.accepts(c.getLevel(), stack) || !c.hasRoom()) return false;
        if (!c.getLevel().isClientSide) {
            ItemStack one = stack.copyWithCount(1);
            if (!ItemHandlerHelper.insertItem(c.inventory, one, false).isEmpty()) return false;
            if (!player.hasInfiniteMaterials()) stack.shrink(1);
            CauldronActions.play(c, SoundEvents.BREWING_STAND_BREW);
        }
        return true;
    }
}
