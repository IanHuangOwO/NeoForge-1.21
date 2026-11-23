package com.mamechin.enchantmod.multiblock.sphere;

import com.mamechin.enchantmod.blocks.altar.AltarBlockEntity;
import com.mamechin.enchantmod.registry.ModBlockEntities;
import com.mamechin.enchantmod.multiblock.GuidedCoreBlockEntity;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.registry.ModDataComponents;
import com.mamechin.enchantmod.items.yggdrasil.Refill;
import com.mamechin.enchantmod.items.yggdrasil.TreeData;
import com.mamechin.enchantmod.items.yggdrasil.Xp;
import com.mamechin.enchantmod.items.yggdrasil.YggdrasilItem;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Holds the build guide and the altar link ({@link GuidedCoreBlockEntity}), and on the client the formed sphere's animation clock: it
 * always runs (Fable's design, author 2026-11-22), at {@link #IDLE_PACE} at rest (author, 2026-11-22: half speed) and up to
 * {@link #MAX_PACE} with the linked altar's spin. Ticks are accumulated, not multiplied, so the motion does not jump when the pace changes.
 * It also holds a bound Yggdrasil ({@link YggdrasilItem}, one slot, opened from the formed sphere): on the server it grows while its
 * owner is online, and every few seconds (by stage) it looks through the linked altar's shelves in a random order and raises one enchant
 * of the first book that has room ({@link Refill}), paid from the owner's XP.
 */
public class SphereCoreBlockEntity extends GuidedCoreBlockEntity implements MenuProvider {
    public static final float IDLE_PACE = 0.5F, MAX_PACE = 4;
    /** Client only: animation time in ticks. */
    private float clock;
    private final ItemStackHandler tree = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof YggdrasilItem && stack.has(ModDataComponents.TREE) && stack.get(ModDataComponents.TREE).bound();
        }

        @Override
        protected void onContentsChanged(int slot) {
            sync(); // clients draw the tree in the sphere
        }
    };

    public SphereCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPHERE_CORE.get(), pos, state);
    }

    /** The linked altar if it still links this sphere back, else null. */
    public AltarBlockEntity linkedAltar() {
        return altar() != null && level != null && level.getBlockEntity(altar()) instanceof AltarBlockEntity a && worldPosition.equals(a.sphere()) ? a : null;
    }

    /** Animation speed: {@link #IDLE_PACE} at rest, up to {@link #MAX_PACE} at the altar's full spin. */
    public float pace(float partialTick) {
        AltarBlockEntity altar = linkedAltar();
        return IDLE_PACE + (MAX_PACE - IDLE_PACE) * (altar == null ? 0 : altar.speed(partialTick));
    }

    /** Animation time in seconds, smoothed with the partial tick. */
    public float seconds(float partialTick) {
        return (clock + pace(partialTick) * partialTick) / 20;
    }

    public void clientTick() {
        clock += pace(0);
    }

    public ItemStackHandler tree() {
        return tree;
    }

    /** Server side, while formed: grows the tree and refills a book on its stage's schedule. Nothing happens while its owner is offline. */
    public void serverTick() {
        ItemStack stack = tree.getStackInSlot(0);
        TreeData data = stack.get(ModDataComponents.TREE);
        if (data == null || level == null || level.getServer() == null) return;
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(data.owner());
        if (owner == null) return;
        long time = level.getGameTime();
        if (time % YggdrasilItem.GROWTH_STEP == 0) {
            int before = YggdrasilItem.stageIndex(stack);
            YggdrasilItem.grow(stack, owner, YggdrasilItem.GROWTH_STEP);
            if (YggdrasilItem.stageIndex(stack) != before) sync(); // a new stage changes the tree clients draw
            else setChanged();
        }
        Balance.Stage stage = YggdrasilItem.stage(stack);
        if (time % (stage.checkSeconds() * 20L) == 0) refill(owner, stage.xpPerLevel());
    }

    private void refill(ServerPlayer owner, int cost) {
        AltarBlockEntity altar = linkedAltar();
        if (altar == null || altar.busy() || Xp.total(owner.experienceLevel, owner.experienceProgress) < cost) return;
        List<BlockPos> shelves = new ArrayList<>(altar.shelves());
        Util.shuffle(shelves, level.random);
        for (BlockPos pos : shelves) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) continue;
            List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
            Util.shuffle(slots, level.random);
            for (int slot : slots) {
                if (Refill.raise(shelf.getItem(slot), level.random)) {
                    shelf.setChanged();
                    owner.giveExperiencePoints(-cost);
                    level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1, 1);
                    return;
                }
            }
        }
    }

    /** Drops the tree at the core (the sphere was broken or released). */
    public void dropTree() {
        ItemStack stack = tree.getStackInSlot(0);
        if (stack.isEmpty() || level == null) return;
        Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack.copy());
        tree.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantmod.sphere");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new SphereMenu(id, playerInventory, tree, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tree", tree.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("tree")) tree.deserializeNBT(registries, tag.getCompound("tree"));
    }
}
