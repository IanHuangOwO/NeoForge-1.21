package com.mamechin.enchantmod.scribing.rules;

/**
 * What one kind of enchant template allows, from data/&lt;namespace&gt;/enchantmod/template/&lt;item&gt;.json (the file name is the
 * item's id). The drawing area is centred on the grid; maxLevel caps the level the template can give.
 *
 * @param item     the template item's id, "namespace:path"
 * @param width    columns of the grid that can be drawn on
 * @param height   rows of the grid that can be drawn on
 * @param maxLevel highest level the forecast may give, Integer.MAX_VALUE for no cap
 */
public record TemplateTier(String item, int width, int height, int maxLevel) {
    /** The whole grid, no cap: used for anything without a file. */
    public static final TemplateTier FULL = new TemplateTier("", BlueprintRules.WIDTH, BlueprintRules.HEIGHT, Integer.MAX_VALUE);

    public int left() {
        return (BlueprintRules.WIDTH - width) / 2;
    }

    public int top() {
        return (BlueprintRules.HEIGHT - height) / 2;
    }

    /** True when this grid cell is inside the drawing area. */
    public boolean contains(int cell) {
        int x = cell % BlueprintRules.WIDTH, y = cell / BlueprintRules.WIDTH;
        return cell >= 0 && cell < BlueprintRules.CELLS && x >= left() && x < left() + width && y >= top() && y < top() + height;
    }
}
