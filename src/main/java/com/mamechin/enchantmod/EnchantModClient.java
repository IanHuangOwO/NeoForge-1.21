package com.mamechin.enchantmod;

import com.mamechin.enchantmod.blocks.ModBlockEntities;
import com.mamechin.enchantmod.altar.client.AltarRenderer;
import com.mamechin.enchantmod.wheel.client.WheelRenderer;
import com.mamechin.enchantmod.press.client.PrintingPressRenderer;
import com.mamechin.enchantmod.press.client.PrintingPressScreen;
import com.mamechin.enchantmod.cauldron.client.CauldronRenderer;
import com.mamechin.enchantmod.fluid.EnchantFluid;
import com.mamechin.enchantmod.scribing.ModMenus;
import com.mamechin.enchantmod.items.ModItems;
import com.mamechin.enchantmod.scribing.research.ResearchScreen;
import com.mamechin.enchantmod.scribing.client.ScribingTableScreen;

import com.mamechin.enchantmod.blocks.TemplateShelfBlock;
import com.mamechin.enchantmod.shelf.TemplateShelfBlockEntity;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = EnchantMod.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public class EnchantModClient {
    public EnchantModClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    // left click with the handbook in hand opens the pages too; a left click on a block must not also break it
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

    /** Holding a quill and pointing at an altar shows its tier and the enchant power it has linked, beside the crosshair. */
    @SubscribeEvent
    static void onRenderAltarInfo(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.options.hideGui || mc.level == null || mc.player == null || !(mc.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;
        if (!mc.player.getMainHandItem().is(AltarBlockEntity.QUILLS) && !mc.player.getOffhandItem().is(AltarBlockEntity.QUILLS)) return;
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

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SCRIBING_TABLE.get(), ScribingTableScreen::new);
        event.register(ModMenus.PRINTING_PRESS.get(), PrintingPressScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CAULDRON.get(), CauldronRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ALTAR.get(), AltarRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.PRINTING_PRESS.get(), PrintingPressRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.WHEEL_CORE.get(), WheelRenderer::new);
    }

    /** The moving parts of the printing press are not in any blockstate, so they are asked for by name. */
    @SubscribeEvent
    static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        event.register(PrintingPressRenderer.HEAD);
        event.register(PrintingPressRenderer.ROLLER);
        event.register(AltarRenderer.FLYER);
        event.register(AltarRenderer.FLYER_GLOW);
        event.register(AltarRenderer.BASE_GLOW);
        event.register(WheelRenderer.ROTOR);
        event.register(WheelRenderer.ROTOR_GLOW);
        event.register(WheelRenderer.PILLAR);
        event.register(WheelRenderer.PILLAR_GLOW);
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

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        EnchantMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        EnchantMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
