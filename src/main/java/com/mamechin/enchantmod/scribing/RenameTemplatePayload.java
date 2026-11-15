package com.mamechin.enchantmod.scribing;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.scribing.rules.SavedTemplate;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** The new name for the template in the open scribing table; an empty name removes it. */
public record RenameTemplatePayload(String name) implements CustomPacketPayload {
    public static final Type<RenameTemplatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "rename_template"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RenameTemplatePayload> STREAM_CODEC =
            ByteBufCodecs.stringUtf8(SavedTemplate.NAME_MAX).map(RenameTemplatePayload::new, RenameTemplatePayload::name).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToServer(TYPE, STREAM_CODEC, (payload, context) -> {
            if (context.player().containerMenu instanceof ScribingTableMenu menu) menu.rename(payload.name());
        });
    }
}
