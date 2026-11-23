package com.mamechin.enchantmod.blocks.press.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.press.TemplateShelfBlock;
import com.mamechin.enchantmod.blocks.press.TemplateShelfBlockEntity;
import com.mamechin.enchantmod.registry.ModBlockEntities;
import com.mamechin.enchantmod.registry.ModMenus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** The printing press and template shelf client wiring: renderer, moving-part models, screen, and the shelf's template tooltip. */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class PressClient {
    private PressClient() {}

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.PRINTING_PRESS.get(), PrintingPressRenderer::new);
    }

    /** The moving parts of the printing press are not in any blockstate, so they are asked for by name. */
    @SubscribeEvent
    static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(PrintingPressRenderer.HEAD);
        event.register(PrintingPressRenderer.ROLLER);
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.PRINTING_PRESS.get(), PrintingPressScreen::new);
    }

    /** Pointing at a template on the shelf shows its tooltip beside the crosshair. */
    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui || mc.level == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        BlockState state = mc.level.getBlockState(hit.getBlockPos());
        if (!(state.getBlock() instanceof TemplateShelfBlock) || !(mc.level.getBlockEntity(hit.getBlockPos()) instanceof TemplateShelfBlockEntity shelf)) return;
        int slot = TemplateShelfBlock.slotAt(state, hit);
        if (slot < 0) return;
        ItemStack stack = shelf.inventory().getStackInSlot(slot);
        if (stack.isEmpty()) return;
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.renderTooltip(mc.font, stack, graphics.guiWidth() / 2 + 6, graphics.guiHeight() / 2 + 6);
    }
}
