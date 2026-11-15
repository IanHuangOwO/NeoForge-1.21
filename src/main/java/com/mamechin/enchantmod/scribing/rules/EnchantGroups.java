package com.mamechin.enchantmod.scribing.rules;

import java.util.List;

/** Which mod an enchant comes from and which kind of tool it is for. Pure data and lookups, shared by the handbook's map and the scribing table's tabs. */
public final class EnchantGroups {
    /** Every category in display order; "general" is the all-purpose one and is also what an enchant with no grouping set up gets. */
    public static final List<String> CATEGORY_ORDER = List.of("general", "tools", "weapons", "armor", "bows");

    private static final List<List<String>> BY_CATEGORY = List.of(
            List.of("efficiency", "fortune", "silk_touch"),
            List.of("sharpness", "smite", "bane_of_arthropods", "knockback", "fire_aspect", "looting", "sweeping_edge"),
            List.of("protection", "fire_protection", "blast_protection", "feather_falling", "respiration", "thorns", "depth_strider", "frost_walker", "soul_speed", "swift_sneak"),
            List.of("power", "flame", "punch", "infinity")); // tools, weapons, armor, bows: CATEGORY_ORDER without "general"

    private EnchantGroups() {}

    /** Vanilla enchant ids are bare; another mod's would be "namespace:id". */
    public static String namespace(String enchant) {
        int colon = enchant.indexOf(':');
        return colon < 0 ? "minecraft" : enchant.substring(0, colon);
    }

    /** The kind of tool an enchant is for, from {@link #CATEGORY_ORDER}. */
    public static String category(String enchant) {
        for (int k = 0; k < BY_CATEGORY.size(); k++) if (BY_CATEGORY.get(k).contains(enchant)) return CATEGORY_ORDER.get(k + 1);
        return "general";
    }
}
