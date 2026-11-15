package com.mamechin.enchantmod.scribing.rules;

import java.util.List;

/** The kinds of tool an enchant row can be filed under (its "category" in JSON), in tab order; "general" is all-purpose and what an unknown category becomes. */
public final class EnchantGroups {
    public static final List<String> CATEGORY_ORDER = List.of("general", "tools", "weapons", "armor", "bows");

    private EnchantGroups() {}
}
