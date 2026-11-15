package com.mamechin.enchantmod.block.cauldron.brewing;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

/** Server-side recipe lookups plus a cached set of every item any recipe accepts. */
public final class CauldronRecipes {
    private static volatile int generation;
    private static Set<Item> accepted = Set.of();
    private static int acceptedGeneration = -1;

    /** Changes whenever recipes may have been reloaded; block entities compare it to know their caches are stale. */
    public static int generation() {
        return generation;
    }

    public static void onDatapackSync(OnDatapackSyncEvent event) {
        generation++;
    }

    public static List<RecipeHolder<CauldronBrewingRecipe>> all(Level level) {
        return level.getRecipeManager().getAllRecipesFor(ModCauldronRecipes.BREWING_TYPE.get());
    }

    /** True when some recipe lists this item as an ingredient (O(1) after the set is built). */
    public static boolean accepts(Level level, ItemStack stack) {
        if (acceptedGeneration != generation) {
            Set<Item> items = new HashSet<>();
            for (RecipeHolder<CauldronBrewingRecipe> holder : all(level)) {
                for (Ingredient ingredient : holder.value().ingredients()) {
                    for (ItemStack s : ingredient.getItems()) items.add(s.getItem());
                }
            }
            accepted = items;
            acceptedGeneration = generation;
        }
        return accepted.contains(stack.getItem());
    }

    private CauldronRecipes() {}
}
