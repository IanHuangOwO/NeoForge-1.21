package com.mamechin.enchantmod.multiblock.sphere.client;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.multiblock.sphere.BoltPath;
import com.mamechin.enchantmod.multiblock.sphere.LightningPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The refill lightning (author, 2026-11-23: purple-white, branched): each bolt lives {@link #LIFE} ticks, takes a new shape every two
 * ticks ({@link BoltPath} with a new seed) so it flickers, and fades out. Every segment is drawn as two camera-facing strips with
 * vanilla's additive lightning render type: a wide faint violet glow and a thin bright white-violet core. Sparks burst at both ends.
 */
@EventBusSubscriber(modid = EnchantMod.MODID, value = Dist.CLIENT)
public final class SphereBolts {
    static final int LIFE = 10;
    private static final float GLOW_WIDTH = 0.22F, CORE_WIDTH = 0.05F, BRANCH_WIDTH = 0.6F;
    private static final float[] GLOW = {0.55F, 0.25F, 1.0F, 0.35F}, CORE = {0.95F, 0.88F, 1.0F, 0.95F};

    private record Bolt(Vec3 from, Vec3 to, long seed, int[] age) {}

    private static final List<Bolt> BOLTS = new ArrayList<>();

    private SphereBolts() {}

    /** A bolt arrived from the server. */
    public static void add(LightningPayload payload) {
        BOLTS.add(new Bolt(payload.from(), payload.to(), payload.seed(), new int[] {0}));
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (int i = 0; i < 10; i++) {
            Vec3 at = i < 7 ? payload.to() : payload.from();
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, (level.random.nextDouble() - 0.5) * 0.3, level.random.nextDouble() * 0.2, (level.random.nextDouble() - 0.5) * 0.3);
        }
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level == null) BOLTS.clear();
        BOLTS.removeIf(bolt -> ++bolt.age()[0] >= LIFE);
    }

    @SubscribeEvent
    static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || BOLTS.isEmpty()) return;
        Vec3 cam = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = pose.last().pose();
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        for (Bolt bolt : BOLTS) {
            float life = (bolt.age()[0] + partial) / LIFE, fade = Math.max(0, 1 - life * life);
            float strobe = bolt.age()[0] % 2 == 0 ? 1 : 0.7F;
            List<List<float[]>> lines = BoltPath.build(point(bolt.from()), point(bolt.to()), bolt.seed() + bolt.age()[0] / 2);
            for (int l = 0; l < lines.size(); l++) {
                float width = (l == 0 ? 1 : BRANCH_WIDTH) * (0.6F + 0.4F * fade);
                strip(consumer, matrix, lines.get(l), cam, GLOW_WIDTH * width, GLOW, fade * strobe);
                strip(consumer, matrix, lines.get(l), cam, CORE_WIDTH * width, CORE, fade * strobe);
            }
        }
        buffers.endBatch(RenderType.lightning());
    }

    /** One polyline as quads that face the camera, {@code width} wide, both windings (the render type culls back faces). Points are made relative to the camera. */
    private static void strip(VertexConsumer consumer, Matrix4f matrix, List<float[]> points, Vec3 cam, float width, float[] rgba, float alpha) {
        float a = rgba[3] * alpha;
        if (a <= 0.01F) return;
        for (int i = 0; i + 1 < points.size(); i++) {
            float[] p = points.get(i), q = points.get(i + 1);
            Vector3f along = new Vector3f(q[0] - p[0], q[1] - p[1], q[2] - p[2]);
            Vector3f toCam = new Vector3f((float) cam.x - (p[0] + q[0]) / 2, (float) cam.y - (p[1] + q[1]) / 2, (float) cam.z - (p[2] + q[2]) / 2);
            Vector3f side = along.cross(toCam);
            if (side.lengthSquared() < 1e-8F) continue;
            side.normalize(width / 2);
            float px = (float) (p[0] - cam.x), py = (float) (p[1] - cam.y), pz = (float) (p[2] - cam.z);
            float qx = (float) (q[0] - cam.x), qy = (float) (q[1] - cam.y), qz = (float) (q[2] - cam.z);
            float[][] quad = {{px + side.x, py + side.y, pz + side.z}, {px - side.x, py - side.y, pz - side.z},
                    {qx - side.x, qy - side.y, qz - side.z}, {qx + side.x, qy + side.y, qz + side.z}};
            for (int v = 0; v < 4; v++) consumer.addVertex(matrix, quad[v][0], quad[v][1], quad[v][2]).setColor(rgba[0], rgba[1], rgba[2], a);
            for (int v = 3; v >= 0; v--) consumer.addVertex(matrix, quad[v][0], quad[v][1], quad[v][2]).setColor(rgba[0], rgba[1], rgba[2], a);
        }
    }

    private static float[] point(Vec3 v) {
        return new float[] {(float) v.x, (float) v.y, (float) v.z};
    }
}
