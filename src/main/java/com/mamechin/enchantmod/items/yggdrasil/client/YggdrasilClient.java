package com.mamechin.enchantmod.items.yggdrasil.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.items.yggdrasil.YggdrasilItem;
import com.mamechin.enchantmod.registry.ModItems;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** The Yggdrasil's item model follows its stage ({@code enchantmod:stage} property). */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class YggdrasilClient {
    private YggdrasilClient() {}

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.YGGDRASIL.get(), ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "stage"),
                (stack, level, entity, seed) -> YggdrasilItem.stageIndex(stack)));
    }
}
