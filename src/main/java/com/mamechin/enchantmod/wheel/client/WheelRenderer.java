package com.mamechin.enchantmod.wheel.client;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.WheelCoreBlock;
import com.mamechin.enchantmod.altar.AltarBlockEntity;
import com.mamechin.enchantmod.altar.AltarSpin;
import com.mamechin.enchantmod.wheel.WheelCoreBlockEntity;
import com.mamechin.enchantmod.wheel.WheelLayout;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Draws a formed spinning wheel from its core: the wheel (Fable's {@code wheel_rotor}, authored facing north, axis through the core's
 * centre) and its two pillars, and the cube of the altar in front of it in the socket before the hub. While the altar enchants, the
 * wheel rises from resting (half a model block below the authored pose) to floating (half a block above) and spins with the altar's
 * speed, and the wheel's and pillars' glow layers are drawn at full brightness (dark carved runes otherwise).
 */
public class WheelRenderer implements BlockEntityRenderer<WheelCoreBlockEntity> {
    public static final ModelResourceLocation ROTOR = part("wheel_rotor"), ROTOR_GLOW = part("wheel_rotor_glow"), PILLAR = part("wheel_pillar"), PILLAR_GLOW = part("wheel_pillar_glow");
    private static final float RESTING_LIFT = -0.5F, RISE = 1; // in model blocks
    /** The models were drawn for a 9-wide wheel; they are scaled about the core's centre to fit the layout's ring (5/9 for the 5-wide ring). */
    private static final int MODEL_RADIUS = 4;
    private static final float SCALE = (WheelLayout.RADIUS + 0.5F) / (MODEL_RADIUS + 0.5F);

    private final ItemRenderer itemRenderer;

    public WheelRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "block/" + name));
    }

    @Override
    public void render(WheelCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffer, int ignoredLight, int overlay) {
        var level = core.getLevel();
        if (level == null) return;
        Direction facing = core.getBlockState().getValue(WheelCoreBlock.FACING);
        if (!core.getBlockState().getValue(WheelCoreBlock.FORMED)) return;
        AltarBlockEntity altar = level.getBlockEntity(WheelCoreBlock.altarPos(core.getBlockPos(), facing)) instanceof AltarBlockEntity a ? a : null;
        float speed = altar == null ? 0 : AltarSpin.speed(altar.elapsed(partialTick));
        float lift = RESTING_LIFT + RISE * speed, angle = altar == null ? 0 : altar.angle(partialTick);
        int light = LevelRenderer.getLightColor(level, core.getBlockPos().relative(facing));

        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot())); // the model faces north (-Z)
        pose.translate(-0.5, 0, -0.5);
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(SCALE, SCALE, SCALE);
        pose.translate(-0.5, -0.5, -0.5);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        // the pillars: crystal face +X in their own frame, so the east one is turned round
        for (int side : new int[] {-1, 1}) {
            pose.pushPose();
            pose.translate(side * (MODEL_RADIUS + 1), -MODEL_RADIUS, 0);
            if (side > 0) {
                pose.translate(0.5, 0, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(180));
                pose.translate(-0.5, 0, -0.5);
            }
            draw(pose, consumer, PILLAR, light, overlay);
            if (speed > 0) draw(pose, consumer, PILLAR_GLOW, LightTexture.FULL_BRIGHT, overlay);
            pose.popPose();
        }
        pose.translate(0, lift, 0);
        pose.translate(0.5, 0.5, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angle));
        pose.translate(-0.5, -0.5, 0);
        draw(pose, consumer, ROTOR, light, overlay);
        if (speed > 0) draw(pose, consumer, ROTOR_GLOW, LightTexture.FULL_BRIGHT, overlay);
        pose.popPose();
        ItemStack cube = altar == null ? ItemStack.EMPTY : altar.inventory().getStackInSlot(AltarBlockEntity.CUBE);
        if (!cube.isEmpty()) {
            // full size and upright, in front of the hub: the scaled wheel's front face is at z 0.5 - 0.5 * SCALE, an 8 px cube is 0.25 deep each way
            pose.translate(0.5, 0.5 + lift * SCALE, 0.5 - 0.5 * SCALE - 0.25);
            pose.scale(0.5F, 0.5F, 0.5F);
            itemRenderer.renderStatic(cube, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffer, level, 0);
        }
        pose.popPose();
    }

    private static void draw(PoseStack pose, VertexConsumer consumer, ModelResourceLocation location, int light, int overlay) {
        var model = Minecraft.getInstance().getModelManager().getModel(location);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(pose.last(), consumer, null, model, 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.cutout());
    }

    /** The wheel and its pillars span the layout's area round the core, in either orientation. */
    @Override
    public AABB getRenderBoundingBox(WheelCoreBlockEntity core) {
        int reach = WheelLayout.RADIUS + 1;
        return new AABB(core.getBlockPos()).inflate(reach, reach, reach);
    }
}
