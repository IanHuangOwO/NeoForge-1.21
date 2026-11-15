package com.mamechin.enchantmod.scribing.rules;

import java.util.Locale;
import java.util.function.Function;

/** The handbook's template store search. */
public final class TemplateSearch {
    private TemplateSearch() {}

    /** True when the query is part of the template's name or of the name of an enchant its drawing makes, ignoring case; an empty query matches all. */
    public static boolean matches(String name, Blueprint blueprint, String query, Function<Enchant, String> enchantName) {
        String q = query.strip().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) return true;
        return EnchantForecast.forecast(blueprint).stream().anyMatch(r -> enchantName.apply(r.enchant()).toLowerCase(Locale.ROOT).contains(q));
    }
}
