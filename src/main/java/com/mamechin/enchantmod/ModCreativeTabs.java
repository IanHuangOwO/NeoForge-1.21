package com.mamechin.enchantmod;


import com.mamechin.enchantmod.fluid.ModFluids;
import com.mamechin.enchantmod.items.ModItems;

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
            .icon(() -> ModItems.RESEARCH_HANDBOOK.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ModItems.QUILL_TIER0.get());
                output.accept(ModItems.QUILL_TIER1.get());
                output.accept(ModItems.QUILL_TIER2.get());
                output.accept(ModItems.INKWELL_TIER0.get());
                output.accept(ModItems.INKWELL_TIER1.get());
                output.accept(ModItems.INKWELL_TIER2.get());
                output.accept(ModFluids.LIQUID_OF_ENCHANTING.bucket().get());
                output.accept(ModFluids.INK_TIER0.bucket().get());
                output.accept(ModFluids.INK_TIER1.bucket().get());
                output.accept(ModFluids.INK_TIER2.bucket().get());
                output.accept(ModItems.SCRIBING_TABLE.get());
                output.accept(ModItems.ENCHANT_TEMPLATE_TIER0.get());
                output.accept(ModItems.ENCHANT_TEMPLATE_TIER1.get());
                output.accept(ModItems.ENCHANT_TEMPLATE_TIER2.get());
                output.accept(ModItems.RESEARCH_HANDBOOK.get());
                output.accept(ModItems.SMALL_CAULDRON.get());
                output.accept(ModItems.ALTAR.get());
            }).build());

    private ModCreativeTabs() {}
}
