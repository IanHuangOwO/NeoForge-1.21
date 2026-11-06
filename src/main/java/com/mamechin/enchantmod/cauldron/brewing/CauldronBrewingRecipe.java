package com.mamechin.enchantmod.cauldron.brewing;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * data/&lt;ns&gt;/recipe/*.json of type enchantmod:cauldron_brewing. Each listed ingredient needs one stored item and
 * the cauldron must hold base_fluid; on completion the contents become result_fluid. min_heat is compared with the
 * heat tier under the cauldron.
 */
public record CauldronBrewingRecipe(List<Ingredient> ingredients, ResourceLocation baseFluid, ResourceLocation resultFluid, int brewTicks, int minHeat)
        implements Recipe<CauldronInput> {

    @Override
    public boolean matches(CauldronInput input, Level level) {
        int[] left = new int[input.size()];
        for (int i = 0; i < left.length; i++) left[i] = input.getItem(i).getCount();
        for (Ingredient ingredient : ingredients) {
            boolean found = false;
            for (int i = 0; i < left.length && !found; i++) {
                if (left[i] > 0 && ingredient.test(input.getItem(i))) {
                    left[i]--;
                    found = true;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    /** Takes one item per ingredient out of the inventory. */
    public void consume(IItemHandler inv) {
        for (Ingredient ingredient : ingredients) {
            for (int i = 0; i < inv.getSlots(); i++) {
                if (!inv.getStackInSlot(i).isEmpty() && ingredient.test(inv.getStackInSlot(i))) {
                    inv.extractItem(i, 1, false);
                    break;
                }
            }
        }
    }

    @Override
    public ItemStack assemble(CauldronInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.copyOf(ingredients);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModCauldronRecipes.BREWING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModCauldronRecipes.BREWING_TYPE.get();
    }

    public static class Serializer implements RecipeSerializer<CauldronBrewingRecipe> {
        private static final MapCodec<CauldronBrewingRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").forGetter(CauldronBrewingRecipe::ingredients),
                ResourceLocation.CODEC.optionalFieldOf("base_fluid", ResourceLocation.withDefaultNamespace("water")).forGetter(CauldronBrewingRecipe::baseFluid),
                ResourceLocation.CODEC.fieldOf("result_fluid").forGetter(CauldronBrewingRecipe::resultFluid),
                Codec.INT.optionalFieldOf("brew_ticks", 100).forGetter(CauldronBrewingRecipe::brewTicks),
                Codec.INT.optionalFieldOf("min_heat", 1).forGetter(CauldronBrewingRecipe::minHeat)
        ).apply(i, CauldronBrewingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, CauldronBrewingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), CauldronBrewingRecipe::ingredients,
                ResourceLocation.STREAM_CODEC, CauldronBrewingRecipe::baseFluid,
                ResourceLocation.STREAM_CODEC, CauldronBrewingRecipe::resultFluid,
                ByteBufCodecs.VAR_INT, CauldronBrewingRecipe::brewTicks,
                ByteBufCodecs.VAR_INT, CauldronBrewingRecipe::minHeat,
                CauldronBrewingRecipe::new);

        @Override
        public MapCodec<CauldronBrewingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, CauldronBrewingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
