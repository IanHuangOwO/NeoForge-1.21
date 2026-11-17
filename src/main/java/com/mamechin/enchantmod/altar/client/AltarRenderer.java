package com.mamechin.enchantmod.altar.client;

import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.altar.AltarSpin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the flyer (Fable's {@code altar_flyer}, turning about the block's vertical axis) with the cube as its bobbin in the socket,
 * and the item floating above. While enchanting, the flyer's and the pillar's glow layers are drawn at full brightness. With a
 * spinning wheel linked (tier 2) the wheel shows the cube too. The pillar itself is the block model.
 */
public class AltarRenderer implements BlockEntityRenderer<AltarBlockEntity> {
    public static final ModelResourceLocation FLYER = part("altar_flyer"), FLYER_GLOW = part("altar_flyer_glow"), BASE_GLOW = part("altar_base_glow");

    private final ItemRenderer itemRenderer;

    public AltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "block/" + name));
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
            pose.translate(0.5, 1.625 + 0.05 * Math.sin(time / 10), 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(time * 2));
            pose.scale(0.6F, 0.6F, 0.6F);
            itemRenderer.renderStatic(item, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 0);
            pose.popPose();
        }
        boolean active = AltarSpin.speed(altar.elapsed(partialTick)) > 0;
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        if (active) draw(pose, consumer, BASE_GLOW, LightTexture.FULL_BRIGHT, overlay);
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(altar.angle(partialTick)));
        pose.translate(-0.5, 0, -0.5);
        draw(pose, consumer, FLYER, light, overlay);
        if (active) draw(pose, consumer, FLYER_GLOW, LightTexture.FULL_BRIGHT, overlay);
        if (!cube.isEmpty()) {
            pose.translate(0.5, 1.0, 0.5); // the 8 px socket in the middle of the flyer, y 12 to 20 px
            pose.scale(0.5F, 0.5F, 0.5F);
            itemRenderer.renderStatic(cube, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 1);
        }
        pose.popPose();
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        var model = Minecraft.getInstance().getModelManager().getModel(location);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The item floats in the block above. */
    @Override
    public AABB getRenderBoundingBox(AltarBlockEntity altar) {
        return new AABB(altar.getBlockPos()).expandTowards(0, 1, 0);
    }
}
