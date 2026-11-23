package com.mamechin.enchantmod.blocks.press;

import com.mamechin.enchantmod.registry.ModBlockEntities;
import com.mamechin.enchantmod.items.EnchantTemplate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Six enchant templates stacked in a column. The block state follows which slots are full, so the model can show them. */
public class TemplateShelfBlockEntity extends BlockEntity {
    public static final int SLOTS = 6;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof EnchantTemplate;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level == null || level.isClientSide) return;
            BlockState state = getBlockState();
            for (int i = 0; i < SLOTS; i++) state = state.setValue(TemplateShelfBlock.FULL[i], !getStackInSlot(i).isEmpty());
            level.setBlock(worldPosition, state, 3);
        }
    };

    public TemplateShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TEMPLATE_SHELF.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < SLOTS; i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
    }

    /** The client needs the templates to show their tooltip when the player points at one. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
