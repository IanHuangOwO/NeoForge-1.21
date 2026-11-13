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

## Direction answers (2026-11-12)

- **Problem with vanilla:** it is boring. Once a player reaches a stable level 30 enchant there is one best pattern of enchantments and it no longer feels magical. Design goal: enchanting stays a choice with many good answers and feels magical. (Author's view, not yet researched.)
- **Thaumcraft 5:** wants the tone and discovery, but not a copy. Exact use **TBD**, to be explored later.
- **Applied Energistics 2:** wants the clean look and the unique logic that pulls a player in as they explore its possibilities (systems with depth that reward experimenting).
- **Ars Nouveau:** only a concept so far: the player builds their own enchants. Not a defined feature yet.
- **Library:** a visual theme, and possibly the altar setup the author imagines (**TBD**, details not described yet).
- **Core gameplay:** not decided yet. The author has no clear picture of it; this is the main thing to work out next.
- **Look and feel:** the fantasy-magic version of Create: lots of player freedom, with gears and some steampunk blended into the magic. Combined with the golden-copper library palette, this is the distinguishing element (gears, brass machinery and written runes together).

## Tentative core loop (author's brief thoughts, "a grain of salt", 2026-11-12)

Status: **tentative**. Not final design; use it to guide, not to constrain.

- **Collecting:** the player does not gather piles of items into chests. Like Thaumcraft's scanning glass, the player explores the world, finds ancient characters (runes) around and collects them by scanning. They are stored in a research tablet, not in inventory.
- **Record keeping:** the author likes the Thaumonomicon idea (the Thaumcraft research book): one book-like item that stores and records all progress. Here that is the research tablet.
- **Making enchants as a game:** pair enchant making with mini-games like Thaumcraft's research puzzle, and similar games the author named (artifact and relic mods, Ars Nouveau).
- **Slots by tier:** the higher the tier, the more characters can be written onto an item.
- **Item limit and risk:** every item can only withstand so many characters. The more characters added, the lower the chance that the fixation of the enchant succeeds at the altar. Risk grows with load, which is the answer to the "one best pattern" problem (the best enchant is a gamble, not a formula).
- **Altar:** where fixation happens (the final step that makes the written characters bind to the item).

Resulting loop in one line: explore and scan characters, record them in the research tablet, write them with quill and ink, then fix them on an item at the altar, with success depending on tier and character load.

## Decisions from the second round (2026-11-12)

- **Scanning, first version:** very simple. Shift-click on certain blocks (spawners, stone, ores and the like) to scan them. Later, world generation will add blocks that carry characters; scanning will extend to those.
- **Failed fixation:** its own class of event, like the Thaumcraft altar's instability events, not a simple lose-the-item penalty. Details **deferred**.
- **Blueprint at the Scribing Table:** the player completes the blueprint of an enchantment at the Scribing Table. The finished template can be saved into the research tablet and reused. What the mini-game is will be a **separate discussion**.
- **How characters combine into an effect:** **deferred**, its own discussion.
- **Character weight:** yes, characters have different weights, but **deferred**.
- **Item limit:** set by both the item's material and its rarity.
- **Altar:** a multiblock.
- **Items that progress:** the author likes artifact-style items that grow within the item (see the inspirations above). Fit with enchants **TBD**.

## Blueprint mini-game direction: circuit routing (author's idea, 2026-11-13, proposed)

Design principle: small simple rules that give a large logical setup, like AE2's limited channels and cellular automata. Rejected so far: heavy maths (GF(2) stacking, knapsack, matrix products) as too complex; cellular-automaton stability was one option but the author preferred this one.

The player lays out an enchant blueprint like a chip or circuit board:

- Characters are components on a grid. They are connected by lines (traces).
- **Neighbour rule:** characters cannot sit side by side. If allowed, adjacency reduces the enchant's strength (**TBD** which).
- **Crossing rule:** lines cannot cross on the same sheet. A crossing needs a different sheet, so components and lines move between stacked layers of paper.
- **Length and stability rule:** the longer a line, the less stable it is. Stability feeds into the altar's fixation chance.
- **Stabilizers:** dropped for now (author, 2026-11-13). Revisit only if line length alone is not enough.
- Sheets are layers; higher tiers allow more characters or more layers per item (tier and item limit rules above still apply).

Why it works: the goal "fit the desired enchant into the smallest, most stable layout" has many good answers, so there is no single best pattern, and skill shows in short routes and clever layer use. Real concepts behind it: circuit layout and routing, planar graphs, vias between layers, wire length, signal repeaters.

## Character hierarchy (author, 2026-11-13, proposed)

- Each character is one enchantment. Example: character 1 means Efficiency I.
- Characters have tiers. Low-tier (basic) characters work alone.
- A higher-tier character needs supporting characters of lower tiers connected to it by lines before it activates. The design is nested: higher tiers lean on lower ones.
- Different higher-tier characters can share the same supporting characters.
- The effect of a higher-tier character depends on which supporting characters it is connected to. Some characters have a strict requirement for how many supporting characters they need.
- Layout rules above (lines, no crossing per sheet, line length and stability) decide how these connections can be drawn.

Why it works: it is a dependency graph. Sharing supports saves weight and space, so players are rewarded for building one compact network that powers many enchants rather than one layout per enchant.

## Hierarchy and altar rules, round two (author, 2026-11-13, proposed)

- **Support chains:** support passes through. A character may connect directly to its supporters and also through chains (A needs B, B needs its own supporters).
- **Mixing:** a character connected to several supporters mixes their effects. Example: A connected to B and D gets an effect from the mix of B and D. B and D need their own supporters. A can also connect to C to boost or alter the effect of A.
- **Sharing limit:** a cap on how many characters one supporter can feed (AE2-style) is **left out of the first version**. Add it if the first design turns out too easy.
- **Unlocking higher tiers:** its own discussion, later.
- **Strict requirements:** as in the hierarchy section above (some characters need a set number of supporters).
- **Lines can break at the altar.** Each line has a chance to disconnect during fixation, for example 10%. A player can lay several parallel lines between the same two characters, so all of them must break for the link to fail (three lines at 10% each is about 0.1%). Characters have different sizes, which gives larger characters more places to attach lines.
- **When a link is lost:** a supporter that disconnects or deactivates weakens the effect or stops it from activating, depending on how important the supporter is (a major supporter stops it, a minor one weakens it).
- **Interaction with the earlier rule:** longer lines are less stable, so the break chance should rise with line length. Redundant lines use up room on the sheet and cannot cross on the same sheet, so reliability costs space and layers.

**Decided (author, 2026-11-13):**
- Break chance rises with line length (longer is less stable).
- Whether a supporter is major or minor depends on how it is connected, not on the character alone.
- Lines may attach on any side of a character (no fixed attachment points).
- Break checks are rolled at the altar and shown as an animation: enchanting power floods from the lowest tier characters up to the highest. A broken line or deactivated supporter stops the flood at that point, so the player sees which part of the network failed.

**Still open:**
1. Does the player see the computed chance of success before fixing?
2. Exact break formula (per cell rate, cap, minimum), to be tuned in play.
3. What "major" and "minor" connection means in numbers (for example the share of a character's supporters that remain).

## Open discussions (each is its own topic, to take one at a time)

1. The blueprint mini-game at the Scribing Table (direction: circuit routing, see above).
2. How characters combine into effects, and character weights.
3. The altar event system (what can go wrong and how the player reacts).
4. World generation: character blocks and the library structures.
5. Research tablet contents and layout.
6. Create-like machines: are there gears, or only stations?
7. Whether items gain progression like artifact mods.

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
