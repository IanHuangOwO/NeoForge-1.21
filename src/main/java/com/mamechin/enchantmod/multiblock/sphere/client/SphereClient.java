package com.mamechin.enchantmod.multiblock.sphere.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.registry.ModBlockEntities;
import com.mamechin.enchantmod.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The Armillary sphere's client wiring: its renderer, models, and tree slot screen. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class SphereClient {
    private SphereClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SPHERE_CORE.get(), SphereRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        SphereRenderer.MODELS.forEach(event::register);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SPHERE.get(), SphereScreen::new);
    }
}
