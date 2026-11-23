package com.mamechin.enchantmod.registry;

import com.mamechin.enchantmod.blocks.scribing.rules.Blueprint;
import com.mamechin.enchantmod.blocks.scribing.rules.CubeContents;
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

    /** The enchants pressed into an enchant cube, with the chance of each level. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CubeContents>> CUBE_CONTENTS =
            COMPONENTS.registerComponentType("cube_contents", b -> b.persistent(CubeContents.CODEC).networkSynchronized(CubeContents.STREAM_CODEC));

    /** A bound Yggdrasil's owner, age and growth. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<com.mamechin.enchantmod.items.yggdrasil.TreeData>> TREE =
            COMPONENTS.registerComponentType("tree", b -> b.persistent(com.mamechin.enchantmod.items.yggdrasil.TreeData.CODEC).networkSynchronized(com.mamechin.enchantmod.items.yggdrasil.TreeData.STREAM_CODEC));

    private ModDataComponents() {}
}
