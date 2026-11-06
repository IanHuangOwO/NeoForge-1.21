package com.mamechin.enchantmod.cauldron.client;

import java.util.ArrayList;
import java.util.List;

import com.mamechin.enchantmod.EnchantMod;
import com.mamechin.enchantmod.cauldron.CalciteCauldronBlock;
import com.mamechin.enchantmod.cauldron.CalciteCauldronBlockEntity;
import com.mamechin.enchantmod.cauldron.CauldronContent;
import com.mamechin.enchantmod.cauldron.brewing.CauldronBrewingRecipe;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Stored ingredients hover in the block above the cauldron, one entry per item type with a count at its
 * bottom-right, bobbing mostly up/down with a slight sideways wobble. When a brew starts they rise and
 * dive into the liquid one after another.
 */
public class CalciteCauldronRenderer implements BlockEntityRenderer<CalciteCauldronBlockEntity> {
    private static final float ITEM_SCALE = 0.45F;
    private static final float HOVER_HEIGHT = 1.5F;  // centre of the block above the cauldron
    private static final float MAX_DRIFT_XZ = 0.25F; // furthest the item strays sideways from its anchor
    private static final float MAX_DRIFT_Y = 0.5F;   // furthest it strays up or down
    private static final float SPACING = 0.5F;
    private static final float TEXT_SCALE = 0.016F;

    private static final float RISE_TICKS = 8.0F;    // lift off
    private static final float DIVE_TICKS = 14.0F;   // fall into the liquid
    private static final float STAGGER_TICKS = 3.0F; // delay between item types

    private final ItemRenderer itemRenderer;
    private final Font font;

    public CalciteCauldronRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.font = context.getFont();
    }

    @Override
    public void render(CalciteCauldronBlockEntity cauldron, float partialTick, PoseStack pose,
            MultiBufferSource buffer, int ignoredLight, int overlay) {
        var level = cauldron.getLevel();
        if (level == null) return;

        float time = level.getGameTime() + partialTick;
        boolean brewing = cauldron.getBlockState().getValue(CalciteCauldronBlock.CONTENT) == CauldronContent.BREWING;
        // Client-only animation clock: starts when we first see the BREWING state, resets when it ends.
        if (!brewing) {
            cauldron.brewAnimStart = -1;
        } else if (cauldron.brewAnimStart < 0) {
            cauldron.brewAnimStart = time - cauldron.getProgress(); // joins a running brew at the right point
        }

        if (brewing) renderBrewSurface(cauldron, level, pose, buffer, ignoredLight, time);

        // One entry per item type, counts summed, in first-seen slot order.
        List<ItemStack> types = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (int i = 0; i < cauldron.inventory.getSlots(); i++) {
            ItemStack stack = cauldron.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            int index = -1;
            for (int j = 0; j < types.size(); j++) {
                if (ItemStack.isSameItemSameComponents(types.get(j), stack)) index = j;
            }
            if (index < 0) {
                types.add(stack.copyWithCount(1));
                counts.add(stack.getCount());
            } else {
                counts.set(index, counts.get(index) + stack.getCount());
            }
        }
        if (types.isEmpty()) return;

        int light = LevelRenderer.getLightColor(level, cauldron.getBlockPos().above());
        float camYaw = Minecraft.getInstance().gameRenderer.getMainCamera().getYRot();
        int lvl = cauldron.getBlockState().getValue(CalciteCauldronBlock.LEVEL);
        float surface = (lvl == 0 ? 4.0F : 6.0F + 3.0F * lvl) / 16.0F;
        float spacing = types.size() == 1 ? 0 : Math.min(SPACING, 0.9F / (types.size() - 1));

        for (int i = 0; i < types.size(); i++) {
            // Drift: three sines of unrelated frequencies per axis give a smooth, non-repeating wander,
            // bounded by MAX_DRIFT_XZ / MAX_DRIFT_Y around the anchor. Seeded per item and per cauldron.
            float seed = i * 1.7F + (cauldron.getBlockPos().hashCode() % 1000) * 0.37F;
            float dx = MAX_DRIFT_XZ * wander(time, seed, 0.031F, 0.047F, 0.019F);
            float dz = MAX_DRIFT_XZ * wander(time, seed + 11.0F, 0.037F, 0.023F, 0.053F);
            float dy = MAX_DRIFT_Y * wander(time, seed + 23.0F, 0.043F, 0.029F, 0.061F);
            float x = 0.5F + dx;
            float z = 0.5F + dz;
            float y = HOVER_HEIGHT + dy;
            float local = (i - (types.size() - 1) / 2.0F) * spacing;          // along the viewer's right
            float scale = 1.0F;
            boolean showCount = true;

            if (brewing) {
                float t = time - cauldron.brewAnimStart - i * STAGGER_TICKS;
                if (t >= RISE_TICKS + DIVE_TICKS) continue; // already swallowed
                if (t >= 0) {
                    showCount = false;
                    if (t < RISE_TICKS) {
                        float u = t / RISE_TICKS;
                        y += 0.35F * (1 - (1 - u) * (1 - u)); // ease out
                    } else {
                        float u = (t - RISE_TICKS) / DIVE_TICKS;
                        float e = u * u; // ease in
                        float top = y + 0.35F;
                        y = Mth.lerp(e, top, surface);
                        x = 0.5F + dx * (1 - e); // pull back over the liquid so the dive lands inside the cauldron
                        z = 0.5F + dz * (1 - e);
                        local = Mth.lerp(e, local, 0.0F);
                        scale = 1.0F - 0.7F * e;
                    }
                }
            }

            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(180.0F - camYaw)); // face the camera
            pose.translate(local, 0, 0);

            pose.pushPose();
            pose.scale(ITEM_SCALE * scale, ITEM_SCALE * scale, ITEM_SCALE * scale);
            itemRenderer.renderStatic(types.get(i), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY,
                    pose, buffer, level, i + (int) cauldron.getBlockPos().asLong());
            pose.popPose();

            int count = counts.get(i);
            if (count > 1 && showCount) {
                String text = Integer.toString(count);
                float half = ITEM_SCALE * 0.5F / TEXT_SCALE; // item half-size in text pixels
                pose.translate(0, 0, 0.03F);
                pose.scale(TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);
                font.drawInBatch(text, half - font.width(text), half - 8, 0xFFFFFF, true, pose.last().pose(), buffer,
                        Font.DisplayMode.NORMAL, 0, light);
            }
            pose.popPose();
        }
    }

    /** While brewing, a translucent tinted layer (it replaces the liquid model) on the liquid fades from the base fluid's colour to the result's (colours: CauldronColors). */
    private void renderBrewSurface(CalciteCauldronBlockEntity cauldron, net.minecraft.world.level.Level level,
            PoseStack pose, MultiBufferSource buffer, int light, float time) {
        var found = cauldron.getActiveRecipeId() == null ? java.util.Optional.<net.minecraft.world.item.crafting.RecipeHolder<?>>empty()
                : level.getRecipeManager().byKey(cauldron.getActiveRecipeId());
        if (found.isEmpty() || !(found.get().value() instanceof CauldronBrewingRecipe recipe)) return;

        float t = Mth.clamp((time - cauldron.brewAnimStart) / recipe.brewTicks(), 0.0F, 1.0F);
        int from = CauldronColors.of(recipe.baseFluid());
        int to = CauldronColors.of(recipe.resultFluid());
        int r = (int) Mth.lerp(t, from >> 16 & 255, to >> 16 & 255);
        int g = (int) Mth.lerp(t, from >> 8 & 255, to >> 8 & 255);
        int b = (int) Mth.lerp(t, from & 255, to & 255);

        int lvl = cauldron.getBlockState().getValue(CalciteCauldronBlock.LEVEL);
        float y = (6.0F + 3.0F * lvl) / 16.0F + 0.002F; // just above the liquid model's top face
        float min = 2.0F / 16.0F, max = 14.0F / 16.0F;
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ResourceLocation.fromNamespaceAndPath(EnchantMod.MODID, "block/calcite_cauldron_liquid_mask"));
        VertexConsumer vc = buffer.getBuffer(RenderType.translucent()); // same pass and alpha as the liquid models
        Matrix4f m = pose.last().pose();
        float[][] corners = {{min, min, sprite.getU0(), sprite.getV0()}, {min, max, sprite.getU0(), sprite.getV1()},
                {max, max, sprite.getU1(), sprite.getV1()}, {max, min, sprite.getU1(), sprite.getV0()}};
        for (float[] c : corners) {
            vc.addVertex(m, c[0], y, c[1]).setColor(r, g, b, 255).setUv(c[2], c[3]).setLight(light).setNormal(pose.last(), 0, 1, 0);
        }
    }

    /** Smooth pseudo-random value in [-1, 1]: weighted sum of three sines with unrelated speeds. */
    private static float wander(float time, float seed, float f1, float f2, float f3) {
        return 0.5F * Mth.sin(time * f1 + seed) + 0.3F * Mth.sin(time * f2 + seed * 1.9F) + 0.2F * Mth.sin(time * f3 + seed * 3.1F);
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(CalciteCauldronBlockEntity be) {
        // items hover one block above the cauldron, outside the block's own bounding box
        return new net.minecraft.world.phys.AABB(be.getBlockPos()).expandTowards(0, 1.5, 0).inflate(1, 0, 1);
    }
}
