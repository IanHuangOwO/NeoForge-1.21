package com.mamechin.enchantmod.registry;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The mod's item tags (data/enchantmod/tags/item/). */
public final class ModTags {
    /** Quills: they start and scan the altar, toggle the build guide, and go in the scribing table's quill slot. */
    public static final TagKey<Item> QUILLS = item("quills");
    public static final TagKey<Item> INKWELLS = item("inkwells");

    private ModTags() {}

    private static TagKey<Item> item(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, name));
    }
}
