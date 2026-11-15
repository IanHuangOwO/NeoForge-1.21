package com.mamechin.enchantmod.scribing.rules;

import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A template kept in the research handbook: the name it had and what is drawn on it. An empty name means it never got one. */
public record SavedTemplate(String name, Blueprint blueprint) {
    public static final int NAME_MAX = 32;

    public static final Codec<SavedTemplate> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("name").forGetter(SavedTemplate::name),
            Blueprint.CODEC.fieldOf("blueprint").forGetter(SavedTemplate::blueprint)).apply(i, SavedTemplate::new));

    public static final StreamCodec<ByteBuf, SavedTemplate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(NAME_MAX), SavedTemplate::name,
            Blueprint.STREAM_CODEC, SavedTemplate::blueprint, SavedTemplate::new);

    /** The enchants the drawing makes. */
    public List<Enchant> enchants() {
        return EnchantForecast.forecast(blueprint).stream().map(EnchantForecast.Result::enchant).toList();
    }

    /** True when the query is part of the name or of the name of one of its enchants, ignoring case; an empty query matches all. */
    public boolean matches(String query, Function<Enchant, String> enchantName) {
        String q = query.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) return true;
        return enchants().stream().anyMatch(e -> enchantName.apply(e).toLowerCase(Locale.ROOT).contains(q));
    }
}
