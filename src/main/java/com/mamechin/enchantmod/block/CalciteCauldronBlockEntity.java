package com.mamechin.enchantmod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Holds the exact liquid amount in ml plus its kind and the 8-slot ingredient inventory.
 * The blockstate (LEVEL, CONTENT) is only a coarse copy for models and comparators.
 */
public class CalciteCauldronBlockEntity extends BlockEntity {
    public static final int CAPACITY_ML = 1000;
    public static final int BREW_TICKS = 100; // 5 seconds

    public final ItemStackHandler inventory = new ItemStackHandler(8) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return InkBrewing.isIngredient(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int amountMl;
    private CauldronContent content = CauldronContent.WATER;
    private ResourceLocation inkwellId; // tier of the ink once content == INK (or the brew result while BREWING)
    private int progress;

    public CalciteCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CALCITE_CAULDRON.get(), pos, state);
    }

    public int getAmountMl() {
        return amountMl;
    }

    public CauldronContent getContent() {
        return content;
    }

    public ResourceLocation getInkwellId() {
        return inkwellId;
    }

    public void setAmountMl(int ml) {
        amountMl = Mth.clamp(ml, 0, CAPACITY_ML);
        if (amountMl == 0) {
            content = CauldronContent.WATER;
            inkwellId = null;
            progress = 0;
        }
        syncState();
    }

    public void startBrewing(ResourceLocation resultInkwell) {
        content = CauldronContent.BREWING;
        inkwellId = resultInkwell;
        progress = 0;
        syncState();
    }

    public void serverTick() {
        if (content != CauldronContent.BREWING) return;
        if (++progress >= BREW_TICKS) {
            content = CauldronContent.INK;
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
        if (inkwellId != null) tag.putString("Inkwell", inkwellId.toString());
        tag.put("Inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        amountMl = tag.getInt("AmountMl");
        content = CauldronContent.valueOf(tag.getString("Content").toUpperCase());
        progress = tag.getInt("Progress");
        inkwellId = tag.contains("Inkwell") ? ResourceLocation.parse(tag.getString("Inkwell")) : null;
        inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
    }
}
