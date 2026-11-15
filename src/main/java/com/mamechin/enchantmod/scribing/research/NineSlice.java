package com.mamechin.enchantmod.scribing.research;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws a square texture as a frame of any size: corners as they are, edges and middle tiled. Pass fillMiddle=false to leave the middle empty. */
final class NineSlice {
    private NineSlice() {}

    static void draw(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int size, int border, boolean fillMiddle) {
        int mid = size - 2 * border, right = x + w - border, bottom = y + h - border;
        g.blit(tex, x, y, 0, 0, border, border, size, size);
        g.blit(tex, right, y, size - border, 0, border, border, size, size);
        g.blit(tex, x, bottom, 0, size - border, border, border, size, size);
        g.blit(tex, right, bottom, size - border, size - border, border, border, size, size);
        tile(g, tex, x + border, y, w - 2 * border, border, border, 0, mid, border, size);
        tile(g, tex, x + border, bottom, w - 2 * border, border, border, size - border, mid, border, size);
        tile(g, tex, x, y + border, border, h - 2 * border, 0, border, border, mid, size);
        tile(g, tex, right, y + border, border, h - 2 * border, size - border, border, border, mid, size);
        if (fillMiddle) tile(g, tex, x + border, y + border, w - 2 * border, h - 2 * border, border, border, mid, mid, size);
    }

    /** Repeats the (rw x rh) piece of the texture at (u, v) over the area. */
    static void tile(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int u, int v, int rw, int rh, int size) {
        for (int ty = 0; ty < h; ty += rh) {
            for (int tx = 0; tx < w; tx += rw) g.blit(tex, x + tx, y + ty, u, v, Math.min(rw, w - tx), Math.min(rh, h - ty), size, size);
        }
    }
}
