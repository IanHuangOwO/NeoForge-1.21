package com.mamechin.enchantmod.scribing;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.scribing.rules.SavedTemplate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** A handbook library action from the client: save the template in the open scribing table, or delete a saved one by name. */
public record LibraryPayload(int op, String name) implements CustomPacketPayload {
    public static final int OP_SAVE = 0, OP_DELETE = 1;
    public static final Type<LibraryPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "library"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LibraryPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LibraryPayload::op,
            ByteBufCodecs.stringUtf8(SavedTemplate.NAME_MAX), LibraryPayload::name,
            LibraryPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (payload.op() == OP_SAVE && context.player().containerMenu instanceof ScribingTableMenu menu) Library.save(context.player(), menu.templateStack());
            else if (payload.op() == OP_DELETE) Library.delete(context.player(), payload.name());
        });
    }
}
