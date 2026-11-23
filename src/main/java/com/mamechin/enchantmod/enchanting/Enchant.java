package com.mamechin.enchantmod.enchanting;

import java.util.List;

/**
 * One row of the core table, from data/&lt;namespace&gt;/enchantmod/scribing/enchant/&lt;name&gt;.json.
 *
 * @param id          the row's own id, "namespace:name"; its texts are research.namespace.name and .why
 * @param enchantment the enchantment it makes, "namespace:path" (a bare id means minecraft)
 * @param runes       the one to three core rune ids that make it, sorted
 * @param category    the kind of tool it is for (see EnchantGroups); anything unknown is "general"
 * @param order       position in the handbook (then by id)
 */
public record Enchant(String id, String enchantment, List<String> runes, String category, int order) {
    /** The mod the enchantment belongs to, which decides its handbook tab. */
    public String namespace() {
        return enchantment.substring(0, enchantment.indexOf(':'));
    }

    /** The vanilla-style translation key of the enchantment's name. */
    public String nameKey() {
        return "enchantment." + enchantment.replace(':', '.');
    }

    /** The lang key of the handbook text about this row (add ".why" for the reason the runes fit). */
    public String textKey() {
        return "research." + id.replace(':', '.');
    }
}
