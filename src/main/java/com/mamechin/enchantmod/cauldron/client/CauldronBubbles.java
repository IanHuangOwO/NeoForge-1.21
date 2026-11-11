package com.mamechin.enchantmod.cauldron.client;

import com.mamechin.enchantmod.cauldron.SmallCauldronBlock;
import com.mamechin.enchantmod.cauldron.CauldronContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Client tick of a heated cauldron: small bubbles pop on the liquid surface, more while brewing. */
public final class CauldronBubbles {
    private static final float IDLE_CHANCE = 0.5F;   // per tick, about 10 bubbles a second
    private static final int BREWING_BUBBLES = 2;    // per tick
    private static final float SIZE = 0.5F;          // half the vanilla bubble

    public static void tick(Level level, BlockPos pos, BlockState state) {
        int lvl = state.getValue(SmallCauldronBlock.LEVEL);
        if (!state.getValue(SmallCauldronBlock.HEATED) || lvl == 0) return;

        RandomSource random = level.random;
        int bubbles = state.getValue(SmallCauldronBlock.CONTENT) == CauldronContent.BREWING
                ? BREWING_BUBBLES : (random.nextFloat() < IDLE_CHANCE ? 1 : 0);
        double surface = pos.getY() + (6.0 + 3.0 * lvl) / 16.0 + 0.02;
        var engine = Minecraft.getInstance().particleEngine;
        for (int i = 0; i < bubbles; i++) {
            Particle bubble = engine.createParticle(ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, surface, pos.getZ() + 0.2 + random.nextDouble() * 0.6,
                    0.0, 0.02, 0.0);
            if (bubble != null) {
                bubble.scale(SIZE);
                engine.add(bubble);
            }
        }
    }

    private CauldronBubbles() {}
}
