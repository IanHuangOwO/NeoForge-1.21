package com.mamechin.enchantmod.altar;

import java.util.ArrayList;
import java.util.List;

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

/** The altar holds the item to enchant and an enchant cube. A quill used on it fixes the cube's enchants onto the item. */
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

    public AltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALTAR.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
        }
    }

    /**
     * Fixes the cube's enchants onto the item, each rolled from its chances. An enchant the item cannot take, or that
     * clashes with one already there, is skipped. The cube is used up unless nothing in it fits the item. Server side only.
     */
    public void enchant(Player player) {
        ItemStack target = inventory.getStackInSlot(ITEM), cube = inventory.getStackInSlot(CUBE);
        if (target.isEmpty() || cube.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.need_both"), true);
            return;
        }
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
            player.displayClientMessage(Component.translatable("message.enchantmod.altar.nothing"), true);
            return;
        }
        inventory.setStackInSlot(CUBE, ItemStack.EMPTY); // the cube is used up, whether or not the fixation worked
        level.playSound(null, worldPosition, done.isEmpty() ? SoundEvents.GRINDSTONE_USE : SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        Component list = ComponentUtils.formatList(done, Component.literal(", "));
        player.displayClientMessage(done.isEmpty()
                ? Component.translatable("message.enchantmod.altar.failed")
                : Component.translatable("message.enchantmod.altar.done", list), true);
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

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
