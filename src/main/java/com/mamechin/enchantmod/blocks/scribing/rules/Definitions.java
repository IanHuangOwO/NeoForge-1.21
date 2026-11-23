package com.mamechin.enchantmod.blocks.scribing.rules;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The runes, enchant rows and forecast numbers currently in force. The server fills it when data packs load and sends the
 * raw files to every client, which parse them the same way; everything that needs a rune or an enchant reads it from here.
 * Pure data, no Minecraft, so rules and tests can use it directly.
 */
public final class Definitions {
    public static final String DEFAULT_NAMESPACE = "enchantmod";

    /** The files as they were read: ids ("namespace:name") to JSON text, and the forecast file or null. This is what is sent to clients. */
    public record Raw(Map<String, String> runes, Map<String, String> enchants, String forecast) {
        public static final Raw EMPTY = new Raw(Map.of(), Map.of(), null);
    }

    /** One parsed set of definitions. version changes every time a new set is installed, so caches can tell. */
    public record Snapshot(List<Rune> runes, List<Enchant> enchants, ForecastRules rules, Raw raw, Map<String, Rune> runeById, Map<String, Enchant> byRunes, int version) {
        public static final Snapshot EMPTY = new Snapshot(List.of(), List.of(), ForecastRules.DEFAULT, Raw.EMPTY, Map.of(), Map.of(), 0);
    }

    private static final AtomicInteger VERSIONS = new AtomicInteger();
    private static volatile Snapshot current = Snapshot.EMPTY;

    private Definitions() {}

    public static Snapshot current() {
        return current;
    }

    /** Installs a parsed set (see DefinitionParser); returns it with its new version. */
    public static Snapshot install(DefinitionParser.Parsed parsed) {
        Snapshot next = new Snapshot(parsed.runes(), parsed.enchants(), parsed.rules(), parsed.raw(), parsed.runeById(), parsed.byRunes(), VERSIONS.incrementAndGet());
        current = next;
        return next;
    }

    public static List<Rune> runes() {
        return current.runes();
    }

    public static List<Enchant> enchants() {
        return current.enchants();
    }

    public static ForecastRules rules() {
        return current.rules();
    }

    /** "namespace:name" for an id that may be bare: ids saved before runes had namespaces are in the enchantmod namespace. */
    public static String canonical(String id) {
        return id.indexOf(':') < 0 ? DEFAULT_NAMESPACE + ":" + id : id;
    }

    public static Rune rune(String id) {
        return id == null ? null : current.runeById().get(canonical(id));
    }

    /** The enchant row made by exactly these canonical rune ids, sorted, or null. */
    public static Enchant enchantFor(List<String> sortedRuneIds) {
        return current.byRunes().get(String.join("+", sortedRuneIds));
    }
}
