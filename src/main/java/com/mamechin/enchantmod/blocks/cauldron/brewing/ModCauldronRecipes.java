package com.mamechin.enchantmod.blocks.cauldron.brewing;

import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCauldronRecipes {
    public static final DeferredRegister<RecipeType<?>> TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, EnchantMod.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, EnchantMod.MODID);

    public static final Supplier<RecipeType<CauldronBrewingRecipe>> BREWING_TYPE = TYPES.register("cauldron_brewing",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "cauldron_brewing")));
    public static final Supplier<RecipeSerializer<CauldronBrewingRecipe>> BREWING_SERIALIZER = SERIALIZERS.register("cauldron_brewing",
            CauldronBrewingRecipe.Serializer::new);

    private ModCauldronRecipes() {}
}
