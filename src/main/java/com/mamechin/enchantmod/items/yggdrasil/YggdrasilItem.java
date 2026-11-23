package com.mamechin.enchantmod.items.yggdrasil;

import java.util.List;

import com.mamechin.enchantmod.balance.Balance;
import com.mamechin.enchantmod.registry.ModDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The Yggdrasil (author, 2026-11-23): a seed that grows into the world tree. Use it in the air to bind it to yourself ({@link TreeData}).
 * It ages one year per Minecraft day ({@code ticks_per_year} in {@code items/yggdrasil.json}) only while it is in a glass sphere and its
 * owner is online (author, 2026-11-23: not in an inventory), and each year costs the owner XP points; without enough it waits at the end of the year. Its stage
 * (seed, sprout, sapling, ...) comes from its age and sets its name, model, and in a sphere how cheaply and often it refills books
 * ({@link com.mamechin.enchantmod.multiblock.sphere.SphereCoreBlockEntity}). Anyone can carry it, but it only grows and pays from its owner.
 */
public class YggdrasilItem extends Item {
    /** Ticks between growth steps, so the stack (and its sync to the client) changes only now and then. */
    public static final int GROWTH_STEP = 100;

    public YggdrasilItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static Balance.Stage stage(ItemStack stack) {
        TreeData data = stack.get(ModDataComponents.TREE);
        return Balance.current().yggdrasil().stage(data == null ? 0 : data.age());
    }

    /** The stage index, for the item model's {@code enchantmod:stage} property. */
    public static int stageIndex(ItemStack stack) {
        TreeData data = stack.get(ModDataComponents.TREE);
        return Balance.current().yggdrasil().index(data == null ? 0 : data.age());
    }

    /**
     * Grows the tree by {@code ticks} when {@code owner} is its owner. A year that ends is paid from the owner's XP; when they have too
     * little the tree stays at the end of the year until they do. Tells the owner when a new stage is reached.
     */
    public static void grow(ItemStack stack, ServerPlayer owner, int ticks) {
        TreeData data = stack.get(ModDataComponents.TREE);
        if (data == null || !data.owner().equals(owner.getUUID())) return;
        Balance.Yggdrasil y = Balance.current().yggdrasil();
        int age = data.age(), growth = Math.min(data.growth() + ticks, y.ticksPerYear());
        if (growth >= y.ticksPerYear()) {
            int cost = y.stage(age).yearCost();
            if (Xp.total(owner.experienceLevel, owner.experienceProgress) >= cost) {
                owner.giveExperiencePoints(-cost);
                age++;
                growth = 0;
                if (y.index(age) > y.index(data.age())) {
                    owner.sendSystemMessage(Component.translatable("message.enchantmod.yggdrasil.grew", stageName(y.stage(age))));
                }
            }
        }
        stack.set(ModDataComponents.TREE, new TreeData(data.owner(), owner.getGameProfile().getName(), age, growth));
    }

    private static Component stageName(Balance.Stage stage) {
        return Component.translatableWithFallback("item.enchantmod.yggdrasil." + stage.name(), stage.name());
    }

    @Override
    public Component getName(ItemStack stack) {
        return stageName(stage(stack));
    }

    /** An unbound tree at the start of each stage, for the creative tab (author, 2026-11-23); the first use in the air binds it, keeping its age. */
    public static List<ItemStack> stages(Item item) {
        return Balance.current().yggdrasil().stages().stream().map(stage -> {
            ItemStack stack = new ItemStack(item);
            if (stage.age() > 0) stack.set(ModDataComponents.TREE, new TreeData(TreeData.NOBODY, "", stage.age(), 0));
            return stack;
        }).toList();
    }

    /** Use in the air binds an unbound tree to the player. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        TreeData data = stack.get(ModDataComponents.TREE);
        if (data != null && data.bound()) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            stack.set(ModDataComponents.TREE, new TreeData(player.getUUID(), player.getGameProfile().getName(), data == null ? 0 : data.age(), 0));
            player.displayClientMessage(Component.translatable("message.enchantmod.yggdrasil.bound"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        TreeData data = stack.get(ModDataComponents.TREE);
        if (data == null || !data.bound()) {
            tooltip.add(Component.translatable("item.enchantmod.yggdrasil.unbound").withStyle(ChatFormatting.GRAY));
            if (data != null) tooltip.add(Component.translatable("item.enchantmod.yggdrasil.age", data.age(), 0).withStyle(ChatFormatting.GRAY));
            return;
        }
        Balance.Yggdrasil y = Balance.current().yggdrasil();
        tooltip.add(Component.translatable("item.enchantmod.yggdrasil.owner", data.ownerName()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.enchantmod.yggdrasil.age", data.age(), data.growth() * 100 / y.ticksPerYear()).withStyle(ChatFormatting.GRAY));
        int next = y.index(data.age()) + 1;
        if (next < y.stages().size()) {
            Balance.Stage n = y.stages().get(next);
            tooltip.add(Component.translatable("item.enchantmod.yggdrasil.next", stageName(n), n.age()).withStyle(ChatFormatting.DARK_GRAY));
        }
        Balance.Stage s = y.stage(data.age());
        tooltip.add(Component.translatable("item.enchantmod.yggdrasil.costs", s.yearCost(), s.xpPerLevel(), s.checkSeconds()).withStyle(ChatFormatting.DARK_GRAY));
    }
}
