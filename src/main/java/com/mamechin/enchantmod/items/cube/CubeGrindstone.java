package com.mamechin.enchantmod.items.cube;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.scribing.ModDataComponents;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.GrindstoneEvent;

/** A pressed enchant cube alone in the grindstone comes out as a blank cube of the same tier, for no experience. */
@EventBusSubscriber(modid = EnchantMod.MODID)
public final class CubeGrindstone {
    @SubscribeEvent
    static void onPlace(GrindstoneEvent.OnPlaceItem event) {
        ItemStack top = event.getTopItem(), bottom = event.getBottomItem();
        ItemStack cube = bottom.isEmpty() ? top : top.isEmpty() ? bottom : ItemStack.EMPTY;
        if (cube.isEmpty() || !EnchantCube.isPressed(cube)) return;
        ItemStack blank = cube.copy();
        blank.remove(ModDataComponents.CUBE_CONTENTS.get());
        event.setOutput(blank);
        event.setXp(0);
    }

    private CubeGrindstone() {}
}
