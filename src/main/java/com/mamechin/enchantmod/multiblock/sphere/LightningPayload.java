package com.mamechin.enchantmod.multiblock.sphere;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** A refill lightning bolt from the sphere to a book on a shelf, sent to the players near the sphere; each client draws it. */
public record LightningPayload(Vec3 from, Vec3 to, long seed) implements CustomPacketPayload {
    public static final Type<LightningPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "sphere_lightning"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LightningPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVec3(p.from);
        buf.writeVec3(p.to);
        buf.writeLong(p.seed);
    }, buf -> new LightningPayload(buf.readVec3(), buf.readVec3(), buf.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar registrar) {
        // the client class is only loaded when a bolt arrives, so a dedicated server never touches it
        registrar.playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> com.mamechin.enchantmod.multiblock.sphere.client.SphereBolts.add(payload)));
    }
}
