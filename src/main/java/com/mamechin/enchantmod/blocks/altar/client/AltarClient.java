package com.mamechin.enchantmod.blocks.altar.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.altar.AltarBlockEntity;
import com.mamechin.enchantmod.registry.ModBlockEntities;
import com.mamechin.enchantmod.registry.ModParticles;
import com.mamechin.enchantmod.registry.ModTags;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** The altar's client wiring: its renderer and extra models, the drain glyph particle, and the tier readout beside the crosshair. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class AltarClient {
    private AltarClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.ALTAR.get(), AltarRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(AltarRenderer.FLYER);
        event.register(AltarRenderer.FLYER_GLOW);
        event.register(AltarRenderer.BASE_GLOW);
    }

    @SubscribeEvent
    static void onRegisterParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DRAIN_GLYPH.get(), DrainGlyphParticle.Provider::new);
    }

    /** Holding a quill and pointing at an altar shows its tier and the enchant power it has linked, beside the crosshair. */
    @SubscribeEvent
    static void onRenderAltarInfo(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui || mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (!mc.player.getMainHandItem().is(ModTags.QUILLS) && !mc.player.getOffhandItem().is(ModTags.QUILLS)) return;
        if (!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof AltarBlockEntity altar)) return;
        GuiGraphics graphics = event.getGuiGraphics();
        int x = graphics.guiWidth() / 2 + 8, y = graphics.guiHeight() / 2 + 6;
        if (altar.tier() < 0) {
            graphics.drawString(mc.font, Component.translatable("gui.enchantmod.altar.not_scanned"), x, y, 0xFFE0C070);
            return;
        }
        graphics.drawString(mc.font, Component.translatable("gui.enchantmod.altar.tier", altar.tier()), x, y, 0xFFE0C070);
        graphics.drawString(mc.font, Component.translatable("gui.enchantmod.altar.power", altar.power()), x, y + 10, 0xFFE0C070);
    }
}
