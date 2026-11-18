package com.mamechin.enchantmod.particle.client;

import com.mamechin.enchantmod.particle.GlyphPath;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * A vanilla enchant glyph (the sga sprites) that wanders from a bookshelf to the altar or the wheel's hub on its own random path
 * ({@link GlyphPath}) and lands exactly on it. Its life grows with the distance so far glyphs do not race. Fully lit.
 */
public class DrainGlyphParticle extends TextureSheetParticle {
    private final GlyphPath path;

    protected DrainGlyphParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
        super(level, x + dx, y + dy, z + dz);
        double[] r = new double[6];
        for (int i = 0; i < r.length; i++) r[i] = random.nextDouble();
        path = GlyphPath.of(new double[] {x + dx, y + dy, z + dz}, new double[] {x, y, z}, r);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        lifetime = (int) Math.min(80, 20 + distance * 5) + random.nextInt(10);
        quadSize = 0.1F * (random.nextFloat() * 0.5F + 0.2F);
        float shade = random.nextFloat() * 0.6F + 0.4F;
        rCol = 0.9F * shade;
        gCol = 0.9F * shade;
        bCol = shade;
        hasPhysics = false;
        xd = yd = zd = 0;
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        double[] p = path.at((double) age / lifetime);
        x = p[0];
        y = p[1];
        z = p[2];
    }

    @Override
    public int getLightColor(float partialTick) {
        return 0xF000F0;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
            DrainGlyphParticle particle = new DrainGlyphParticle(level, x, y, z, dx, dy, dz);
            particle.pickSprite(sprites);
            return particle;
        }
    }
}
