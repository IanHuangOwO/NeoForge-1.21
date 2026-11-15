package com.mamechin.enchantmod.press;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.blocks.ModBlockEntities;
import com.mamechin.enchantmod.items.EnchantTemplate;
import com.mamechin.enchantmod.items.cube.EnchantCube;
import com.mamechin.enchantmod.scribing.ModDataComponents;
import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.CubeContents;
import com.mamechin.enchantmod.scribing.rules.EnchantForecast;
import com.mamechin.enchantmod.scribing.rules.PressLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Slot 0 holds an enchant cube, slots 1 to 6 hold templates; the cube's tier decides how many of those are usable. Pressing
 * combines the enchants of the usable templates into the cube. The templates are kept.
 */
public class PrintingPressBlockEntity extends BlockEntity implements MenuProvider {
    public static final int CUBE = 0, TEMPLATES = 6;

    private final ItemStackHandler inventory = new ItemStackHandler(1 + TEMPLATES) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == CUBE ? stack.getItem() instanceof EnchantCube : stack.getItem() instanceof EnchantTemplate;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public PrintingPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTING_PRESS.get(), pos, state);
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    /** How many template slots the cube in the press opens; none without a cube. */
    public static int capacity(ItemStack cube) {
        return cube.getItem() instanceof EnchantCube c ? c.capacity() : 0;
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int i = 0; i < inventory.getSlots(); i++) Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), inventory.getStackInSlot(i));
    }

    /** Server side: presses the enchants of the usable templates into the cube, replacing what it carried. */
    public void press(Player player) {
        ItemStack cube = inventory.getStackInSlot(CUBE);
        if (cube.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.press.need_cube"), true);
            return;
        }
        List<EnchantForecast.Result> results = new ArrayList<>();
        for (int i = 1; i <= TEMPLATES; i++) {
            if (!PressLayout.isOpen(i - 1, capacity(cube))) continue;
            results.addAll(EnchantForecast.forecast(inventory.getStackInSlot(i).getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY)));
        }
        CubeContents contents = CubeContents.combine(results);
        if (contents.entries().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.press.nothing"), true);
            return;
        }
        ItemStack pressed = cube.copy();
        pressed.set(ModDataComponents.CUBE_CONTENTS.get(), contents);
        inventory.setStackInSlot(CUBE, pressed);
        level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6F, 1.2F);
        List<Component> names = contents.entries().stream().map(e -> (Component) Component.translatable("enchantment." + e.enchantment().replace(':', '.'))).toList();
        player.displayClientMessage(Component.translatable("message.enchantmod.press.done", ComponentUtils.formatList(names, Component.literal(", "))), true);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.enchantmod.printing_press");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new PrintingPressMenu(id, playerInventory, inventory, ContainerLevelAccess.create(level, worldPosition));
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
