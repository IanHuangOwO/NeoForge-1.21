package com.mamechin.enchantmod.altar.client;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Shows the item turning above the altar and the enchant cube floating over it. */
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
    private final ItemRenderer itemRenderer;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(AltarBlockEntity altar, float partialTick, PoseStack pose, MultiBufferSource buffer, int ignoredLight, int overlay) {
        var level = altar.getLevel();
        if (level == null) return;
        int light = LevelRenderer.getLightColor(level, altar.getBlockPos().above());
        float time = level.getGameTime() + partialTick;
        ItemStack item = altar.inventory().getStackInSlot(AltarBlockEntity.ITEM), cube = altar.inventory().getStackInSlot(AltarBlockEntity.CUBE);
        if (!item.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.0 + 0.05 * Math.sin(time / 10), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 2));
            pose.scale(0.6F, 0.6F, 0.6F);
            itemRenderer.renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 0);
            pose.popPose();
        }
        if (!cube.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 1.5 + 0.05 * Math.cos(time / 10), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-time * 3));
            pose.scale(0.4F, 0.4F, 0.4F);
            itemRenderer.renderStatic(cube, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 1);
            pose.popPose();
        }
    }
}
