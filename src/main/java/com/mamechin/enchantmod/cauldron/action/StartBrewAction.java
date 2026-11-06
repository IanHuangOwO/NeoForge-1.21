package com.mamechin.enchantmod.cauldron.action;

import java.util.Comparator;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronContent;
import com.mamechin.enchantmod.cauldron.brewing.CauldronBrewingRecipe;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Empty hand: start the best brew whose ingredients are stored and whose heat requirement is met
 * (ingredients are used up when it finishes).
 */
public class StartBrewAction implements CauldronAction {
    @Override
    public boolean useEmptyHand(CalciteCauldronBlockEntity c, Player player) {
        if (player.isShiftKeyDown() || c.getContent() != CauldronContent.WATER || c.getAmountMl() == 0) return false;
        // The client has no recipe data cache; it predicts success and the server decides.
        if (c.getLevel().isClientSide) return true;

        var options = c.satisfiedRecipes();
        if (options.isEmpty()) return false;
        int heat = CauldronHeat.tierBelow(c.getLevel(), c.getBlockPos());
        RecipeHolder<CauldronBrewingRecipe> best = options.stream()
                .filter(h -> heat >= h.value().minHeat())
                .max(Comparator.comparingInt(h -> h.value().minHeat()))
                .orElse(null);
        if (best == null) {
            player.displayClientMessage(Component.translatable("message.enchantmod.cauldron.no_heat"), true);
            return true;
        }
        c.startBrewing(best);
        CauldronActions.play(c, SoundEvents.BREWING_STAND_BREW);
        return true;
    }
}
