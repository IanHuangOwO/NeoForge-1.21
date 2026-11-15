package com.mamechin.enchantmod.scribing.rules;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import io.netty.buffer.ByteBuf;

/**
 * The layout drawn on an enchant template: runes placed on a grid and lines between them. Cells are numbered
 * y * BlueprintRules.WIDTH + x. A line runs from rune cell a to rune cell b through the (possibly empty) path of cells
 * between them; every path cell counts as occupied. Immutable, so the item stack can detect a change by equality.
 */
public record Blueprint(List<RuneAt> runes, List<Line> lines) {
    public static final Blueprint EMPTY = new Blueprint(List.of(), List.of());

    public Blueprint {
        runes = List.copyOf(runes);
        lines = List.copyOf(lines);
    }

    public record RuneAt(int cell, String rune) {
        public static final Codec<RuneAt> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("cell").forGetter(RuneAt::cell),
                Codec.STRING.fieldOf("rune").forGetter(RuneAt::rune)).apply(i, RuneAt::new));
        public static final StreamCodec<ByteBuf, RuneAt> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, RuneAt::cell, ByteBufCodecs.stringUtf8(32), RuneAt::rune, RuneAt::new);
    }

    public record Line(int a, int b, List<Integer> path) {
        public Line {
            path = List.copyOf(path);
        }

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("a").forGetter(Line::a),
                Codec.INT.fieldOf("b").forGetter(Line::b),
                Codec.INT.listOf().fieldOf("path").forGetter(Line::path)).apply(i, Line::new));
        public static final StreamCodec<ByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Line::a, ByteBufCodecs.VAR_INT, Line::b,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(BlueprintRules.CELLS)), Line::path, Line::new);
    }

    public static final Codec<Blueprint> CODEC = RecordCodecBuilder.create(i -> i.group(
            RuneAt.CODEC.listOf().fieldOf("runes").forGetter(Blueprint::runes),
            Line.CODEC.listOf().fieldOf("lines").forGetter(Blueprint::lines)).apply(i, Blueprint::new));

    public static final StreamCodec<ByteBuf, Blueprint> STREAM_CODEC = StreamCodec.composite(
            RuneAt.STREAM_CODEC.apply(ByteBufCodecs.list(BlueprintRules.CELLS)), Blueprint::runes,
            Line.STREAM_CODEC.apply(ByteBufCodecs.list(BlueprintRules.CELLS)), Blueprint::lines, Blueprint::new);
}
