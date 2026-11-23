package com.mamechin.enchantmod.blocks.press.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.press.PrintingPressBlockEntity;
import com.mamechin.enchantmod.blocks.scribing.rules.PressAnimation;
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
 * Draws the moving parts of the printing press (the base is the block model): the head goes down and up, the cube lifts and rolls to
 * show a new face to the head, the four roller gears on the sides spin while it rolls. Everything is positioned in the lower block's frame; the model files give the numbers.
 */
public class PrintingPressRenderer implements BlockEntityRenderer<PrintingPressBlockEntity> {
    public static final ModelResourceLocation HEAD = part("press_head"), ROLLER = part("press_roller");
    private static final float HEAD_TRAVEL = 5 / 16F; // px to blocks

    private final BlockEntityRendererProvider.Context context;

    public PrintingPressRenderer(BlockEntityRendererProvider.Context context) {
        this.context = context;
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "block/press/" + name));
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

        // the roller gears on the sides spin while the cube rolls: about X the east and west pair, about Z the north and south pair
        for (int side = 0; side < 4; side++) {
            boolean spins = anim.rolling() != null && (anim.rolling().axis() == 'X') == (side % 2 == 1);
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(90 * side));
            pose.translate(-0.5, 0, -0.5);
            pose.translate(0.5, 0.875, 0);
            if (spins) pose.mulPose(Axis.ZP.rotationDegrees(anim.angle()));
            pose.translate(-0.5, -0.875, 0);
            draw(pose, consumer, ROLLER, light, overlay);
            pose.popPose();
        }

        ItemStack cube = press.inventory().getStackInSlot(PrintingPressBlockEntity.CUBE);
        if (!cube.isEmpty()) {
            pose.pushPose();
            pose.translate(0.5, 0.75 + anim.lift() * 2 / 16F, 0.5); // an 8 px cube standing on the anvil, y 8 to 16 px, lifted 2 px to roll
            // every roll is about a world axis through the cube's centre, on top of the ones before: the newest is applied outermost
            if (anim.rolling() != null) pose.mulPose(axis(anim.rolling().axis()).rotationDegrees(anim.angle()));
            for (int i = anim.done() - 1; i >= 0; i--) pose.mulPose(axis(PressAnimation.ROLLS.get(i).axis()).rotationDegrees(PressAnimation.ROLLS.get(i).degrees()));
            context.getItemRenderer().renderStatic(cube, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 0);
            pose.popPose();
        }
    }

    private static Axis axis(char axis) {
        return axis == 'X' ? Axis.XP : Axis.ZP;
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
