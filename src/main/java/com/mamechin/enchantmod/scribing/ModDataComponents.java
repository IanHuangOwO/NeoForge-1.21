package com.mamechin.enchantmod.scribing;

import java.util.List;

import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.SavedTemplate;
import net.minecraft.network.codec.ByteBufCodecs;
import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, EnchantMod.MODID);

    /** The layout drawn on an enchant template. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Blueprint>> BLUEPRINT =
            COMPONENTS.registerComponentType("blueprint", b -> b.persistent(Blueprint.CODEC).networkSynchronized(Blueprint.STREAM_CODEC));

    /** The templates kept in a research handbook. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<SavedTemplate>>> SAVED_TEMPLATES =
            COMPONENTS.registerComponentType("saved_templates", b -> b.persistent(SavedTemplate.CODEC.listOf())
                    .networkSynchronized(SavedTemplate.STREAM_CODEC.apply(ByteBufCodecs.list(Library.MAX))));

    private ModDataComponents() {}
}
