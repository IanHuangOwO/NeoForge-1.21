package com.mamechin.enchantmod.scribing;

import java.util.HashMap;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.scribing.rules.Definitions;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The server's rune and enchant files, sent to a client when it joins or data is reloaded; the client parses them the same way. */
public record DefinitionsPayload(Definitions.Raw raw) implements CustomPacketPayload {
    public static final Type<DefinitionsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "definitions"));

    private static final StreamCodec<ByteBuf, HashMap<String, String>> FILES = ByteBufCodecs.map(HashMap::new, ByteBufCodecs.stringUtf8(256), ByteBufCodecs.stringUtf8(32767));

    public static final StreamCodec<RegistryFriendlyByteBuf, DefinitionsPayload> STREAM_CODEC = StreamCodec.composite(
            FILES, p -> new HashMap<>(p.raw().runes()),
            FILES, p -> new HashMap<>(p.raw().enchants()),
            FILES, p -> new HashMap<>(p.raw().templates()),
            ByteBufCodecs.stringUtf8(32767), p -> p.raw().forecast() == null ? "" : p.raw().forecast(),
            (runes, enchants, templates, forecast) -> new DefinitionsPayload(new Definitions.Raw(runes, enchants, templates, forecast.isEmpty() ? null : forecast)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void register(PayloadRegistrar registrar) {
        // only pure classes are touched here, so a dedicated server can load this
        registrar.playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> DefinitionLoader.install(payload.raw(), "client")));
    }
}
