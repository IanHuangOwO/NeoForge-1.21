package com.mamechin.enchantmod.items.books;

import java.util.List;
import java.util.function.Supplier;

import com.mamechin.enchantmod.EnchantMod;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Where the book level cap ({@link BookLimit}) meets vanilla: the enchanting table and villager trades trim the books they make,
 * loot is trimmed by {@link TrimBooksModifier}, and the anvil refuses a combined book over its cap. The anvil also does the whole
 * combining when a tier 1 or 2 enchanted book is involved, because vanilla's anvil only reads stored enchants from its own enchanted
 * book.
 */
public final class BookEvents {
    private BookEvents() {}

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, EnchantMod.MODID);
    public static final Supplier<MapCodec<TrimBooksModifier>> TRIM_BOOKS = LOOT_MODIFIERS.register("trim_books", () -> TrimBooksModifier.CODEC);

    public static void register(IEventBus modBus) {
        LOOT_MODIFIERS.register(modBus);
        NeoForge.EVENT_BUS.addListener((PlayerEnchantItemEvent e) -> Books.trim(e.getEnchantedItem()));
        NeoForge.EVENT_BUS.addListener(BookEvents::onTrades);
        NeoForge.EVENT_BUS.addListener(BookEvents::onAnvil);
    }

    private static void onTrades(VillagerTradesEvent event) {
        for (List<VillagerTrades.ItemListing> listings : event.getTrades().values()) {
            listings.replaceAll(listing -> (trader, random) -> {
                MerchantOffer offer = listing.getOffer(trader, random);
                if (offer != null) Books.trim(offer.getResult()); // the offer keeps this stack and hands out copies
                return offer;
            });
        }
    }

    /** True for an enchanted book of tier 1 or 2, which vanilla's anvil cannot read. */
    private static boolean ours(ItemStack stack) {
        return Books.isEnchantedBook(stack) && Books.tier(stack) > 0;
    }

    private static void onAnvil(AnvilUpdateEvent event) {
        ItemStack left = event.getLeft(), right = event.getRight();
        ItemEnchantments add = right.get(DataComponents.STORED_ENCHANTMENTS);
        if (left.isEmpty() || add == null || add.isEmpty()) return;
        boolean ours = ours(left) || ours(right), leftBook = Books.isEnchantedBook(left);
        int cap = Books.cap(left);
        if (!ours && cap < 0) return; // plain vanilla anvil work

        // vanilla's rules (AnvilMenu.createResult) for putting a book's enchants onto the left item
        DataComponentType<ItemEnchantments> type = leftBook ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
        ItemEnchantments.Mutable result = new ItemEnchantments.Mutable(left.getOrDefault(type, ItemEnchantments.EMPTY));
        boolean creative = event.getPlayer() != null && event.getPlayer().getAbilities().instabuild, applied = false;
        int cost = 0;
        for (var entry : add.entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            boolean fits = creative || leftBook || left.supportsEnchantment(holder);
            for (Holder<Enchantment> other : result.keySet()) {
                if (!other.equals(holder) && !Enchantment.areCompatible(holder, other)) {
                    fits = false;
                    cost++;
                }
            }
            if (!fits) continue;
            applied = true;
            int level = BookLimit.anvilLevel(result.getLevel(holder), entry.getIntValue(), holder.value().getMaxLevel());
            result.set(holder, level);
            cost += Math.max(1, holder.value().getAnvilCost() / 2) * level;
        }
        ItemEnchantments merged = result.toImmutable();
        if (!applied || (cap >= 0 && Books.totalLevels(merged) > cap)) {
            event.setCanceled(true); // no result: nothing fits, or the book would go over its cap
            return;
        }
        if (!ours) return; // under the cap: vanilla builds the result itself

        ItemStack out = left.copy();
        out.set(type, merged);
        int leftRepair = left.getOrDefault(DataComponents.REPAIR_COST, 0), rightRepair = right.getOrDefault(DataComponents.REPAIR_COST, 0);
        cost += leftRepair + rightRepair;
        String name = event.getName();
        if (name != null && !StringUtil.isBlank(name)) {
            if (!name.equals(left.getHoverName().getString())) {
                cost++;
                out.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            }
        } else if (left.has(DataComponents.CUSTOM_NAME)) {
            cost++;
            out.remove(DataComponents.CUSTOM_NAME);
        }
        if (left.getCount() > 1) cost = 40; // vanilla: too expensive for a stack
        out.set(DataComponents.REPAIR_COST, AnvilMenu.calculateIncreasedRepairCost(Math.max(leftRepair, rightRepair)));
        event.setOutput(out);
        event.setCost(cost);
        event.setMaterialCost(1);
    }
}
