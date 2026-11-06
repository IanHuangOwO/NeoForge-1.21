package com.mamechin.enchantmod;


import com.mamechin.enchantmod.item.ModItems;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnchantMod.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENCHANTMOD_TAB = TABS.register("enchantmod_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.enchantmod"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> Items.LAPIS_LAZULI.getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.COPPER_QUILL.get());
                output.accept(ModItems.GOLDEN_QUILL.get());
                output.accept(ModItems.LAPIS_INKWELL.get());
                output.accept(ModItems.GLOWING_LAPIS_INKWELL.get());
                output.accept(ModItems.SCRIBING_TABLE.get());
                output.accept(ModItems.CALCITE_CAULDRON.get());
            }).build());

    private ModCreativeTabs() {}
}
