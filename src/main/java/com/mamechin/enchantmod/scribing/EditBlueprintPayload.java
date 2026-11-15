package com.mamechin.enchantmod.scribing;

import com.mamechin.enchantmod.scribing.rules.BlueprintRules;
import java.util.List;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** One edit from the scribing table screen: place a rune, erase a cell, or add a line (ScribingTableMenu.OP_*). */
public record EditBlueprintPayload(int op, int cell, int cell2, String rune, List<Integer> path) implements CustomPacketPayload {
    public static final Type<EditBlueprintPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "edit_blueprint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EditBlueprintPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EditBlueprintPayload::op,
            ByteBufCodecs.VAR_INT, EditBlueprintPayload::cell,
            ByteBufCodecs.VAR_INT, EditBlueprintPayload::cell2,
            ByteBufCodecs.stringUtf8(128), EditBlueprintPayload::rune,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(BlueprintRules.CELLS)), EditBlueprintPayload::path,
            EditBlueprintPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof ScribingTableMenu menu) {
                menu.applyEdit(payload.op(), payload.cell(), payload.cell2(), payload.rune(), payload.path());
            }
        });
    }
}
