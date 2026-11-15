package com.mamechin.enchantmod.scribing.rules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a pressed enchant cube carries: for each enchantment, the chance of each level (the rest of the chance is a failed
 * fixation). Made by combining the forecasts of the templates pressed into the cube.
 */
public record CubeContents(List<Entry> entries) {
    public static final CubeContents EMPTY = new CubeContents(List.of());

    public record Chance(int level, double chance) {
        public static final Codec<Chance> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("level").forGetter(Chance::level),
                Codec.DOUBLE.fieldOf("chance").forGetter(Chance::chance)).apply(i, Chance::new));
        public static final StreamCodec<ByteBuf, Chance> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Chance::level, ByteBufCodecs.DOUBLE, Chance::chance, Chance::new);
    }

    /** One enchantment ("namespace:path") with the chance of each level, lowest level first. */
    public record Entry(String enchantment, List<Chance> chances) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("enchantment").forGetter(Entry::enchantment),
                Chance.CODEC.listOf().fieldOf("chances").forGetter(Entry::chances)).apply(i, Entry::new));
        public static final StreamCodec<ByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(128), Entry::enchantment,
                Chance.STREAM_CODEC.apply(ByteBufCodecs.list(256)), Entry::chances, Entry::new);

        /** Turns one roll in [0, 1) into the level, or 0 when the fixation fails. */
        public int roll(double roll) {
            double acc = 0;
            for (Chance c : chances) {
                acc += c.chance();
                if (roll < acc) return c.level();
            }
            return 0;
        }

        public double failChance() {
            return Math.max(0, 1 - chances.stream().mapToDouble(Chance::chance).sum());
        }
    }

    public static final Codec<CubeContents> CODEC = Entry.CODEC.listOf().xmap(CubeContents::new, CubeContents::entries);
    public static final StreamCodec<ByteBuf, CubeContents> STREAM_CODEC = Entry.STREAM_CODEC.apply(ByteBufCodecs.list(256)).map(CubeContents::new, CubeContents::entries);

    /**
     * Combines forecasts. The same enchantment made more than once (by one template or several) keeps the best outcome: the
     * results are independent, so the chance that the best level is at most L is the product of each one's chance of at most L.
     */
    public static CubeContents combine(List<EnchantForecast.Result> results) {
        Map<String, List<EnchantForecast.Result>> byEnchantment = new LinkedHashMap<>();
        for (EnchantForecast.Result r : results) byEnchantment.computeIfAbsent(r.enchant().enchantment(), k -> new ArrayList<>()).add(r);
        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, List<EnchantForecast.Result>> group : byEnchantment.entrySet()) {
            TreeMap<Integer, Double> levels = new TreeMap<>();
            for (EnchantForecast.Result r : group.getValue()) for (int level : r.levels().keySet()) if (level > 0) levels.put(level, 0.0);
            double before = 1; // chance that the best level is below the one being looked at; to begin with, that every result failed
            for (EnchantForecast.Result r : group.getValue()) before *= 1 - r.levels().values().stream().mapToDouble(Double::doubleValue).sum();
            for (int level : new ArrayList<>(levels.keySet())) {
                double atMost = 1;
                for (EnchantForecast.Result r : group.getValue()) {
                    double above = 0;
                    for (Map.Entry<Integer, Double> e : r.levels().entrySet()) if (e.getKey() > level) above += e.getValue();
                    double notAbove = 1 - above;
                    atMost *= notAbove;
                }
                levels.put(level, Math.max(0, atMost - before));
                before = atMost;
            }
            List<Chance> chances = new ArrayList<>();
            for (Map.Entry<Integer, Double> e : levels.entrySet()) if (e.getValue() > 1e-9) chances.add(new Chance(e.getKey(), e.getValue()));
            if (!chances.isEmpty()) entries.add(new Entry(group.getKey(), List.copyOf(chances)));
        }
        return new CubeContents(List.copyOf(entries));
    }
}
