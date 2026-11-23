package com.mamechin.enchantmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/** The mod config, {@code config/enchantmod-common.toml}. Tunable game numbers live in the balance JSON instead ({@code balance/}). */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue BOOK_LIMIT_ENABLED = BUILDER
            .comment("Cap the total enchant levels an enchanted book can hold (2 for vanilla books, 4 and 16 for the mod's book tiers).",
                    "The cap applies only when this and limit_enabled in the data file enchantmod/items/books.json are both true.")
            .define("bookLimitEnabled", true);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
