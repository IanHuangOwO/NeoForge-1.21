package com.mamechin.enchantmod.books;

/**
 * The book level cap rules (pure). A book may hold at most {@code cap} enchant levels summed over all its enchants (author,
 * 2026-11-22: tier 0 vanilla books 2, tier 1 4, tier 2 16, in {@code items/books.json}). Books made by the table, the quill, loot and
 * trades are trimmed to fit; the anvil refuses a combined book over the cap.
 */
public final class BookLimit {
    private BookLimit() {}

    /**
     * The levels kept when trimming to {@code cap}: in order, each enchant keeps as many of its levels as are left (0 means it is
     * dropped). A negative cap keeps everything.
     */
    public static int[] trim(int[] levels, int cap) {
        int[] out = levels.clone();
        if (cap < 0) return out;
        int left = cap;
        for (int i = 0; i < out.length; i++) {
            out[i] = Math.max(0, Math.min(out[i], left));
            left -= out[i];
        }
        return out;
    }

    /** Vanilla anvil level for one enchant: equal levels go up one, else the higher stays; never above the enchantment's maximum. */
    public static int anvilLevel(int left, int right, int max) {
        return Math.min(left == right ? right + 1 : Math.max(left, right), max);
    }
}
