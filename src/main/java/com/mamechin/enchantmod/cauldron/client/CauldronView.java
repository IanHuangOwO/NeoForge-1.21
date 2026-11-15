package com.mamechin.enchantmod.cauldron.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Everything only the client needs about one cauldron: the smoothed liquid look, the brew animation clock and the stored items grouped per item type. */
public final class CauldronView {
    public final LiquidView liquid = new LiquidView();

    /** Game time the BREWING state was first seen, drives the items' dive animation; -1 when idle. */
    public float brewAnimStart = -1;

    private boolean itemsDirty = true;
    private final List<ItemStack> types = new ArrayList<>();
    private final List<Integer> counts = new ArrayList<>();

    /** The stored items changed, so the grouping is rebuilt on the next read. */
    public void markItemsDirty() {
        itemsDirty = true;
    }

    public List<ItemStack> types(IItemHandler inventory) {
        rebuild(inventory);
        return types;
    }

    public List<Integer> counts(IItemHandler inventory) {
        rebuild(inventory);
        return counts;
    }

    private void rebuild(IItemHandler inventory) {
        if (!itemsDirty) return;
        itemsDirty = false;
        types.clear();
        counts.clear();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            int index = -1;
            for (int j = 0; j < types.size(); j++) {
                if (ItemStack.isSameItemSameComponents(types.get(j), stack)) index = j;
            }
            if (index < 0) {
                types.add(stack.copyWithCount(1));
                counts.add(stack.getCount());
            } else {
                counts.set(index, counts.get(index) + stack.getCount());
            }
        }
    }
}
