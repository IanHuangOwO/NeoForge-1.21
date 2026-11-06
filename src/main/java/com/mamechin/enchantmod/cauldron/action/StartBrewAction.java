package com.mamechin.enchantmod.cauldron.action;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronContent;
import com.mamechin.enchantmod.cauldron.brewing.InkBrewing;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;

/** Empty hand: start the brew when there is water, a full ingredient set and a heat source (ingredients are used up when it finishes). */
public class StartBrewAction implements CauldronAction {
    @Override
    public boolean useEmptyHand(CalciteCauldronBlockEntity c, Player player) {
        if (player.isShiftKeyDown() || c.getContent() != CauldronContent.WATER || c.getAmountMl() == 0) return false;
        InkBrewing.Recipe recipe = c.matchedRecipe();
        if (recipe == null) return false;
        if (CauldronHeat.tierBelow(c.getLevel(), c.getBlockPos()) < recipe.minHeat()) {
            if (!c.getLevel().isClientSide) player.displayClientMessage(Component.translatable("message.enchantmod.cauldron.no_heat"), true);
            return true;
        }
        if (!c.getLevel().isClientSide) {
            c.startBrewing(BuiltInRegistries.ITEM.getKey(recipe.inkwell().get()));
            CauldronActions.play(c, SoundEvents.BREWING_STAND_BREW);
        }
        return true;
    }
}
