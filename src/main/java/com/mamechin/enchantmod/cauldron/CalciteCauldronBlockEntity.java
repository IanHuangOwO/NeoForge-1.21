package com.mamechin.enchantmod.cauldron;

import java.util.List;
import java.util.Optional;

import com.mamechin.enchantmod.cauldron.brewing.CauldronBrewingRecipe;
import com.mamechin.enchantmod.cauldron.brewing.CauldronInput;
import com.mamechin.enchantmod.cauldron.brewing.CauldronRecipes;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import com.mamechin.enchantmod.fluid.InkTiers;

/**
 * Holds the exact liquid amount in ml plus its kind and the 8-slot ingredient inventory.
 * The blockstate (LEVEL, CONTENT) is only a coarse copy for models and comparators.
 */
public class CalciteCauldronBlockEntity extends BlockEntity {
    public static final int CAPACITY_ML = 1000;
    public static final int BOTTLE_ML = 250;
    public static final int INK_ML_PER_USE = 10;
    public static final int CHECK_TICKS = 20;

    public final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getLevel() == null || CauldronRecipes.accepts(getLevel(), stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            inventoryDirty = true;
            setChanged();
        }
    };

    // Derived from the inventory; recomputed lazily, only after it changed.
    private boolean inventoryDirty = true;
    private int cachedGeneration = -1;
    private List<RecipeHolder<CauldronBrewingRecipe>> satisfied = List.of();
    private RecipeHolder<CauldronBrewingRecipe> activeRecipe; // resolved from activeRecipeId
    private ResourceLocation activeRecipeId;
    private boolean hasRoom = true;

    private int amountMl;
    private CauldronContent content = CauldronContent.WATER;
    private ResourceLocation inkFluidId; // the ink fluid once content == INK (or the brew result while BREWING)
    private int progress;

    /** Exposes the cauldron as a 1000 mB tank: water, or enchanting ink of one tier; never mixed, locked while brewing. */
    public final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return contents();
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY_ML;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return stack.is(Fluids.WATER) || InkTiers.inkwellFor(stack.getFluid()) != null;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int room = CAPACITY_ML - amountMl;
            if (resource.isEmpty() || content == CauldronContent.BREWING || room <= 0) return 0;
            int accepted = Math.min(room, resource.getAmount());
            if (resource.is(Fluids.WATER)) {
                if (content != CauldronContent.WATER) return 0;
                if (action.execute()) setAmountMl(amountMl + accepted);
                return accepted;
            }
            if (InkTiers.inkwellFor(resource.getFluid()) != null) {
                ResourceLocation id = BuiltInRegistries.FLUID.getKey(resource.getFluid());
                if (amountMl > 0 && !(content == CauldronContent.INK && id.equals(inkFluidId))) return 0;
                if (action.execute()) {
                    content = CauldronContent.INK;
                    inkFluidId = id;
                    setAmountMl(amountMl + accepted);
                }
                return accepted;
            }
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.isSameFluidSameComponents(resource, contents()) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (amountMl == 0 || content == CauldronContent.BREWING || maxDrain <= 0) return FluidStack.EMPTY;
            int drained = Math.min(maxDrain, amountMl);
            FluidStack out = contents().copyWithAmount(drained);
            if (action.execute()) setAmountMl(amountMl - drained);
            return out;
        }
    };

    private FluidStack contents() {
        if (amountMl == 0) return FluidStack.EMPTY;
        if (content == CauldronContent.INK && inkFluidId != null) return new FluidStack(getInkFluid(), amountMl);
        return new FluidStack(Fluids.WATER, amountMl);
    }

    public CalciteCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALCITE_CAULDRON.get(), pos, state);
    }

    public int getAmountMl() {
        return amountMl;
    }

    private void refresh() {
        if (level == null || level.isClientSide) return;
        if (!inventoryDirty && cachedGeneration == CauldronRecipes.generation()) return;
        CauldronInput input = CauldronInput.of(inventory);
        satisfied = CauldronRecipes.all(level).stream().filter(h -> h.value().matches(input, level)).toList();
        hasRoom = false;
        for (int i = 0; i < inventory.getSlots() && !hasRoom; i++) {
            ItemStack s = inventory.getStackInSlot(i);
            hasRoom = s.isEmpty() || s.getCount() < s.getMaxStackSize();
        }
        if (cachedGeneration != CauldronRecipes.generation()) activeRecipe = null;
        cachedGeneration = CauldronRecipes.generation();
        inventoryDirty = false;
    }

    /** Recipes whose ingredients are all stored right now (server only). */
    public List<RecipeHolder<CauldronBrewingRecipe>> satisfiedRecipes() {
        refresh();
        return satisfied;
    }

    /** False when every slot is full, so thrown items can be ignored without touching the inventory. */
    public boolean hasRoom() {
        refresh();
        return hasRoom;
    }

    public CauldronContent getContent() {
        return content;
    }

    /** The ink fluid held (or being brewed); null when none. */
    public Fluid getInkFluid() {
        return inkFluidId == null ? null : BuiltInRegistries.FLUID.get(inkFluidId);
    }

    public void setAmountMl(int ml) {
        amountMl = Mth.clamp(ml, 0, CAPACITY_ML);
        if (amountMl == 0) {
            content = CauldronContent.WATER;
            inkFluidId = null;
            progress = 0;
        }
        syncState();
    }

    public void startBrewing(RecipeHolder<CauldronBrewingRecipe> recipe) {
        content = CauldronContent.BREWING;
        activeRecipe = recipe;
        activeRecipeId = recipe.id();
        inkFluidId = BuiltInRegistries.FLUID.getKey(InkTiers.fluidFor(recipe.value().result().getItem()));
        progress = 0;
        syncState();
    }

    @SuppressWarnings("unchecked")
    private RecipeHolder<CauldronBrewingRecipe> activeRecipe() {
        if (activeRecipe == null && activeRecipeId != null) {
            Optional<RecipeHolder<?>> found = level.getRecipeManager().byKey(activeRecipeId);
            if (found.isPresent() && found.get().value() instanceof CauldronBrewingRecipe) {
                activeRecipe = (RecipeHolder<CauldronBrewingRecipe>) found.get();
            }
        }
        return activeRecipe;
    }

    private void cancelBrew() {
        content = CauldronContent.WATER;
        inkFluidId = null;
        activeRecipe = null;
        activeRecipeId = null;
        progress = 0;
        syncState();
    }

    /** Every 20 ticks (and at the end) the brew re-checks heat and ingredients; any miss cancels it back to plain water. */
    public void serverTick() {
        if (content != CauldronContent.BREWING) return;
        RecipeHolder<CauldronBrewingRecipe> holder = activeRecipe();
        if (holder == null) {
            cancelBrew();
            return;
        }
        CauldronBrewingRecipe recipe = holder.value();
        progress++;
        if (progress % CHECK_TICKS != 0 && progress < recipe.brewTicks()) return;

        refresh();
        if (!satisfied.contains(holder) || CauldronHeat.tierBelow(level, worldPosition) < recipe.minHeat()) {
            cancelBrew();
        } else if (progress >= recipe.brewTicks()) {
            recipe.consume(inventory);
            content = CauldronContent.INK;
            activeRecipe = null;
            activeRecipeId = null;
            progress = 0;
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            syncState();
        }
    }

    private void syncState() {
        setChanged();
        if (level != null) {
            int lvl = (amountMl * 3 + CAPACITY_ML - 1) / CAPACITY_ML; // ceil: 1-333 ml = 1, up to 1000 ml = 3
            level.setBlock(worldPosition, getBlockState()
                    .setValue(CalciteCauldronBlock.LEVEL, lvl)
                    .setValue(CalciteCauldronBlock.CONTENT, content), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("AmountMl", amountMl);
        tag.putString("Content", content.getSerializedName());
        tag.putInt("Progress", progress);
        if (inkFluidId != null) tag.putString("InkFluid", inkFluidId.toString());
        if (activeRecipeId != null) tag.putString("Recipe", activeRecipeId.toString());
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        amountMl = tag.getInt("AmountMl");
        content = CauldronContent.valueOf(tag.getString("Content").toUpperCase());
        progress = tag.getInt("Progress");
        inkFluidId = tag.contains("InkFluid") ? ResourceLocation.parse(tag.getString("InkFluid")) : null;
        activeRecipeId = tag.contains("Recipe") ? ResourceLocation.parse(tag.getString("Recipe")) : null;
        activeRecipe = null;
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        inventoryDirty = true;
    }
}
