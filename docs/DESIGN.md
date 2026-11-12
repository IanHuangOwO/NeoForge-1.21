# EnchantMod design notes

Living reference for direction, style and decisions. Read it before adding a function or concept; update it when a decision changes. Items marked **TBD** are open and need the author's input.

## What the mod is

A magic mod centred on enchanting. Mod id `enchantmod`, package `com.mamechin.enchantmod`, NeoForge 21.1.265 for Minecraft 1.21.1, JDK 21.

The core loop: brew ink in cauldrons, store it in inkwells, write with quills at a Scribing Table.

## Vision (the author's words, recorded 2026-11-12)

- **Goal:** expand and modify Minecraft's enchanting system. The author finds vanilla enchanting not fun. This is the author's claim and has **not** been researched or checked yet; collect the concrete problems (see "Open questions") before designing against them.
- **Setting:** fantasy, built on runes and the power of ancient characters (writing as magic). The place where words are most common is a huge library, so the world feel is a labyrinth library.
- **Colour direction:** golden and coppery, labyrinth-library. Many mods share this palette, so the mod needs something of its own that sets it apart. **TBD:** what that distinguishing element is (a second accent colour, a shape language, a material or texture motif).
- **Inspirations:** Thaumcraft 5, Applied Energistics 2, and "Ars Nuevo" (read as Ars Nouveau; the feature wanted is that players create their own spells). **TBD:** which parts of each to take. From the roadmap so far, the guess is: Thaumcraft for research and discovery, AE2 for crafting and networks, Ars Nouveau for player-built spells.

## Open questions

Answer these to fill the big picture. Record answers here.

1. What exactly is wrong with vanilla enchanting for the author? (random outcomes, no choice, one-time level grind, enchantments that nobody uses, etc.)
2. Which part of Thaumcraft 5 appeals: research and discovery, the knowledge-gated progression, aspects, the arcane workbench, or the tone?
3. Which part of AE2: the logistics network, the autocrafting, the multiblocks, or the clean tech-magic look?
4. Which part of Ars Nouveau: building a spell from parts, the glyph and book style, or the mana system?
5. Is "create your own spell" a core feature, and does a spell built from written glyphs fit the quill-and-ink loop?
6. Is there a library structure (a generated labyrinth, shelves, books) to explore, or is the library only a visual theme for the player's own base?
7. Does the player learn glyphs by discovery (found, researched) or by crafting?
8. What is the end of progression: an ultimate item, a spell power ceiling, a mastery title?

**TBD:** progression beyond tier 2, other systems.

## How we work

- Small incremental steps; one batch of work, then a commit.
- Commit messages end with the attribution lines the harness gives.
- Nothing is considered verified until it has run in the game; say so when it has not.
- Replies are terse (caveman and ponytail modes): smallest change that fully solves the task, report anything skipped or unchecked.

## Naming

- Ids are tiered and neutral: `ink_tier0/1/2`, `inkwell_tier0/1/2`, `quill_tier0/1/2`, `small_cauldron`, `large_cauldron`. Renaming a tier later means a lang edit only.
- Display names use the material: Lapis Inkwell, Copper Quill, Calcite Cauldron, Dragon Ink and similar.
- Enchant-related naming is preferred for new things.

## Tiers

Three tiers each for ink, inkwell, quill and cauldron.

| Tier | Ink | Recipe base | Min heat |
|------|-----|-------------|----------|
| 0 | ink_tier0 | ink sac | 1 |
| 1 | ink_tier1 (glowing lapis look) | glow ink sac | 3 |
| 2 | ink_tier2 (dragon breath, darker than tier 1) | dragon breath | 3 |

- Liquid of enchanting is brewed from water and lapis lazuli (heat 1) and is the base of every ink.
- Tier 2 quill (obsidian plus ender eye) is a temporary recipe.
- Cauldron tiers: small and large exist; a third is deferred ("we wait it out").

## Cauldron

Small and large cauldron share one brewing logic (`CauldronBlockEntity`); only the numbers in `CauldronSize` differ.

| | Small | Large |
|---|---|---|
| Fluid | 4000 mB | 16000 mB |
| Item slots | 8 | 32 |
| Speed | x1 | x2 |

Brew rules:
- A brew converts the whole tank.
- Each ingredient is needed exactly once per 1000 mB (rounded up). Too few or too many means no brew.
- Recipes are data: type `enchantmod:cauldron_brewing`, fields `ingredients`, `base_fluid`, `result_fluid`, `brew_ticks`, `min_heat`.
- Heat comes from the block directly below the core, via a data map.
- Stored ingredients float above the cauldron; the liquid is drawn by the block entity renderer with eased height and colour, not a block model.
- Finish effect: a burst of small bubbles rising straight up (speed 0.015).
- Mining level and hardness match the vanilla cauldron.
- Idle cost matters: cached item list, no drawing when empty, tickers only while heated or brewing.

## Large cauldron multiblock

Layout (3x3x3, core at the centre):

```
y0: B A B   A H A   B A B
y1: B B B   B C B   B B B
y2: B B B   B A B   B B B
```

B smooth basalt, C small cauldron (becomes the core), A must be air, H any heat source (never converted, free to swap).

- Forms automatically when the pattern is completed. Shift-click forming is planned (**TBD**; vanilla skips `useItemOn` when sneaking with an item, so it needs another hook).
- The controller (core) holds all data. The 20 part blocks carry their cube position and forward clicks to it.
- Removing any part dismantles it back to a small cauldron plus smooth basalt.
- Looks: the core draws one OBJ model (`large_cauldron.obj`, 3 blocks across and tall, origin at the corner of the centre block). Parts are invisible but solid.
- The model's bowl floor is 8 px above the structure floor, so a full-block heat source overlaps its underside. **TBD:** fix by shortening the model or using shorter heat blocks.

## Style and art

- Textures start from vanilla textures with tints for development; custom art comes later.
- Glowing inks have no light border.
- 3D models: large cauldron is a generated OBJ model. The inkwell 3D model and per-tier cauldron ink models are deferred ("we will get to that").
- Particles: small and quiet (half-size bubbles).

## Not built yet

- Scribing Table function and GUI (the two-block block exists, it does nothing yet).
- Shift-click forming for the large cauldron.
- Cauldron tier 3.
- Per-tier cauldron ink models, inkwell 3D model.
- In-game verification of the large cauldron (rendering, forming, dismantling, light, shapes).

## Technical gotchas

- `LevelChunk.setBlockState` has the new state in place before `onRemove(old, ..., newState)` runs; a block entity is reused only if its type is valid for the new block, hence one block entity type per cauldron size and an explicit `moveTo` transfer.
- Server particles with count 0 treat the offsets as a velocity.
- Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew ...` from the project root.
