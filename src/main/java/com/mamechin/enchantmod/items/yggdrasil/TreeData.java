package com.mamechin.enchantmod.items.yggdrasil;

import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A bound Yggdrasil: its player (and their name, for the tooltip), its age in years, and the game ticks grown into the current year. */
public record TreeData(UUID owner, String ownerName, int age, int growth) {
    public static final Codec<TreeData> CODEC = RecordCodecBuilder.create(i -> i.group(
            UUIDUtil.CODEC.fieldOf("owner").forGetter(TreeData::owner),
            Codec.STRING.fieldOf("owner_name").forGetter(TreeData::ownerName),
            Codec.INT.fieldOf("age").forGetter(TreeData::age),
            Codec.INT.fieldOf("growth").forGetter(TreeData::growth)).apply(i, TreeData::new));
    public static final StreamCodec<ByteBuf, TreeData> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, TreeData::owner, ByteBufCodecs.STRING_UTF8, TreeData::ownerName,
            ByteBufCodecs.VAR_INT, TreeData::age, ByteBufCodecs.VAR_INT, TreeData::growth, TreeData::new);
}
