package com.mamechin.enchantmod.guide.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Draws a build guide: a small see-through copy of the wanted block in every cell that does not hold it yet. */
public final class GhostBlocks {
    private static final float SIZE = 0.6F, ALPHA = 0.5F;
    private static final RandomSource RANDOM = RandomSource.create();

    private GhostBlocks() {}

    /** One cell: where it is and the block it wants. */
    public record Cell(BlockPos pos, Block block) {}

    /** {@code pose} is at the core's corner; cells already holding their block are skipped. */
    public static void draw(Level level, BlockPos core, List<Cell> cells, PoseStack pose, MultiBufferSource buffer) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.translucent());
        var models = Minecraft.getInstance().getBlockRenderer();
        for (Cell cell : cells) {
            if (level.getBlockState(cell.pos()).is(cell.block())) continue;
            var state = cell.block().defaultBlockState();
            var model = models.getBlockModel(state);
            pose.pushPose();
            pose.translate(cell.pos().getX() - core.getX() + 0.5, cell.pos().getY() - core.getY() + 0.5, cell.pos().getZ() - core.getZ() + 0.5);
            pose.scale(SIZE, SIZE, SIZE);
            pose.translate(-0.5, -0.5, -0.5);
            for (Direction side : Direction.values()) {
                for (var quad : model.getQuads(state, side, RANDOM)) consumer.putBulkData(pose.last(), quad, 1, 1, 1, ALPHA, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            for (var quad : model.getQuads(state, null, RANDOM)) consumer.putBulkData(pose.last(), quad, 1, 1, 1, ALPHA, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            pose.popPose();
        }
    }
}
