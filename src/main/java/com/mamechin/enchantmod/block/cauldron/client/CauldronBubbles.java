package com.mamechin.enchantmod.block.cauldron.client;

import com.mamechin.enchantmod.block.cauldron.CauldronBlockEntity;
import com.mamechin.enchantmod.block.cauldron.CauldronSize;
import com.mamechin.enchantmod.block.SmallCauldronBlock;
import com.mamechin.enchantmod.block.cauldron.CauldronContent;

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
        var size = level.getBlockEntity(pos) instanceof CauldronBlockEntity c ? c.size() : CauldronSize.SMALL;
        double surface = pos.getY() + size.surface(lvl) + 0.02, spread = size.bubbleSpread();
        var engine = Minecraft.getInstance().particleEngine;
        for (int i = 0; i < bubbles; i++) {
            Particle bubble = engine.createParticle(ParticleTypes.BUBBLE_POP,
                    pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * spread, surface, pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * spread,
                    0.0, 0.02, 0.0);
            if (bubble != null) {
                bubble.scale(SIZE);
                engine.add(bubble);
            }
        }
    }

    private CauldronBubbles() {}
}
