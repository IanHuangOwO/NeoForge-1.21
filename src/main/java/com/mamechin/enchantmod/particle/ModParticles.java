package com.mamechin.enchantmod.particle;

import com.mamechin.enchantmod.EnchantMod;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = DeferredRegister.create(Registries.PARTICLE_TYPE, EnchantMod.MODID);

    /**
     * An enchant glyph drained from a book. Spawned like the vanilla enchant particle: position = where it lands, speed = the offset
     * back to where it starts; it wanders on the way ({@link GlyphPath}).
     */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DRAIN_GLYPH = PARTICLE_TYPES.register("drain_glyph", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
