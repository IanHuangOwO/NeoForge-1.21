package com.mamechin.enchantmod.blocks.cauldron.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.cauldron.EnchantFluid;
import com.mamechin.enchantmod.registry.ModBlockEntities;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** The cauldron's client wiring: its renderer and the look of the enchant fluids. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class CauldronClient {
    private CauldronClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CAULDRON.get(), CauldronRenderer::new);
    }

    /** Ink fluids reuse the vanilla water textures, tinted per tier. */
    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        for (EnchantFluid ink : EnchantFluid.ALL) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                @Override
                public ResourceLocation getStillTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_still");
                }

                @Override
                public ResourceLocation getFlowingTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_flow");
                }

                @Override
                public ResourceLocation getOverlayTexture() {
                    return ResourceLocation.withDefaultNamespace("block/water_overlay");
                }

                @Override
                public int getTintColor() {
                    return ink.tint();
                }
            }, ink.type().get());
        }
    }
}
