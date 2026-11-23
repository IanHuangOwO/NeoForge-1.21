package com.mamechin.enchantmod.items.handbook;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.registry.ModItems;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Left click with the handbook in hand opens its pages; a left click on a block must not also break it. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class HandbookClient {
    private HandbookClient() {}

    @SubscribeEvent
    static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getItemStack().is(ModItems.RESEARCH_HANDBOOK.get())) ResearchScreen.open();
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(ModItems.RESEARCH_HANDBOOK.get())) {
            event.setCanceled(true);
            ResearchScreen.open();
        }
    }
}
