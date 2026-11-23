package com.mamechin.enchantmod.blocks.cauldron.action;

import java.util.Comparator;

import com.mamechin.enchantmod.blocks.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.blocks.cauldron.brewing.CauldronBrewingRecipe;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Shift + empty hand: start the best brew whose ingredients are stored, whose base fluid is in the cauldron and whose heat
 * requirement is met (ingredients are used up when it finishes).
 */
public class StartBrewAction implements CauldronAction {
    @Override
    public boolean useEmptyHand(CauldronBlockEntity c, Player player) {
        if (!player.isShiftKeyDown() || c.isBrewing() || c.getAmountMl() == 0) return false;
        // The client has no recipe data cache; it predicts success and the server decides.
        if (c.getLevel().isClientSide) return true;

        var options = c.satisfiedRecipes().stream().filter(h -> c.holdsBaseOf(h.value())).toList();
        if (options.isEmpty()) return false;
        int heat = c.heatTier();
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
