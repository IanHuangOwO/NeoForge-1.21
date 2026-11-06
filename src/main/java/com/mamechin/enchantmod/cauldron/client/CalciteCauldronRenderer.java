package com.mamechin.enchantmod.cauldron.client;

import com.mamechin.enchantmod.cauldron.CalciteCauldronBlock;
import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws each stored ingredient as a small flat item floating on the liquid, slowly turning in a ring. */
public class CalciteCauldronRenderer implements BlockEntityRenderer<CalciteCauldronBlockEntity> {
    private static final float ITEM_SCALE = 0.3F;
    private static final float RING_RADIUS = 0.22F;

    private final ItemRenderer itemRenderer;

    public CalciteCauldronRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(CalciteCauldronBlockEntity cauldron, float partialTick, PoseStack poseStack,
            MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var stored = cauldron.inventory;
        int count = 0;
        for (int i = 0; i < stored.getSlots(); i++) {
            if (!stored.getStackInSlot(i).isEmpty()) count++;
        }
        if (count == 0 || cauldron.getLevel() == null) return;

        // Same heights as the liquid models: floor at 4/16, surface at (6 + 3 * level)/16.
        int level = cauldron.getBlockState().getValue(CalciteCauldronBlock.LEVEL);
        float surface = (level == 0 ? 4.0F : 6.0F + 3.0F * level) / 16.0F + 0.02F;
        float time = cauldron.getLevel().getGameTime() + partialTick;

        int placed = 0;
        for (int i = 0; i < stored.getSlots(); i++) {
            ItemStack stack = stored.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            double angle = placed * (2 * Math.PI / count) + time * 0.02;
            float radius = count == 1 ? 0.0F : RING_RADIUS;
            float bob = (float) Math.sin(time * 0.1 + placed) * 0.01F;

            poseStack.pushPose();
            poseStack.translate(0.5 + Math.cos(angle) * radius, surface + bob, 0.5 + Math.sin(angle) * radius);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F)); // lie flat, facing up
            poseStack.mulPose(Axis.ZP.rotationDegrees(time * 1.5F + placed * 40.0F));
            poseStack.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
            itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY,
                    poseStack, buffer, cauldron.getLevel(), placed + (int) cauldron.getBlockPos().asLong());
            poseStack.popPose();
            placed++;
        }
    }
}
