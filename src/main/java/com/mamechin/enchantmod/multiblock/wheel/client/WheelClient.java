package com.mamechin.enchantmod.multiblock.wheel.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.registry.ModBlockEntities;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;

/** The spinning wheel's client wiring: its renderer and extra models. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class WheelClient {
    private WheelClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.WHEEL_CORE.get(), WheelRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(WheelRenderer.ROTOR);
        event.register(WheelRenderer.ROTOR_GLOW);
        event.register(WheelRenderer.PILLAR);
        event.register(WheelRenderer.PILLAR_GLOW);
    }
}
