package com.mamechin.enchantmod;

import com.mamechin.enchantmod.registry.ModCreativeTabs;
import org.slf4j.Logger;

import com.mamechin.enchantmod.registry.ModBlocks;
import com.mamechin.enchantmod.blocks.cauldron.brewing.CauldronRecipes;
import com.mamechin.enchantmod.blocks.cauldron.brewing.ModCauldronRecipes;
import com.mamechin.enchantmod.blocks.cauldron.ModCapabilities;
import com.mamechin.enchantmod.registry.ModFluids;
import com.mamechin.enchantmod.registry.ModItems;
import com.mamechin.enchantmod.balance.BalanceLoader;
import com.mamechin.enchantmod.enchanting.DefinitionLoader;
import com.mamechin.enchantmod.registry.ModNetwork;
import com.mamechin.enchantmod.registry.ModDataComponents;
import com.mamechin.enchantmod.registry.ModMenus;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import com.mamechin.enchantmod.registry.ModBlockEntities;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(EnchantMod.MODID)
public class EnchantMod {
    public static final String MODID = "enchantmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnchantMod(IEventBus modEventBus, ModContainer modContainer) {
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        ModCauldronRecipes.TYPES.register(modEventBus);
        ModCauldronRecipes.SERIALIZERS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(CauldronRecipes::onDatapackSync);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        com.mamechin.enchantmod.items.books.BookEvents.register(modEventBus);
        com.mamechin.enchantmod.mobs.allay.AllayBinding.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        com.mamechin.enchantmod.registry.ModParticles.PARTICLE_TYPES.register(modEventBus);
        modEventBus.addListener(ModNetwork::register);
        NeoForge.EVENT_BUS.addListener(DefinitionLoader::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(BalanceLoader::onAddReloadListeners);
        NeoForge.EVENT_BUS.addListener(BalanceLoader::onDatapackSync);
        NeoForge.EVENT_BUS.addListener(DefinitionLoader::onDatapackSync);
        ModCreativeTabs.TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
