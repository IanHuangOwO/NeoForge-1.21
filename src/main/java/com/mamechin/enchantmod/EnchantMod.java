package com.mamechin.enchantmod;

import org.slf4j.Logger;

import com.mamechin.enchantmod.block.ModBlocks;
import com.mamechin.enchantmod.item.ModItems;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(EnchantMod.MODID)
public class EnchantMod {
    public static final String MODID = "enchantmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EnchantMod(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
}
