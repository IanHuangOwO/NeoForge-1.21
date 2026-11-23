package com.mamechin.enchantmod.blocks.scribing;

import com.mamechin.enchantmod.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Holds what sits in the table: an inkwell, a quill and an enchant template. Lives on the left half of the table only. */
public class ScribingTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INKWELL = 0, QUILL = 1, TEMPLATE = 2;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (slot == TEMPLATE && level != null && !level.isClientSide && getBlockState().getBlock() instanceof ScribingTableBlock) {
                ScribingTableBlock.setTemplate(level, worldPosition, getBlockState(), !getStackInSlot(TEMPLATE).isEmpty());
            }
        }
    };

    public ScribingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SCRIBING_TABLE.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantmod.scribing_table");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new ScribingTableMenu(id, playerInventory, inventory, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("items"));
    }
}
