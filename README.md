EnchantMod
==========

A NeoForge 1.21.1 mod that replaces the usual enchanting loop with a rune-based one. The theme is a labyrinth library: copper, brass, parchment and indigo. The mod is in early development and many parts are unfinished. The direction, rules and open questions are in [docs/DESIGN.md](docs/DESIGN.md), which is the place to read first.

What is in it
-------------

- **Cauldron and inks.** A small cauldron brews liquid of enchanting and three tiers of ink from water, lapis and other ingredients over a heat source. Recipes are data pack JSON.
- **Inkwells and quills.** Three tiers of each. An inkwell holds ink and wears down as you draw. It never breaks, it just runs dry.
- **Scribing table.** A two-block table where you draw on an enchant template. You place runes on a grid and join them with lines. Runes may not touch. Core runes are 3x3, stabilizers are 2x2 and the boost rune is 1x1. Every cell drawn costs one durability of the inkwell.
- **Enchant templates.** One template item. The template's tooltip shows which enchants the drawing makes and the exact chance of each level.
- **Research Handbook.** A Thaumonomicon-style map of knowledge nodes with side tabs. It has one page per rune and one per enchant. Carry it to see the enchant pages beside the scribing table, or click with it in hand to open the map. Its Templates tab is a small chest for enchant templates, with a search by name or enchant.

Not built yet: the altar that fixes a template onto an item, rune discovery and progression, recipes for most items, and a lot of art. Most of the in-game behaviour has been written and unit tested but not played through yet.

Runes and enchants are data
---------------------------

Runes, the enchant table, and the line-break numbers are JSON files under `data/<namespace>/enchantmod/` (`rune/`, `enchant/` and `forecast.json`). A data pack or another mod can add or change them without code. The server sends them to clients when a player joins or data reloads, and a wrong entry is dropped with a warning in the log. The file formats and the lang keys for their texts are described in the "Data-driven runes and enchants" section of the design document.

Building
--------

The project needs JDK 21.

- `./gradlew build` builds the mod.
- `./gradlew test` runs the unit tests. They cover the pure rules: blueprint placement, line routing, the forecast, the JSON parser and the handbook map layout.
- `./gradlew runClient` starts a development client. It also loads JEI for browsing recipes.

If libraries are missing in your IDE, run `./gradlew --refresh-dependencies`. `./gradlew clean` resets the build and does not touch your code.

Code layout
-----------

Code is grouped by feature under `src/main/java/com/mamechin/enchantmod/`:

- `scribing/` has the scribing table, its menu and screen, the handbook, and `rules/`, the pure rules with no Minecraft code.
- `cauldron/` has the cauldron block entity, its actions, brewing recipes, rendering and heat.
- `blocks/` and `items/` hold the classes Minecraft registers.
- `fluid/` has the ink and enchanting fluids.

Credits and licence
-------------------

All rights reserved. The project started from the NeoForge MDK template, whose licence is in `TEMPLATE_LICENSE.txt`. By default the project uses Mojang's official mapping names, which are covered by their own licence: https://github.com/NeoForged/NeoForm/blob/main/Mojang.md

Community documentation: https://docs.neoforged.net/ and the NeoForged Discord: https://discord.neoforged.net/
