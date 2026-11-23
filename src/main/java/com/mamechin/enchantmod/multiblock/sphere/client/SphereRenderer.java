package com.mamechin.enchantmod.multiblock.sphere.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.blocks.altar.AltarBlockEntity;
import com.mamechin.enchantmod.multiblock.sphere.SphereCoreBlock;
import com.mamechin.enchantmod.multiblock.sphere.SpherePartBlock;
import com.mamechin.enchantmod.multiblock.client.GhostBlocks;
import com.mamechin.enchantmod.multiblock.sphere.SphereCoreBlockEntity;
import com.mamechin.enchantmod.multiblock.sphere.SphereLayout;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

/**
 * Draws a formed glass sphere from its core (Fable's pack, 2026-11-22, numbers from its README), or the build guide of an unformed one.
 * Always moving, on the core's animation clock (faster while the linked altar spins): the glass shell turns about the vertical once a
 * minute, the 38 gears on it ({@code assets/enchantmod/sphere/gears.json}) tick one tooth a second like clockwork, and the three broken
 * rings each spin, swing their tilt and precess. The glow layers are drawn at full brightness while the altar enchants. Drawn with the block
 * cutout shader and no back-face culling ({@link #CUTOUT_NO_CULL}) so the far panes and the heart show through the glass.
 */
public class SphereRenderer implements BlockEntityRenderer<SphereCoreBlockEntity> {
    public static final ModelResourceLocation SHELL = part("sphere_shell"), SHELL_GLOW = part("sphere_shell_glow");
    private static final String[] RINGS = {"sphere_ring_a", "sphere_ring_b", "sphere_ring_c"};
    private static final String[] GEARS = {"sphere_gear_large", "sphere_gear_medium", "sphere_gear_small"};
    private static final float[] RING_SPIN = {14, -22, 35}, RING_PRECESSION = {5, -8, 11}, RING_TILT = {20, 65, -45}, RING_SWING = {15, 25, 40}, RING_PERIOD = {23, 17, 13};
    private static final float SHELL_DEGREES_PER_SECOND = 6, GEAR_TOOTH = 0.5236F; // radians per tooth of the large gear

    /** Every model this renderer draws, for registering as additional models. */
    public static final List<ModelResourceLocation> MODELS = Stream.of(Stream.of("sphere_shell"), Stream.of(RINGS), Stream.of(GEARS))
            .flatMap(s -> s).flatMap(name -> Stream.of(part(name), part(name + "_glow"))).toList();

    /**
     * Block cutout like the wheel's (lit by the light level only), but without back-face culling so the far panes show through the
     * glass. The entity no-cull type shaded each face by its normal, and Fable's OBJs have none, so much of the sphere came out dark.
     */
    private static final RenderType CUTOUT_NO_CULL = RenderType.create(EnchantMod.MODID + ":sphere_cutout_no_cull", DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS, RenderType.SMALL_BUFFER_SIZE, true, false, RenderType.CompositeState.builder()
                    .setLightmapState(RenderStateShard.LIGHTMAP).setShaderState(RenderStateShard.RENDERTYPE_CUTOUT_SHADER)
                    .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED).setCullState(RenderStateShard.NO_CULL).createCompositeState(true));

    private record Gear(ModelResourceLocation model, ModelResourceLocation glow, float x, float y, float z, Quaternionf rotation, float phase, float ratio) {}

    private final List<Gear> gears = loadGears();

    public SphereRenderer(BlockEntityRendererProvider.Context context) {}

    private static ModelResourceLocation part(String name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "block/sphere/" + name));
    }

    /** Reads the gear placements; renderers are rebuilt on a resource reload, so this follows resource packs. */
    private static List<Gear> loadGears() {
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "sphere/gears.json");
        List<Gear> list = new ArrayList<>();
        try (Reader reader = Minecraft.getInstance().getResourceManager().openAsReader(file)) {
            for (JsonElement e : JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("gears")) {
                JsonObject g = e.getAsJsonObject();
                JsonArray pos = g.getAsJsonArray("pos"), q = g.getAsJsonArray("quat");
                String model = g.get("model").getAsString();
                list.add(new Gear(part(model), part(model + "_glow"), pos.get(0).getAsFloat(), pos.get(1).getAsFloat(), pos.get(2).getAsFloat(),
                        new Quaternionf(q.get(0).getAsFloat(), q.get(1).getAsFloat(), q.get(2).getAsFloat(), q.get(3).getAsFloat()),
                        g.get("phase").getAsFloat(), g.get("ratio").getAsFloat()));
            }
        } catch (Exception ex) {
            LogUtils.getLogger().warn("Glass sphere gears not loaded from {}: {}", file, ex.toString());
        }
        return List.copyOf(list);
    }

    @Override
    public void render(SphereCoreBlockEntity core, float partialTick, PoseStack pose, MultiBufferSource buffer, int ignoredLight, int overlay) {
        var level = core.getLevel();
        if (level == null) return;
        if (!core.getBlockState().getValue(SphereCoreBlock.FORMED)) {
            if (core.guide()) {
                List<GhostBlocks.Cell> cells = new ArrayList<>();
                for (boolean ring : new boolean[] {false, true}) {
                    for (BlockPos pos : SphereCoreBlock.parts(core.getBlockPos(), ring)) cells.add(new GhostBlocks.Cell(pos, SpherePartBlock.original(ring)));
                }
                GhostBlocks.draw(level, core.getBlockPos(), cells, pose, buffer);
            }
            return;
        }
        AltarBlockEntity altar = core.linkedAltar();
        boolean glow = altar != null && altar.busy();
        float t = core.seconds(partialTick);
        // the core sits inside the ball, so take the brighter of its cell and the cell just above the top ring
        int inside = LevelRenderer.getLightColor(level, core.getBlockPos()), above = LevelRenderer.getLightColor(level, core.getBlockPos().above(SphereLayout.RING_RADIUS + 1));
        int light = LightTexture.pack(Math.max(LightTexture.block(inside), LightTexture.block(above)), Math.max(LightTexture.sky(inside), LightTexture.sky(above)));
        VertexConsumer consumer = buffer.getBuffer(CUTOUT_NO_CULL);

        // the shell turns, and the gears ride on it
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(t * SHELL_DEGREES_PER_SECOND));
        pose.translate(-0.5, -0.5, -0.5);
        draw(pose, consumer, SHELL, light, glow ? SHELL_GLOW : null);
        float snap = Math.min(1, (t - Mth.floor(t)) * 4);
        float gearAngle = (Mth.floor(t) + snap * snap * (3 - 2 * snap)) * GEAR_TOOTH; // one tooth a second, eased
        for (Gear g : gears) {
            pose.pushPose();
            pose.translate(g.x(), g.y(), g.z());
            pose.mulPose(g.rotation());
            pose.mulPose(Axis.YP.rotation(g.phase() + g.ratio() * gearAngle));
            draw(pose, consumer, g.model(), light, glow ? g.glow() : null);
            pose.popPose();
        }
        pose.popPose();

        for (int i = 0; i < RINGS.length; i++) {
            float tilt = RING_TILT[i] + RING_SWING[i] * Mth.sin(t * Mth.TWO_PI / RING_PERIOD[i]);
            pose.pushPose();
            pose.translate(0.5, 0.5, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(120F * i + RING_PRECESSION[i] * t));
            pose.mulPose(Axis.XP.rotationDegrees(tilt));
            pose.mulPose(Axis.YP.rotationDegrees(RING_SPIN[i] * t));
            pose.translate(-0.5, -0.5, -0.5);
            draw(pose, consumer, part(RINGS[i]), light, glow ? part(RINGS[i] + "_glow") : null);
            pose.popPose();
        }
    }

    /** Draws a model, and its glow layer at full brightness when {@code glow} is given. */
    private static void draw(PoseStack pose, VertexConsumer consumer, ModelResourceLocation location, int light, ModelResourceLocation glow) {
        var renderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
        var models = Minecraft.getInstance().getModelManager();
        renderer.renderModel(pose.last(), consumer, null, models.getModel(location), 1, 1, 1, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
        if (glow != null) renderer.renderModel(pose.last(), consumer, null, models.getModel(glow), 1, 1, 1, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, RenderType.cutout());
    }

    /** Fable's models stay inside -5..+6 blocks round the core's corner in every pose. */
    @Override
    public AABB getRenderBoundingBox(SphereCoreBlockEntity core) {
        return new AABB(core.getBlockPos()).inflate(SphereLayout.RING_RADIUS);
    }
}
