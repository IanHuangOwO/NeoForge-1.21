package com.mamechin.enchantmod.press;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mamechin.enchantmod.blocks.ModBlockEntities;
import com.mamechin.enchantmod.items.EnchantTemplate;
import com.mamechin.enchantmod.items.cube.EnchantCube;
import com.mamechin.enchantmod.scribing.ModDataComponents;
import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.CubeContents;
import com.mamechin.enchantmod.scribing.rules.EnchantForecast;
import com.mamechin.enchantmod.scribing.rules.PressAnimation;
import com.mamechin.enchantmod.scribing.rules.PressLayout;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return isBusy() ? ItemStack.EMPTY : super.extractItem(slot, amount, simulate); // nothing comes out while pressing
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !isBusy() && (slot == CUBE ? stack.getItem() instanceof EnchantCube : stack.getItem() instanceof EnchantTemplate);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); // the renderer shows the cube
        }
    };

    public PrintingPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTING_PRESS.get(), pos, state);
    }

    // client only: the animation being played
    private long animStart = Long.MIN_VALUE;
    private int animStrokes;

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

    // server only: ticks left of a pressing in progress, ticks it started with, and who to tell when it is done
    private int remaining, total;
    private UUID presser;

    /** True while a pressing runs; the slots are then locked. On the client this follows the animation. */
    public boolean isBusy() {
        if (level != null && level.isClientSide) return animStrokes > 0 && level.getGameTime() - animStart < PressAnimation.duration(animStrokes);
        return remaining > 0;
    }

    /** The templates in the slots the cube in the press opens; empty when there is no cube. */
    private List<ItemStack> usableTemplates() {
        List<ItemStack> list = new ArrayList<>();
        int capacity = capacity(inventory.getStackInSlot(CUBE));
        for (int i = 1; i <= TEMPLATES; i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (PressLayout.isOpen(i - 1, capacity) && stack.getItem() instanceof EnchantTemplate) list.add(stack);
        }
        return list;
    }

    /** Server side: starts the pressing, one second per template. The cube and the templates are checked again when it ends. */
    public void press(Player player) {
        if (isBusy()) return;
        if (!(inventory.getStackInSlot(CUBE).getItem() instanceof EnchantCube)) {
            player.displayClientMessage(Component.translatable("message.enchantmod.press.need_cube"), true);
            return;
        }
        if (EnchantCube.isPressed(inventory.getStackInSlot(CUBE))) {
            player.displayClientMessage(Component.translatable("message.enchantmod.press.already_pressed"), true);
            return;
        }
        List<ItemStack> templates = usableTemplates().stream().filter(t -> !t.isEmpty()).toList();
        if (CubeContents.combine(forecasts(templates)).entries().isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.press.nothing"), true);
            return;
        }
        total = remaining = PressAnimation.duration(templates.size());
        presser = player.getUUID();
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, templates.size());
        setChanged();
    }

    private static List<EnchantForecast.Result> forecasts(List<ItemStack> templates) {
        List<EnchantForecast.Result> results = new ArrayList<>();
        for (ItemStack t : templates) results.addAll(EnchantForecast.forecast(t.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY)));
        return results;
    }

    /** Server side, every tick: counts the pressing down, a stamp sound at every impact, and finishes it. */
    public void serverTick() {
        if (remaining <= 0) return;
        if ((total - remaining) % PressAnimation.STROKE == PressAnimation.DOWN) level.playSound(null, worldPosition, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5F, 1.2F);
        if (--remaining > 0) return;
        Player player = presser == null ? null : level.getPlayerByUUID(presser);
        presser = null;
        ItemStack cube = inventory.getStackInSlot(CUBE);
        if (!(cube.getItem() instanceof EnchantCube) || EnchantCube.isPressed(cube)) return; // the cube is gone or already pressed: nothing to press into
        CubeContents contents = CubeContents.combine(forecasts(usableTemplates())).capped(((EnchantCube) cube.getItem()).maxLevel()); // read again: only the templates the cube opens now count
        if (contents.entries().isEmpty()) return;
        ItemStack pressed = cube.copy();
        pressed.set(ModDataComponents.CUBE_CONTENTS.get(), contents);
        inventory.setStackInSlot(CUBE, pressed);
        if (player != null) {
            List<Component> names = contents.entries().stream().map(e -> (Component) Component.translatable("enchantment." + e.enchantment().replace(':', '.'))).toList();
            player.displayClientMessage(Component.translatable("message.enchantmod.press.done", ComponentUtils.formatList(names, Component.literal(", "))), true);
        }
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id != 1 || level == null) return super.triggerEvent(id, param);
        if (!level.isClientSide) return true; // true on the server is what makes the game send the event to the clients
        animStart = level.getGameTime();
        animStrokes = param;
        return true;
    }

    /** Client side: where the moving parts are now. */
    public PressAnimation.Pose pose(float partialTick) {
        if (level == null || animStrokes <= 0) return PressAnimation.Pose.REST;
        return PressAnimation.at(level.getGameTime() + partialTick - animStart, animStrokes);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
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
        tag.putInt("remaining", remaining);
        tag.putInt("total", total);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("items"));
        remaining = tag.getInt("remaining");
        total = tag.getInt("total");
    }
}
