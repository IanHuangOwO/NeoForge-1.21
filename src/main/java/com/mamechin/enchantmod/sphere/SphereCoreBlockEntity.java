package com.mamechin.enchantmod.sphere;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.blocks.ModBlockEntities;
import com.mamechin.enchantmod.guide.GuidedCoreBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds the build guide and the altar link ({@link GuidedCoreBlockEntity}), and on the client the formed sphere's animation clock: it
 * always runs (Fable's design, author 2026-11-22), at {@link #IDLE_PACE} at rest (author, 2026-11-22: half speed) and up to
 * {@link #MAX_PACE} with the linked altar's spin. Ticks are accumulated, not multiplied, so the motion does not jump when the pace changes.
 */
public class SphereCoreBlockEntity extends GuidedCoreBlockEntity {
    public static final float IDLE_PACE = 0.5F, MAX_PACE = 4;
    /** Client only: animation time in ticks. */
    private float clock;

    public SphereCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPHERE_CORE.get(), pos, state);
    }

    /** The linked altar if it still links this sphere back, else null. */
    public AltarBlockEntity linkedAltar() {
        return altar() != null && level != null && level.getBlockEntity(altar()) instanceof AltarBlockEntity a && worldPosition.equals(a.sphere()) ? a : null;
    }

    /** Animation speed: {@link #IDLE_PACE} at rest, up to {@link #MAX_PACE} at the altar's full spin. */
    public float pace(float partialTick) {
        AltarBlockEntity altar = linkedAltar();
        return IDLE_PACE + (MAX_PACE - IDLE_PACE) * (altar == null ? 0 : altar.speed(partialTick));
    }

    /** Animation time in seconds, smoothed with the partial tick. */
    public float seconds(float partialTick) {
        return (clock + pace(partialTick) * partialTick) / 20;
    }

    public void clientTick() {
        clock += pace(0);
    }
}
