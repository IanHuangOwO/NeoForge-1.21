package com.mamechin.enchantmod;

import org.slf4j.Logger;

import com.mamechin.enchantmod.blocks.ModBlocks;
import com.mamechin.enchantmod.cauldron.brewing.CauldronRecipes;
import com.mamechin.enchantmod.cauldron.brewing.ModCauldronRecipes;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;
import com.mamechin.enchantmod.fluid.ModCapabilities;
import com.mamechin.enchantmod.fluid.ModFluids;
import com.mamechin.enchantmod.items.ModItems;
import com.mamechin.enchantmod.scribing.EditBlueprintPayload;
import com.mamechin.enchantmod.scribing.ModDataComponents;
import com.mamechin.enchantmod.scribing.ModMenus;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import com.mamechin.enchantmod.blocks.ModBlockEntities;
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
        modEventBus.addListener(CauldronHeat::register);
        ModFluids.FLUID_TYPES.register(modEventBus);
        ModFluids.FLUIDS.register(modEventBus);
        modEventBus.addListener(ModCapabilities::register);
        ModCauldronRecipes.TYPES.register(modEventBus);
        ModCauldronRecipes.SERIALIZERS.register(modEventBus);
        NeoForge.EVENT_BUS.addListener(CauldronRecipes::onDatapackSync);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModDataComponents.COMPONENTS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        modEventBus.addListener(EditBlueprintPayload::register);
        ModCreativeTabs.TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
