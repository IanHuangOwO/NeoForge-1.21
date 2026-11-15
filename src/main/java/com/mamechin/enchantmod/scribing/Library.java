package com.mamechin.enchantmod.scribing;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.items.ModItems;
import com.mamechin.enchantmod.scribing.rules.Blueprint;
import com.mamechin.enchantmod.scribing.rules.SavedTemplate;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The templates a player keeps in the first research handbook they carry. */
public final class Library {
    public static final int MAX = 50;

    private Library() {}

    /** The handbook in the inventory or the off hand, or an empty stack. */
    public static ItemStack handbook(Player player) {
        for (ItemStack stack : player.getInventory().items) if (stack.is(ModItems.RESEARCH_HANDBOOK.get())) return stack;
        for (ItemStack stack : player.getInventory().offhand) if (stack.is(ModItems.RESEARCH_HANDBOOK.get())) return stack;
        return ItemStack.EMPTY;
    }

    public static List<SavedTemplate> saved(Player player) {
        return handbook(player).getOrDefault(ModDataComponents.SAVED_TEMPLATES.get(), List.of());
    }

    /** Server side: keeps a copy of the drawing on this template, under its name; a saved template of the same name is replaced. */
    public static void save(Player player, ItemStack template) {
        ItemStack book = handbook(player);
        Blueprint blueprint = template.getOrDefault(ModDataComponents.BLUEPRINT.get(), Blueprint.EMPTY);
        if (book.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.enchantmod.library.no_handbook"), true);
            return;
        }
        if (blueprint.runes().isEmpty()) return;
        Component custom = template.get(DataComponents.CUSTOM_NAME);
        String name = custom == null ? "" : custom.getString();
        List<SavedTemplate> list = new ArrayList<>(saved(player));
        list.removeIf(t -> t.name().equals(name));
        if (list.size() >= MAX) {
            player.displayClientMessage(Component.translatable("message.enchantmod.library.full"), true);
            return;
        }
        list.add(new SavedTemplate(name, blueprint));
        book.set(ModDataComponents.SAVED_TEMPLATES.get(), List.copyOf(list));
        player.displayClientMessage(Component.translatable("message.enchantmod.library.saved"), true);
    }

    /** Server side: forgets the saved template of that name. */
    public static void delete(Player player, String name) {
        ItemStack book = handbook(player);
        if (book.isEmpty()) return;
        List<SavedTemplate> list = new ArrayList<>(saved(player));
        if (list.removeIf(t -> t.name().equals(name))) book.set(ModDataComponents.SAVED_TEMPLATES.get(), List.copyOf(list));
    }
}
