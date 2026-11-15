package com.mamechin.enchantmod.press.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.press.PrintingPressBlockEntity;
import com.mamechin.enchantmod.scribing.rules.PressAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws the moving parts of the printing press (the base is the block model): the head goes down and up, the turntable turns with
 * the cube on it, the four side gears spin. Everything is positioned in the lower block's frame; the model files give the numbers.
 */
public class PrintingPressRenderer implements BlockEntityRenderer<PrintingPressBlockEntity> {
    public static final ModelResourceLocation HEAD = part("press_head"), TURNTABLE = part("press_turntable"), GEAR = part("press_gear");
    private static final float HEAD_TRAVEL = 5 / 16F; // px to blocks

    private final BlockEntityRendererProvider.Context context;

    public PrintingPressRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "block/" + name));
    }

    @Override
    public void render(PrintingPressBlockEntity press, float partialTick, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        var level = press.getLevel();
        if (level == null) return;
        int upperLight = LevelRenderer.getLightColor(level, press.getBlockPos().above());
        PressAnimation.Pose anim = press.pose(partialTick);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        pose.pushPose();
        pose.translate(0, -anim.head() * HEAD_TRAVEL, 0);
        draw(pose, consumer, HEAD, upperLight, overlay);
        pose.popPose();

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(anim.turn()));
        pose.translate(-0.5, 0, -0.5);
        draw(pose, consumer, TURNTABLE, light, overlay);
        pose.popPose();

        for (int side = 0; side < 4; side++) {
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(90 * side));
            pose.translate(-0.5, 0, -0.5);
            pose.translate(0.5, 0.25, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(side % 2 == 0 ? anim.spin() : -anim.spin()));
            pose.translate(-0.5, -0.25, 0);
            draw(pose, consumer, GEAR, light, overlay);
            pose.popPose();
        }

        ItemStack cube = press.inventory().getStackInSlot(PrintingPressBlockEntity.CUBE);
        if (!cube.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.75, 0.5); // an 8 px cube standing on the turntable, y 8 to 16 px
            pose.mulPose(Axis.YP.rotationDegrees(anim.turn()));
            context.getItemRenderer().renderStatic(cube, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 0);
            pose.popPose();
        }
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The machine is two blocks tall; the upper half has no block entity of its own. */
    @Override
    public AABB getRenderBoundingBox(PrintingPressBlockEntity press) {
        return new AABB(press.getBlockPos()).expandTowards(0, 1, 0);
    }
}
