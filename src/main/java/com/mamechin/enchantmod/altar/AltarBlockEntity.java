package com.mamechin.enchantmod.altar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mamechin.enchantmod.blocks.ModBlockEntities;
import com.mamechin.enchantmod.items.cube.EnchantCube;
import com.mamechin.enchantmod.scribing.ModDataComponents;
import com.mamechin.enchantmod.scribing.rules.CubeContents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

import com.mamechin.enchantmod.EnchantMod;

/**
 * The altar holds the item to enchant and an enchant cube. A quill used on it starts a 10 second enchanting ({@link AltarSpin}): the
 * flyer spins up, the cube's enchants are fixed onto the item, and it slows down. With a formed spinning wheel in front of it (tier 2)
 * the wheel shows the cube and spins instead.
 */
public class AltarBlockEntity extends BlockEntity {
    public static final int ITEM = 0, CUBE = 1;
    public static final TagKey<Item> QUILLS = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "quills"));

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return (slot == CUBE) == (stack.getItem() instanceof EnchantCube) && !stack.is(QUILLS);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    };

    /** Ticks left of the enchanting in progress, 0 when idle. Synced; the client counts down on its own between syncs. */
    private int remaining;
    private UUID starter;
    /** Client only: how far the flyer or wheel has turned, in degrees. */
    private float angle;

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public boolean busy() {
        return remaining > 0;
    }

    /** Ticks since the quill click (fractional with the partial tick), 0 when idle. */
    public float elapsed(float partialTick) {
        return busy() ? AltarSpin.DURATION - remaining + partialTick : 0;
    }

    /** Degrees turned so far, smoothed with the partial tick. Client side. */
    public float angle(float partialTick) {
        return angle + AltarSpin.speed(elapsed(partialTick)) * MAX_DEGREES_PER_TICK * partialTick;
    }

    private static final float MAX_DEGREES_PER_TICK = 24;

    /** Starts the enchanting. Server side only. */
    public void start(Player player) {
        if (busy()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.busy"), true);
            return;
        }
        if (inventory.getStackInSlot(ITEM).isEmpty() || inventory.getStackInSlot(CUBE).isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.need_both"), true);
            return;
        }
        remaining = AltarSpin.DURATION;
        starter = player.getUUID();
        sync();
    }

    public void serverTick() {
        if (!busy()) return;
        remaining--;
        if (AltarSpin.DURATION - remaining == AltarSpin.ENCHANT_AT) enchant(starter == null ? null : level.getPlayerByUUID(starter));
        if (remaining == 0) sync();
        else setChanged();
    }

    public void clientTick() {
        if (!busy()) return;
        angle = (angle + AltarSpin.speed(elapsed(0)) * MAX_DEGREES_PER_TICK) % 360;
        remaining--;
    }

    private void sync() {
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }

    /**
     * Fixes the cube's enchants onto the item, each rolled from its chances. An enchant the item cannot take, or that
     * clashes with one already there, is skipped. The cube is used up unless nothing in it fits the item. Server side only;
     * {@code player} (who started it, null if gone) gets the result on the action bar.
     */
    private void enchant(Player player) {
        ItemStack target = inventory.getStackInSlot(ITEM), cube = inventory.getStackInSlot(CUBE);
        if (target.isEmpty() || cube.isEmpty()) return; // the slots are locked while busy, so only a broken world gets here
        CubeContents contents = cube.getOrDefault(ModDataComponents.CUBE_CONTENTS.get(), CubeContents.EMPTY);
        var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Component> done = new ArrayList<>();
        int[] failed = {0};
        EnchantmentHelper.updateEnchantments(target, enchants -> {
            for (CubeContents.Entry entry : contents.entries()) {
                Holder<Enchantment> holder = registry.get(ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.parse(entry.enchantment()))).orElse(null);
                if (holder == null || !holder.value().canEnchant(target) || !EnchantmentHelper.isEnchantmentCompatible(enchants.keySet(), holder)) continue;
                int levelGot = entry.roll(level.random.nextDouble());
                if (levelGot <= 0) failed[0]++;
                else {
                    enchants.upgrade(holder, levelGot);
                    done.add(Enchantment.getFullname(holder, levelGot));
                }
            }
        });
        inventory.setStackInSlot(ITEM, target); // updateEnchantments changed the stack in place; this resyncs the client
        if (done.isEmpty() && failed[0] == 0) {
            tell(player, Component.translatable("message.enchantmod.altar.nothing"));
            return;
        }
        inventory.setStackInSlot(CUBE, ItemStack.EMPTY); // the cube is used up, whether or not the fixation worked
        level.playSound(null, worldPosition, done.isEmpty() ? SoundEvents.GRINDSTONE_USE : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        Component list = ComponentUtils.formatList(done, Component.literal(", "));
        tell(player, done.isEmpty()
                ? Component.translatable("message.enchantmod.altar.failed")
                : Component.translatable("message.enchantmod.altar.done", list));
    }

    private static void tell(Player player, Component message) {
        if (player != null) player.displayClientMessage(message, true);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", inventory.serializeNBT(registries));
        tag.putInt("remaining", remaining);
        if (starter != null) tag.putUUID("starter", starter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("items"));
        remaining = tag.getInt("remaining");
        starter = tag.hasUUID("starter") ? tag.getUUID("starter") : null;
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
