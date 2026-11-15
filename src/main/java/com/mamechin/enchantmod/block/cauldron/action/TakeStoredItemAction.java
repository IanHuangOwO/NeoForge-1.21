package com.mamechin.enchantmod.block.cauldron.action;

import com.mamechin.enchantmod.block.cauldron.CauldronBlockEntity;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Shift + empty hand: hand out the first stored stack (lowest slot first). */
public class TakeStoredItemAction implements CauldronAction {
    @Override
    public boolean useEmptyHand(CauldronBlockEntity c, Player player) {
        if (!player.isShiftKeyDown()) return false;
        for (int i = 0; i < c.inventory.getSlots(); i++) {
            ItemStack stored = c.inventory.getStackInSlot(i);
            if (!stored.isEmpty()) {
                if (!c.getLevel().isClientSide) {
                    player.getInventory().placeItemBackInInventory(c.inventory.extractItem(i, stored.getCount(), false));
                }
                return true;
            }
        }
        return false;
    }
}
