package com.mamechin.enchantmod.blocks.scribing.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The scribing table's client wiring: its screen. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class ScribingClient {
    private ScribingClient() {}

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SCRIBING_TABLE.get(), ScribingTableScreen::new);
    }
}
