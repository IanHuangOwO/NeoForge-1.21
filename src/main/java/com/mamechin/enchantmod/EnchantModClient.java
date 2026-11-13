package com.mamechin.enchantmod;

import com.mamechin.enchantmod.cauldron.ModBlockEntities;
import com.mamechin.enchantmod.cauldron.client.CauldronRenderer;
import com.mamechin.enchantmod.fluid.EnchantFluid;
import com.mamechin.enchantmod.scribing.ModMenus;
import com.mamechin.enchantmod.item.ModItems;
import com.mamechin.enchantmod.scribing.client.ResearchScreen;
import com.mamechin.enchantmod.scribing.client.ScribingTableScreen;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
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

    // left click with the tablet in hand opens the pages too; a left click on a block must not also break it
    @SubscribeEvent
    static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (event.getItemStack().is(ModItems.RESEARCH_TABLET.get())) ResearchScreen.open();
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide && event.getItemStack().is(ModItems.RESEARCH_TABLET.get())) {
            event.setCanceled(true);
            ResearchScreen.open();
        }
    }

    @SubscribeEvent
    static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.SCRIBING_TABLE.get(), ScribingTableScreen::new);
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SMALL_CAULDRON.get(), CauldronRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.LARGE_CAULDRON.get(), CauldronRenderer::new);
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
