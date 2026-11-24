package com.mamechin.enchantmod.blocks.altar.client;

import java.util.List;
import java.util.function.Function;

import com.mamechin.enchantmod.blocks.altar.AltarBlockEntity;
import com.mamechin.enchantmod.blocks.altar.AltarSpin;
import com.mamechin.enchantmod.multiblock.wheel.WheelCoreBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * The power of the enchant charging at the altar (author, 2026-11-23; white with a gold shade). From every book feeding it, a ribbon of
 * enchanting-table glyphs flows through the air in an arc, twisting and swaying, to the nearest point of the spinning wheel's ring (tier 1 and 2) or straight
 * to the item (tier 0). The wheel spins it into three twisted threads that run to the item, and
 * round the item the power swirls as a vortex (author, 2026-11-24, from a reference picture): strands spiral in from
 * a wide band at the item's height and stretch up and down along the axis, round a few tight core helices. Glyphs are vanilla's {@code sga_*} particle sprites drawn camera-facing; the ribbon's sheen, the threads and
 * the coil are camera-facing strips in vanilla's additive lightning render type. Each enchant (author, 2026-11-24): the ribbon flies out of
 * the shelf, its head racing to the wheel; once the enchant is fixed its tail leaves the shelf and is drawn into the wheel while the next
 * ribbon already flies out.
 */
public final class AltarRibbons {
    private static final float[] GLYPH = {1.0F, 1.0F, 0.97F}, SHEEN = {1.0F, 0.98F, 0.93F}, GOLD = {1.0F, 0.88F, 0.6F}, CORE = {1.0F, 0.99F, 0.96F};
    private static final float RIBBON_WIDTH = 0.24F, GLYPH_SIZE = 0.085F, FLOW = 0.09F; // blocks per tick along a ribbon
    private static final int LANES = 3;
    /** Where the threads from the wheel end (author, 2026-11-24): the flyer's spindle, the middle of its socket (AltarRenderer, y 12 to 20 px). */
    private static final Vec3 SPINDLE = new Vec3(0.5, 1.0, 0.5);
    /** How far from the hub the ribbons meet the wheel: its ring of frame blocks, 3 out (WheelLayout.RADIUS), nearer side. */
    private static final double RIM = 2.8;
    /** Ticks for a ribbon's head to reach the target and for the threads to reach the item; the tail's ticks are the altar's (it times the glyph burst). */
    private static final float RIBBON_OUT = 40, THREAD_OUT = 12, RIBBON_IN = AltarBlockEntity.TRAIL_TICKS, THREAD_IN = AltarBlockEntity.THREAD_TICKS;

    private AltarRibbons() {}

    /**
     * Draws the ribbons, threads and coil; {@code pose} is at the altar block's corner. No pause between enchants (author, 2026-11-24):
     * a ribbon stays joined to its shelf until its enchant is fixed, and its tail is drawn into the wheel early in the next phase while
     * the next ribbon flies out. The threads and the vortex stay on between enchants and drain and fade only in the cool-down.
     */
    public static void draw(AltarBlockEntity altar, float partialTick, PoseStack pose, MultiBufferSource buffer) {
        int phase = altar.phase();
        if ((phase != AltarSpin.CHARGE && phase != AltarSpin.COOLDOWN) || altar.getLevel() == null) return;
        boolean charging = phase == AltarSpin.CHARGE;
        float t = altar.getLevel().getGameTime() + partialTick;
        float into = altar.phaseTime(partialTick);
        BlockPos origin = altar.getBlockPos();
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 cam = camera.getPosition().subtract(Vec3.atLowerCornerOf(origin));
        Vector3f left = camera.getLeftVector(), up = camera.getUpVector();
        Vec3 item = new Vec3(0.5, 1.625, 0.5);
        Vec3 hub = hub(altar);
        Vec3 target = hub != null ? hub : item.add(0, 0.7, 0);
        List<Vec3> trailSources = altar.trailSources();
        boolean trail = !trailSources.isEmpty();
        // the cool-down can be shorter than the trail: squeeze the trail into it so the last drain is not cut off
        float threadSpan = hub != null ? THREAD_IN : 0, squeeze = charging ? 1 : Math.min(1, altar.phaseLength() / (RIBBON_IN + threadSpan));
        float ribbonIn = RIBBON_IN * squeeze, threadIn = threadSpan * squeeze, arrive = RIBBON_OUT + (hub != null ? THREAD_OUT : 0);
        // threads: growing to the item for the first enchant, held on between enchants, drained into the item in the cool-down
        float threadHead = !trail && charging ? ramp(into, RIBBON_OUT, THREAD_OUT) : trail || charging ? 1 : 0;
        float threadTail = charging ? 0 : ramp(into, ribbonIn, Math.max(threadIn, 1));
        float shown = charging ? (trail ? 1 : ramp(into, arrive, 8)) : trail ? 1 - ramp(into, ribbonIn + threadIn - 8, 8) : 0; // the vortex

        // particle atlas is owned by ParticleEngine, not ModelManager, so getTextureAtlas() throws for it
        Function<ResourceLocation, TextureAtlasSprite> atlas = ((TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES))::getSprite;
        VertexConsumer glyphs = buffer.getBuffer(RenderType.entityTranslucentEmissive(TextureAtlas.LOCATION_PARTICLES));
        PoseStack.Pose last = pose.last();

        List<Vec3[]> ribbons = new java.util.ArrayList<>();
        // each enchant's ribbons take their own shape, so one book feeding two enchants in a row does not look like one unbroken ribbon
        int seed = altar.enchantIndex() * 5;
        if (charging) {
            float head = 1 - sq(1 - ramp(into, 0, RIBBON_OUT)); // the head flies out of the shelf
            List<Vec3> sources = altar.feedSources();
            for (int r = 0; r < sources.size(); r++) ribbon(altar, sources.get(r), hub, target, t, r + seed, 0, head, ribbons, glyphs, last, atlas, left, up);
        }
        if (trail && into < ribbonIn) {
            float tail = sq(ramp(into, 0, ribbonIn)); // the last enchant's tail, drawn into the wheel
            for (int r = 0; r < trailSources.size(); r++) ribbon(altar, trailSources.get(r), hub, target, t, r + seed - 5, tail, 1, ribbons, glyphs, last, atlas, left, up);
        }
        Vec3[][] threads = hub != null && threadHead > threadTail ? threads(hub.add(0, 0.4, 0), SPINDLE, t) : new Vec3[0][];
        for (Vec3[] thread : threads) flowGlyphs(glyphs, last, atlas, thread, t, 0.8F, threadTail, threadHead, GLYPH_SIZE * 0.6F, left, up, 11);
        Vec3[][] coil = shown > 0 ? coil(item, altar.angle(partialTick)) : new Vec3[0][];
        for (int k = 0; k < coil.length; k += 2) flowGlyphs(glyphs, last, atlas, coil[k], t, shown * 0.7F, 0, 1, GLYPH_SIZE * 0.5F, left, up, 23 + k);

        VertexConsumer glow = buffer.getBuffer(RenderType.lightning());
        for (Vec3[] ribbon : ribbons) strip(glow, last, ribbon, cam, RIBBON_WIDTH * 1.3F, SHEEN, 0.16F);
        for (Vec3[] thread : threads) {
            Vec3[] part = slice(thread, threadTail, threadHead);
            strip(glow, last, part, cam, 0.12F, GOLD, 0.22F);
            strip(glow, last, part, cam, 0.035F, CORE, 0.8F);
        }
        for (int k = 0; k < coil.length; k++) {
            boolean core = k >= VORTEX_STRANDS;
            strip(glow, last, coil[k], cam, core ? 0.07F : 0.05F, GOLD, (core ? 0.25F : 0.15F) * shown);
            strip(glow, last, coil[k], cam, core ? 0.025F : 0.018F, CORE, (core ? 0.8F : 0.55F) * shown);
        }
    }

    /** One ribbon from the book at {@code source} (world position) to the wheel's ring, or to {@code target} without a wheel; its glyphs now, its sheen into {@code out}. */
    private static void ribbon(AltarBlockEntity altar, Vec3 source, Vec3 hub, Vec3 target, float t, int index, float tail, float head, List<Vec3[]> out,
            VertexConsumer glyphs, PoseStack.Pose pose, Function<ResourceLocation, TextureAtlasSprite> atlas, Vector3f left, Vector3f up) {
        Vec3 from = source.subtract(Vec3.atLowerCornerOf(altar.getBlockPos()));
        Vec3 to = hub != null ? rim(altar, hub, from) : target;
        out.add(slice(ribbonCentre(from, to, t, index), tail, head));
        ribbonGlyphs(glyphs, pose, atlas, from, to, t, index, tail, head, left, up);
    }

    /** 0 before {@code start}, rising to 1 over {@code span} ticks. */
    private static float ramp(float into, float start, float span) {
        return Mth.clamp((into - start) / span, 0, 1);
    }

    private static float sq(float x) {
        return x * x;
    }

    /** The part of a polyline between {@code from} and {@code to} (0 to 1 along its points), resampled to as many points. */
    private static Vec3[] slice(Vec3[] line, float from, float to) {
        if (to <= from) return new Vec3[0];
        Vec3[] out = new Vec3[line.length];
        for (int i = 0; i < out.length; i++) out[i] = at(line, Mth.lerp(i / (float) (out.length - 1), from, to));
        return out;
    }

    private static Vec3 at(Vec3[] line, float s) {
        float f = s * (line.length - 1);
        int k = Math.min(line.length - 2, (int) f);
        return line[k].lerp(line[k + 1], f - k);
    }

    /** Fades a glyph at {@code s} near the ends of the visible part [{@code tail}, {@code head}] and of the whole line. */
    private static float window(float s, float tail, float head) {
        return Mth.clamp(Math.min(s - tail, head - s) / 0.05F, 0, 1) * endFade(s);
    }

    /** The wheel's hub, raised with the spin, relative to the altar's corner; null without a formed wheel. */
    private static Vec3 hub(AltarBlockEntity altar) {
        BlockPos wheel = altar.wheel();
        if (wheel == null || altar.getLevel() == null) return null;
        BlockState core = altar.getLevel().getBlockState(wheel);
        if (!(core.getBlock() instanceof WheelCoreBlock) || !core.getValue(WheelCoreBlock.FORMED)) return null;
        return Vec3.atLowerCornerOf(wheel.subtract(altar.getBlockPos())).add(0.5, 0.5 + (-0.5F + altar.speed(0)), 0.5);
    }

    /** The point of the wheel's ring nearest {@code from} (author, 2026-11-24: ribbons meet the wheel's edge, not its hub). */
    private static Vec3 rim(AltarBlockEntity altar, Vec3 hub, Vec3 from) {
        Direction facing = altar.getLevel().getBlockState(altar.wheel()).getValue(WheelCoreBlock.FACING);
        Vec3 d = from.subtract(hub);
        // the ring's plane is at right angles to the core's facing: drop the part of d along it
        d = facing.getAxis() == Direction.Axis.X ? new Vec3(0, d.y, d.z) : new Vec3(d.x, d.y, 0);
        return hub.add(d.lengthSqr() < 1e-6 ? new Vec3(0, RIM, 0) : d.normalize().scale(RIM));
    }

    /** A point on the ribbon's arc at {@code s} (0 at the book, 1 at the target), swaying slowly. */
    private static Vec3 arc(Vec3 from, Vec3 to, float s, float t, int index) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        Vec3 side = new Vec3(-d.z, 0, d.x);
        side = side.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : side.normalize();
        float sway = 0.8F * Mth.sin(t * 0.07F + index * 1.7F), sway2 = 0.7F * Mth.sin(t * 0.05F + index * 2.3F + 1);
        float lift = 0.3F * Mth.sin(t * 0.06F + index * 0.9F), lift2 = 0.3F * Mth.sin(t * 0.045F + index * 1.4F + 2);
        Vec3 p1 = from.add(d.scale(0.3)).add(0, 0.15 * len + 0.5 + lift, 0).add(side.scale(sway));
        Vec3 p2 = from.add(d.scale(0.7)).add(0, 0.12 * len + 0.4 + lift2, 0).add(side.scale(-sway2));
        float u = 1 - s;
        Vec3 curve = from.scale(u * u * u).add(p1.scale(3 * u * u * s)).add(p2.scale(3 * u * s * s)).add(to.scale(s * s * s));
        // a wild, snaking wiggle on top of the arc, zero at both ends so the ribbon still leaves the book and meets the target
        float envelope = Mth.sin(s * Mth.PI) * (float) Math.min(1, len / 4);
        float wiggle = 0.45F * Mth.sin(s * Mth.TWO_PI * 1.7F + t * 0.11F + index * 1.3F) + 0.2F * Mth.sin(s * Mth.TWO_PI * 3.1F - t * 0.17F + index);
        float bob = 0.35F * Mth.sin(s * Mth.TWO_PI * 1.2F - t * 0.08F + index * 2.1F);
        return curve.add(side.scale(wiggle * envelope)).add(0, bob * envelope, 0);
    }

    private static Vec3[] ribbonCentre(Vec3 from, Vec3 to, float t, int index) {
        Vec3[] points = new Vec3[25];
        for (int i = 0; i < points.length; i++) points[i] = arc(from, to, i / (float) (points.length - 1), t, index);
        return points;
    }

    /** Glyphs in {@link #LANES} lanes across a band that twists along the arc, flowing from the book to the target. */
    private static void ribbonGlyphs(VertexConsumer consumer, PoseStack.Pose pose, Function<ResourceLocation, TextureAtlasSprite> atlas, Vec3 from, Vec3 to,
            float t, int index, float tail, float head, Vector3f left, Vector3f up) {
        if (head <= tail) return;
        double len = Math.max(1, to.subtract(from).length() * 1.2);
        int count = (int) Math.ceil(len * 5);
        for (int lane = 0; lane < LANES; lane++) {
            float offset = (lane - (LANES - 1) / 2F) * RIBBON_WIDTH / (LANES - 1);
            for (int i = 0; i < count; i++) {
                float s = frac(i / (float) count + t * FLOW / (float) len + lane * 0.37F / count);
                if (s < tail || s > head) continue;
                Vec3 p = arc(from, to, s, t, index), ahead = arc(from, to, Math.min(1, s + 0.01F), t, index);
                Vec3 tangent = ahead.subtract(p);
                if (tangent.lengthSqr() < 1e-9) tangent = to.subtract(from);
                Vec3 n = tangent.cross(new Vec3(0, 1, 0));
                n = n.lengthSqr() < 1e-9 ? new Vec3(1, 0, 0) : n.normalize();
                Vec3 b = n.cross(tangent.normalize());
                float twist = s * Mth.TWO_PI * 1.5F + t * 0.12F + index;
                Vec3 at = p.add(n.scale(Mth.cos(twist) * offset)).add(b.scale(Mth.sin(twist) * offset));
                float width = Mth.sqrt(Mth.sin(s * Mth.PI)); // the band narrows to a point at both ends
                glyph(consumer, pose, atlas, at, GLYPH_SIZE * (0.6F + 0.4F * width), window(s, tail, head), left, up, i * 7 + lane * 3 + index * 5);
            }
        }
    }

    /** Glyphs flowing along a polyline from its first point to its last, only between {@code tail} and {@code head}. */
    private static void flowGlyphs(VertexConsumer consumer, PoseStack.Pose pose, Function<ResourceLocation, TextureAtlasSprite> atlas, Vec3[] line, float t,
            float shown, float tail, float head, float size, Vector3f left, Vector3f up, int salt) {
        int count = line.length / 2;
        for (int i = 0; i < count; i++) {
            float s = frac(i / (float) count + t * 0.03F);
            if (s < tail || s > head) continue;
            glyph(consumer, pose, atlas, at(line, s), size, window(s, tail, head) * shown, left, up, i * 5 + salt);
        }
    }

    /** Three strands twisted round the straight line from the wheel's hub to above the item, turning as they flow. */
    private static Vec3[][] threads(Vec3 from, Vec3 to, float t) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 n = axis.cross(new Vec3(0, 1, 0));
        n = n.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : n.normalize();
        Vec3 b = n.cross(axis);
        Vec3[][] strands = new Vec3[3][33];
        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < 33; i++) {
                float s = i / 32F, r = 0.04F + 0.05F * Mth.sin(s * Mth.PI);
                float a = s * Mth.TWO_PI * 4 - t * 0.35F + k * Mth.TWO_PI / 3;
                Vec3 base = from.lerp(to, s);
                strands[k][i] = base.add(n.scale(Mth.cos(a) * r)).add(b.scale(Mth.sin(a) * r));
            }
        }
        return strands;
    }

    private static final int VORTEX_STRANDS = 10, CORE_STRANDS = 3;

    /**
     * The vortex round the item: {@link #VORTEX_STRANDS} strands start wide at about the item's height and spiral in, half rising and
     * half sinking, stretching faster along the axis as they tighten; {@link #CORE_STRANDS} tight helices run up the axis. All turn with
     * the altar's flyer; the core turns faster.
     */
    private static Vec3[][] coil(Vec3 item, float angleDegrees) {
        float base = angleDegrees * Mth.DEG_TO_RAD;
        Vec3[][] strands = new Vec3[VORTEX_STRANDS + CORE_STRANDS][];
        for (int k = 0; k < VORTEX_STRANDS; k++) {
            float r0 = 0.75F + 0.35F * frac(k * 0.618F), y0 = 0.15F * (frac(k * 0.382F) - 0.5F), dir = k % 2 == 0 ? 1 : -1;
            float phase = k * Mth.TWO_PI / VORTEX_STRANDS, turns = 2.2F + frac(k * 0.73F);
            Vec3[] line = new Vec3[48];
            for (int i = 0; i < line.length; i++) {
                float s = i / (float) (line.length - 1), r = 0.07F + (r0 - 0.07F) * (1 - s) * (1 - s);
                float a = base + phase + s * Mth.TWO_PI * turns;
                line[i] = item.add(Mth.cos(a) * r, y0 + dir * s * s * (dir > 0 ? 1.0F : 0.75F), Mth.sin(a) * r);
            }
            strands[k] = line;
        }
        for (int k = 0; k < CORE_STRANDS; k++) {
            Vec3[] line = new Vec3[40];
            for (int i = 0; i < line.length; i++) {
                float s = i / (float) (line.length - 1), a = base * 2 + s * Mth.TWO_PI * 5 + k * Mth.TWO_PI / CORE_STRANDS;
                line[i] = item.add(Mth.cos(a) * 0.09F, -0.75F + 1.75F * s, Mth.sin(a) * 0.09F);
            }
            strands[VORTEX_STRANDS + k] = line;
        }
        return strands;
    }

    private static void glyph(VertexConsumer consumer, PoseStack.Pose pose, Function<ResourceLocation, TextureAtlasSprite> atlas, Vec3 at, float size,
            float alpha, Vector3f left, Vector3f up, int pick) {
        if (alpha <= 0.02F) return;
        TextureAtlasSprite sprite = atlas.apply(ResourceLocation.withDefaultNamespace("sga_" + (char) ('a' + Math.floorMod(pick, 26))));
        float lx = left.x() * size, ly = left.y() * size, lz = left.z() * size, ux = up.x() * size, uy = up.y() * size, uz = up.z() * size;
        float x = (float) at.x, y = (float) at.y, z = (float) at.z;
        vertex(consumer, pose, x + lx - ux, y + ly - uy, z + lz - uz, sprite.getU0(), sprite.getV1(), alpha);
        vertex(consumer, pose, x - lx - ux, y - ly - uy, z - lz - uz, sprite.getU1(), sprite.getV1(), alpha);
        vertex(consumer, pose, x - lx + ux, y - ly + uy, z - lz + uz, sprite.getU1(), sprite.getV0(), alpha);
        vertex(consumer, pose, x + lx + ux, y + ly + uy, z + lz + uz, sprite.getU0(), sprite.getV0(), alpha);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, float x, float y, float z, float u, float v, float alpha) {
        consumer.addVertex(pose, x, y, z).setColor(GLYPH[0], GLYPH[1], GLYPH[2], alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 1, 0);
    }

    /** A polyline as camera-facing quads {@code width} wide, both windings (the lightning type culls back faces). */
    private static void strip(VertexConsumer consumer, PoseStack.Pose pose, Vec3[] points, Vec3 cam, float width, float[] rgb, float alpha) {
        if (alpha <= 0.01F || points.length < 2) return;
        for (int i = 0; i + 1 < points.length; i++) {
            Vec3 p = points[i], q = points[i + 1];
            Vec3 side = q.subtract(p).cross(cam.subtract(p.add(q).scale(0.5)));
            if (side.lengthSqr() < 1e-10) continue;
            side = side.normalize().scale(width / 2);
            float a = alpha * endFade(i / (float) (points.length - 1));
            Vec3[] quad = {p.add(side), p.subtract(side), q.subtract(side), q.add(side)};
            for (int v = 0; v < 4; v++) consumer.addVertex(pose, (float) quad[v].x, (float) quad[v].y, (float) quad[v].z).setColor(rgb[0], rgb[1], rgb[2], a);
            for (int v = 3; v >= 0; v--) consumer.addVertex(pose, (float) quad[v].x, (float) quad[v].y, (float) quad[v].z).setColor(rgb[0], rgb[1], rgb[2], a);
        }
    }

    private static float endFade(float s) {
        return Mth.clamp(Math.min(s, 1 - s) / 0.08F, 0, 1);
    }

    private static float frac(float x) {
        return x - Mth.floor(x);
    }
}
