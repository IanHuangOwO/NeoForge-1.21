package com.mamechin.enchantmod.multiblock;

import com.mamechin.enchantmod.blocks.altar.AltarBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A multiblock core (spinning wheel, glass sphere). It can show a build guide: a quill on the unformed core turns the guide on or off
 * (synced, saved), and forming turns it off; the core's renderer draws the missing blocks as ghosts while it is on. It also remembers
 * the altar whose scan last linked it (synced), so the renderer can follow that altar's enchanting.
 */
public abstract class GuidedCoreBlockEntity extends BlockEntity {
    private boolean guide;
    private BlockPos altar;

    protected GuidedCoreBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean guide() {
        return guide;
    }

    public void setGuide(boolean on) {
        if (guide == on) return;
        guide = on;
        sync();
    }

    /** The altar that linked this core, or null. */
    public BlockPos altar() {
        return altar;
    }

    public void setAltar(BlockPos pos) {
        altar = pos;
        sync();
    }

    /** The core block's quill click: toggles the guide of an unformed core. */
    public static ItemInteractionResult toggle(ItemStack held, boolean formed, Level level, BlockPos pos, Player player) {
        if (!held.is(AltarBlockEntity.QUILLS) || formed || !(level.getBlockEntity(pos) instanceof GuidedCoreBlockEntity core)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide) {
            core.setGuide(!core.guide);
            player.displayClientMessage(Component.translatable(core.guide ? "message.enchantmod.guide.on" : "message.enchantmod.guide.off"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    protected void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("guide", guide);
        if (altar != null) tag.putLong("altar", altar.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        guide = tag.getBoolean("guide");
        altar = tag.contains("altar") ? BlockPos.of(tag.getLong("altar")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
