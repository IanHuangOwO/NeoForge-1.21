package com.mamechin.enchantmod.blocks.cauldron.client;

/**
 * Client-side smoothed copy of what the cauldron liquid looks like: surface height, opacity and colour ease toward
 * their targets so state changes cross-fade instead of popping. Plain data, no client classes.
 */
public final class LiquidView {
    private static final float FADE_TICKS = 5.0F; // time constant: ~90% of the way after about 11 ticks

    public float top;       // surface height in blocks
    public float alpha;     // 0..1
    public float r, g, b;   // 0..255
    private float last = -1;
    private boolean started;

    public void update(float time, float targetTop, float targetAlpha, int rgb) {
        float tr = rgb >> 16 & 255, tg = rgb >> 8 & 255, tb = rgb & 255;
        if (!started) { // first sight (chunk load): show the final look at once
            top = targetTop; alpha = targetAlpha; r = tr; g = tg; b = tb;
            started = true;
            last = time;
            return;
        }
        float k = 1.0F - (float) Math.exp(-Math.max(time - last, 0.0F) / FADE_TICKS);
        last = time;
        if (alpha < 0.02F) { // invisible: reposition and recolour silently, then only fade in
            top = targetTop; r = tr; g = tg; b = tb;
        } else {
            top += (targetTop - top) * k; r += (tr - r) * k; g += (tg - g) * k; b += (tb - b) * k;
        }
        alpha += (targetAlpha - alpha) * k;
    }
}
