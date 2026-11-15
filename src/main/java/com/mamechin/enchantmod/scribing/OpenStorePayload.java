package com.mamechin.enchantmod.scribing;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Asks the server to open the template store of the research handbook the player carries. */
public record OpenStorePayload() implements CustomPacketPayload {
    public static final Type<OpenStorePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "open_store"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenStorePayload> STREAM_CODEC = StreamCodec.unit(new OpenStorePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            ItemStack book = HandbookStoreMenu.handbook(context.player());
            if (book.isEmpty()) return;
            context.player().openMenu(new SimpleMenuProvider((id, inventory, player) -> new HandbookStoreMenu(id, inventory, book),
                    Component.translatable("gui.enchantmod.research.tab.templates")), buffer -> buffer.writeVarInt(HandbookStoreMenu.slotsFor(book)));
        });
    }
}
