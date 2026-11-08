package com.mamechin.enchantmod.cauldron;

import java.util.List;
import java.util.Optional;

import com.mamechin.enchantmod.cauldron.brewing.CauldronBrewingRecipe;
import com.mamechin.enchantmod.cauldron.brewing.CauldronInput;
import com.mamechin.enchantmod.cauldron.brewing.CauldronRecipes;
import com.mamechin.enchantmod.cauldron.heat.CauldronHeat;
import com.mamechin.enchantmod.fluid.InkTiers;
import com.mamechin.enchantmod.fluid.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Holds one fluid (water, liquid of enchanting or an ink tier) in exact ml, the 8-slot ingredient inventory and the
 * brew progress. The blockstate (LEVEL, CONTENT) is only a coarse copy for models and comparators.
 */
public class CalciteCauldronBlockEntity extends BlockEntity {
    public static final int CAPACITY_ML = 1000;
    public static final int BOTTLE_ML = 250;
    public static final int INK_ML_PER_USE = 10;
    public static final int CHECK_TICKS = 20;
    private static final int BURST_COUNT = 24;

    private static final ResourceLocation WATER = BuiltInRegistries.FLUID.getKey(Fluids.WATER);

    public final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return getLevel() == null || CauldronRecipes.accepts(getLevel(), stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            inventoryDirty = true;
            setChanged();
            // the client renders the stored items, so it needs the new inventory
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    // Derived from the inventory; recomputed lazily, only after it changed.
    private boolean inventoryDirty = true;
    private int cachedGeneration = -1;
    private List<RecipeHolder<CauldronBrewingRecipe>> satisfied = List.of();
    private boolean hasRoom = true;

    /** Client-only: smoothed liquid appearance, see LiquidView. */
    public final com.mamechin.enchantmod.cauldron.client.LiquidView liquidView = new com.mamechin.enchantmod.cauldron.client.LiquidView();

    /** Client-only: game time the BREWING state was first seen, drives the items' dive animation; -1 when idle. */
    public float brewAnimStart = -1;

    private int amountMl;
    private ResourceLocation fluidId = WATER; // what is stored; unchanged while brewing
    private boolean brewing;
    private RecipeHolder<CauldronBrewingRecipe> activeRecipe; // resolved from activeRecipeId
    private ResourceLocation activeRecipeId;
    private int progress;

    /** Exposes the cauldron as a 1000 mB tank of one fluid at a time; locked while brewing. */
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
            return isAccepted(stack.getFluid());
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int room = CAPACITY_ML - amountMl;
            if (resource.isEmpty() || brewing || room <= 0 || !isAccepted(resource.getFluid())) return 0;
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(resource.getFluid());
            if (amountMl > 0 && !id.equals(fluidId)) return 0; // never mix fluids
            int accepted = Math.min(room, resource.getAmount());
            if (action.execute()) {
                fluidId = id;
                setAmountMl(amountMl + accepted);
            }
            return accepted;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.isSameFluidSameComponents(resource, contents()) ? drain(resource.getAmount(), action) : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (amountMl == 0 || brewing || maxDrain <= 0) return FluidStack.EMPTY;
            int drained = Math.min(maxDrain, amountMl);
            FluidStack out = contents().copyWithAmount(drained);
            if (action.execute()) setAmountMl(amountMl - drained);
            return out;
        }
    };

    /** Fluids the cauldron takes: water, liquid of enchanting and every ink tier. */
    private static boolean isAccepted(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == ModFluids.LIQUID_OF_ENCHANTING.source().get() || InkTiers.inkwellFor(fluid) != null;
    }

    private FluidStack contents() {
        return amountMl == 0 ? FluidStack.EMPTY : new FluidStack(getFluid(), amountMl);
    }

    public CalciteCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALCITE_CAULDRON.get(), pos, state);
    }

    public int getAmountMl() {
        return amountMl;
    }

    public Fluid getFluid() {
        return BuiltInRegistries.FLUID.get(fluidId);
    }

    public ResourceLocation getActiveRecipeId() {
        return activeRecipeId;
    }

    /** Brew progress in ticks as of the last sync (the client uses it to place its animation clock). */
    public int getProgress() {
        return progress;
    }

    public boolean isBrewing() {
        return brewing;
    }

    /** The visual category of the stored fluid, for the blockstate. */
    public CauldronContent getContent() {
        if (brewing) return CauldronContent.BREWING;
        Fluid fluid = getFluid();
        if (fluid == Fluids.WATER) return CauldronContent.WATER;
        if (fluid == ModFluids.LIQUID_OF_ENCHANTING.source().get()) return CauldronContent.ENCHANTING;
        return CauldronContent.INK;
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

    /** Recipes whose ingredients are all stored right now, whatever the fluid (server only). */
    public List<RecipeHolder<CauldronBrewingRecipe>> satisfiedRecipes() {
        refresh();
        return satisfied;
    }

    /** False when every slot is full, so thrown items can be ignored without touching the inventory. */
    public boolean hasRoom() {
        refresh();
        return hasRoom;
    }

    /** True when the recipe's base fluid is what the cauldron holds. */
    public boolean holdsBaseOf(CauldronBrewingRecipe recipe) {
        return amountMl > 0 && fluidId.equals(recipe.baseFluid());
    }

    public void setAmountMl(int ml) {
        amountMl = Mth.clamp(ml, 0, CAPACITY_ML);
        if (amountMl == 0) {
            fluidId = WATER;
            progress = 0;
        }
        syncState();
    }

    public void startBrewing(RecipeHolder<CauldronBrewingRecipe> recipe) {
        brewing = true;
        activeRecipe = recipe;
        activeRecipeId = recipe.id();
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

    private void stopBrewing() {
        brewing = false;
        activeRecipe = null;
        activeRecipeId = null;
        progress = 0;
    }

    /** Every 20 ticks (and at the end) the brew re-checks heat and ingredients; any miss cancels it, keeping the base fluid. */
    public void serverTick() {
        if (!brewing) return;
        RecipeHolder<CauldronBrewingRecipe> holder = activeRecipe();
        if (holder == null) {
            stopBrewing();
            syncState();
            return;
        }
        CauldronBrewingRecipe recipe = holder.value();
        progress++;
        if (progress % CHECK_TICKS != 0 && progress < recipe.brewTicks()) return;

        refresh();
        if (!satisfied.contains(holder) || !getBlockState().getValue(CalciteCauldronBlock.HEATED)
                || CauldronHeat.tierBelow(level, worldPosition) < recipe.minHeat()) {
            stopBrewing();
            syncState();
        } else if (progress >= recipe.brewTicks()) {
            recipe.consume(inventory);
            fluidId = recipe.resultFluid();
            stopBrewing();
            level.playSound(null, worldPosition, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.0F);
            syncState();
            burst();
        }
    }

    /** A burst of small bubbles popping off the liquid when a brew finishes. */
    private void burst() {
        if (!(level instanceof ServerLevel server)) return;
        int lvl = getBlockState().getValue(CalciteCauldronBlock.LEVEL);
        double surface = worldPosition.getY() + (6.0 + 3.0 * lvl) / 16.0;
        // count 0 = the offset is a velocity, so every bubble rises straight up from a random spot on the surface
        for (int i = 0; i < BURST_COUNT; i++) {
            server.sendParticles(ParticleTypes.BUBBLE_POP,
                    worldPosition.getX() + 0.2 + level.random.nextDouble() * 0.6, surface + 0.02,
                    worldPosition.getZ() + 0.2 + level.random.nextDouble() * 0.6, 0, 0.0, 1.0, 0.0, 0.015);
        }
    }

    private void syncState() {
        setChanged();
        if (level != null) {
            int lvl = (amountMl * 3 + CAPACITY_ML - 1) / CAPACITY_ML; // ceil: 1-333 ml = 1, up to 1000 ml = 3
            level.setBlock(worldPosition, getBlockState()
                    .setValue(CalciteCauldronBlock.LEVEL, lvl)
                    .setValue(CalciteCauldronBlock.CONTENT, getContent()), 3);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("AmountMl", amountMl);
        tag.putString("Fluid", fluidId.toString());
        tag.putBoolean("Brewing", brewing);
        tag.putInt("Progress", progress);
        if (activeRecipeId != null) tag.putString("Recipe", activeRecipeId.toString());
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        amountMl = tag.getInt("AmountMl");
        fluidId = tag.contains("Fluid") ? ResourceLocation.parse(tag.getString("Fluid")) : WATER;
        brewing = tag.getBoolean("Brewing");
        progress = tag.getInt("Progress");
        activeRecipeId = tag.contains("Recipe") ? ResourceLocation.parse(tag.getString("Recipe")) : null;
        activeRecipe = null;
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        inventoryDirty = true;
    }
}
