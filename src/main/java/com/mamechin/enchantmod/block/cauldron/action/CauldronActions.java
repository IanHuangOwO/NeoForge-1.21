package com.mamechin.enchantmod.block.cauldron.action;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.block.cauldron.CauldronBlockEntity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;

/** Ordered action list: the first action that applies wins. Addons may add to it during setup. */
public final class CauldronActions {
    public static final List<CauldronAction> ACTIONS = new ArrayList<>(List.of(
            new FluidContainerAction(),
            new AddWaterBottleAction(),
            new BottleInkAction(),
            new RefillInkwellAction(),
            new TakeStoredItemAction(),
            new StartBrewAction()));

    /** Replaces the held stack; single-item stacks (inkwells, bottles of water) are swapped outright. */
    static void swapHeld(Player player, InteractionHand hand, ItemStack held, ItemStack returned) {
        player.setItemInHand(hand, held.getMaxStackSize() == 1 ? returned : ItemUtils.createFilledResult(held, player, returned));
    }

    static void play(CauldronBlockEntity cauldron, SoundEvent sound) {
        cauldron.getLevel().playSound(null, cauldron.getBlockPos(), sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private CauldronActions() {}
}
