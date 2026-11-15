package com.mamechin.enchantmod.scribing.rules;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/** Installs the runes and enchants shipped in the mod's own data folder, the way the game does when data loads. */
public final class TestDefinitions {
    private static final Path DATA = Path.of("src/main/resources/data/enchantmod/enchantmod");

    private TestDefinitions() {}

    private static Definitions.Raw shipped;

    /** Makes the shipped files the current definitions (again, if another test replaced them). */
    public static synchronized void load() {
        if (shipped == null) shipped = new Definitions.Raw(read("rune"), read("enchant"), read("template"), text(DATA.resolve("forecast.json")));
        if (!shipped.equals(Definitions.current().raw())) install(shipped);
    }

    /** Installs exactly these files (ids "namespace:name") and returns what the parser complained about. */
    public static java.util.List<String> install(Map<String, String> runes, Map<String, String> enchants, String forecast) {
        return install(new Definitions.Raw(runes, enchants, forecast));
    }

    public static java.util.List<String> install(Definitions.Raw raw) {
        java.util.List<String> warnings = new java.util.ArrayList<>();
        Definitions.install(DefinitionParser.parse(raw, warnings::add));
        return warnings;
    }

    private static Map<String, String> read(String folder) {
        Map<String, String> out = new HashMap<>();
        try (Stream<Path> files = Files.list(DATA.resolve(folder))) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                out.put("enchantmod:" + name.substring(0, name.length() - ".json".length()), text(file));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return out;
    }

    private static String text(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
