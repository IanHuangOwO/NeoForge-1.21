package com.mamechin.enchantmod.balance;

import java.util.HashMap;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The server's merged balance files, sent to a client when it joins or data is reloaded; the client parses them the same way. */
public record BalancePayload(HashMap<String, String> files) implements CustomPacketPayload {
    public static final Type<BalancePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "balance"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BalancePayload> STREAM_CODEC = ByteBufCodecs.<RegistryFriendlyByteBuf, String, String, HashMap<String, String>>map(
            HashMap::new, ByteBufCodecs.stringUtf8(64), ByteBufCodecs.stringUtf8(262144)).map(BalancePayload::new, BalancePayload::files);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        // only pure classes are touched here, so a dedicated server can load this
        registrar.playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> BalanceLoader.install(payload.files(), "client")));
    }
}
