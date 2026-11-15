# EnchantMod design notes

Living reference for direction, style and decisions. Read it before adding a function or concept; update it when a decision changes. Items marked **TBD** are open and need the author's input.

## What the mod is

A magic mod centred on enchanting. Mod id `enchantmod`, package `com.mamechin.enchantmod`, NeoForge 21.1.265 for Minecraft 1.21.1, JDK 21. Code layout (author, 2026-11-15, revised): code is grouped by feature. `scribing/` holds everything about the scribing table and the research handbook: `rules/` (pure, no Minecraft: `Rune`, `Enchant`, `Definitions` and `DefinitionParser` for the JSON-driven runes and enchants, `Blueprint`, `BlueprintRules`, `EnchantForecast`, `EnchantGroups` for the category order, `LineRouting` for how a line leaves a rune and follows the cursor), the menu, block entity, payload, data components, `client/` (the table's screen) and `research/` (the handbook's map, pages and nine-slice drawing, and `GuiArt`, the rune drawing and texture paths the table's screen shares). `cauldron/` holds the cauldron block entity with `action/`, `brewing/`, `client/`, `heat/`. `blocks/` and `items/` hold the classes Minecraft registers: block and item classes, `ModBlocks`, `ModBlockEntities`, `ModItems`, with `items/inkwell/` and `items/quill/`. Fluids stay in `fluid/`. Class names for items carry no `Item` suffix.

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

- **Collecting:** the player does not gather piles of items into chests. Like Thaumcraft's scanning glass, the player explores the world, finds ancient characters (runes) around and collects them by scanning. They are stored in a research handbook, not in inventory.
- **Record keeping:** the author likes the Thaumonomicon idea (the Thaumcraft research book): one book-like item that stores and records all progress. Here that is the research handbook.
- **Making enchants as a game:** pair enchant making with mini-games like Thaumcraft's research puzzle, and similar games the author named (artifact and relic mods, Ars Nouveau).
- **Slots by tier:** the higher the tier, the more characters can be written onto an item.
- **Item limit and risk:** every item can only withstand so many characters. The more characters added, the lower the chance that the fixation of the enchant succeeds at the altar. Risk grows with load, which is the answer to the "one best pattern" problem (the best enchant is a gamble, not a formula).
- **Altar:** where fixation happens (the final step that makes the written characters bind to the item).

Resulting loop in one line: explore and scan characters, record them in the research handbook, write them with quill and ink, then fix them on an item at the altar, with success depending on tier and character load.

## Decisions from the second round (2026-11-12)

- **Scanning, first version:** very simple. Shift-click on certain blocks (spawners, stone, ores and the like) to scan them. Later, world generation will add blocks that carry characters; scanning will extend to those.
- **Failed fixation:** its own class of event, like the Thaumcraft altar's instability events, not a simple lose-the-item penalty. Details **deferred**.
- **Blueprint at the Scribing Table:** the player completes the blueprint of an enchantment at the Scribing Table. The finished template can be saved into the research handbook and reused. What the mini-game is will be a **separate discussion**.
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
- ~~No success preview~~ **Reversed (author, 2026-11-13):** the enchant template's tooltip shows the enchants the blueprint makes and the exact chance of each level, always visible for now. A later unlock (such as a scanning lens) may gate it. Numbers: see "Forecast model" below.
- The author leaves the exact numbers (break formula, major and minor weights) to Claude to decide, then tune in play.

## First enchant: Efficiency I to V (SUPERSEDED by the rune and core model below, 2026-11-13)

Efficiency is the first family to build. Each level is one character, and each character's tier equals its level.

| Character | Tier | Enchantment | Needs connected |
|-----------|------|-------------|-----------------|
| Efficiency I | 0 | Efficiency I | nothing |
| Efficiency II | 1 | Efficiency II | I |
| Efficiency III | 2 | Efficiency III | II and I |
| Efficiency IV | 3 | Efficiency IV | III and II |
| Efficiency V | 4 | Efficiency V | IV and III |

Each level connects to the two levels below it. One character of each level is enough, and the lower ones are shared: II feeds III, IV and V's chain, so a complete Efficiency V network is five characters and seven lines.

Failure behaviour: the item gets the highest level whose supporters are all still connected. If a line breaks, the effect drops down the chain (for example V loses the line to III and the result falls to IV). If the base character I fails, nothing activates. The altar animation floods from I upward and stops where a link is broken.

Known weakness: this layout is a flat strip with no crossings, so it does not use layers. Layers matter once enchants mix (a second family, or a booster character connected to several), which is for the next step.

## Runes, enchant cores and utility runes (author's idea, Claude's suggestion, 2026-11-13, proposed)

The author changed the model: a single character is not an enchant. Characters are **runes** (the Elder Futhark: Berkana, Mannaz, Dagaz and so on), and an **enchant core** is a group of connected runes that decides the enchant type.

### Rules

1. **Core:** a group of connected core runes that matches a row of the core table. A single rune can be a core (Berkana = Efficiency); a pair or triple of connected runes forms rarer cores.
2. **Boost runes:** a utility rune connected to a core adds levels. Suggested: Uruz (strength), +1 level per connected Uruz.
3. **Stabilizer runes:** high levels need them or the core is unstable. Suggested: Mannaz is needed from level 5, Dagaz from level 10 (assumption, the author's wording was loose: confirm).
4. **Utility runes are never part of a core**, so a core can always be read without ambiguity. Utility set for now: Uruz (boost), Mannaz (stabilizer 1), Dagaz (stabilizer 2).
5. **Sharing:** one rune may belong to two cores. Each shared rune raises the line break chance of every core that uses it (suggested +25% of the base per extra core). The more you share, the less stable.
6. **Matching:** a connected group of core runes must match a table row exactly. Unmatched groups do nothing. If two rows could match, the larger one wins.
7. Lines, layers, no crossing on one sheet, and break chance rising with length stay as above. Failure drops the level step by step; losing the core rune itself means no enchant.

### Core table (first draft, meanings from the runes' traditional names)

Rune meanings: Berkana growth, Isa ice or stillness, Fehu wealth, Jera year or renewal, Tiwaz victory (Tyr), Thurisaz thorn or force, Kenaz torch or fire, Sowilo sun, Algiz protection, Hagalaz hail, Laguz water, Ehwaz horse or movement, Ansuz breath or speech, Eihwaz yew (bows), Raidho journey, Gebo gift, Perthro hidden lot, Nauthiz need.

| Enchant | Core runes | Reason |
|---|---|---|
| Efficiency | Berkana | growth of speed (author's choice) |
| Unbreaking | Isa | stillness, holds its shape |
| Fortune | Fehu | wealth |
| Mending | Jera | renewal over time |
| Silk Touch | Gebo + Isa | a gift taken whole, unchanged |
| Sharpness | Tiwaz | victory, the spear god |
| Smite | Tiwaz + Sowilo | victory plus sun (holy) |
| Bane of Arthropods | Tiwaz + Thurisaz | victory plus thorn |
| Knockback | Thurisaz | force |
| Fire Aspect | Kenaz | torch |
| Looting | Tiwaz + Fehu | victory plus wealth |
| Sweeping Edge | Tiwaz + Ehwaz | victory plus movement |
| Protection | Algiz | protection |
| Fire Protection | Algiz + Kenaz | ward against torch |
| Blast Protection | Algiz + Hagalaz | ward against hail |
| Feather Falling | Algiz + Ansuz | ward plus breath |
| Respiration | Algiz + Laguz | ward plus water |
| Thorns | Algiz + Thurisaz | ward plus thorn |
| Depth Strider | Laguz + Ehwaz | water plus movement |
| Frost Walker | Laguz + Isa | water plus stillness |
| Soul Speed | Ehwaz + Nauthiz | movement plus need |
| Swift Sneak | Ehwaz + Perthro | movement plus hidden |
| Power | Eihwaz | yew, the bow |
| Flame | Eihwaz + Kenaz | bow plus torch |
| Punch | Eihwaz + Thurisaz | bow plus force |
| Infinity | Eihwaz + Jera | bow plus renewal |

The rest of the vanilla list (Lure, Luck of the Sea, Impaling, Riptide, Loyalty, Channeling, Multishot, Piercing, Quick Charge, Aqua Affinity, Density, Breach, Wind Burst) follows the same pattern and is filled in later. Possible first-wave build: Efficiency, Unbreaking, Fortune, Sharpness, Protection (all single-rune).

**Open:**
1. Confirm the stabilizer thresholds (Mannaz from level 5, Dagaz from level 10) and the enchant level caps (above vanilla, up to X and beyond).
2. Which runes the player knows at the start and how they are discovered is its own discussion (deferred).

**Decided (author, 2026-11-13):** a boost rune works when connected to any rune in a multi-rune core, not only a main one. The stabilizer thresholds (Mannaz from level 5, Dagaz from level 10) were not contradicted and stand as the working assumption; enchant levels have **no explicit cap** (author, 2026-11-13): the paper (layers, space) and the line breaks decide how high a level gets. Consequence: the evaluator must handle any level, and past level 10 the stabilizer rule needs a decision (suggested: each further 10 levels needs one more Dagaz).

## Scribing table UI, first version (built 2026-11-13, not yet tried in game)

What exists (package `scribing`):
- The table's left half holds a block entity with three slots: inkwell (tag `inkwells`), quill (tag `quills`) and an **enchant template** (new item `enchant_template`, paper look, stack of 1, creative tab only, no recipe yet). The right half forwards clicks. Contents drop when the table breaks.
- The layout is stored on the template item as the data component `enchantmod:blueprint` (runes plus lines), so it travels with the item.
- Screen: a 7 x 7 grid (shrunk from 15 x 11 to 9 x 9 and then 7 x 7 on 2026-11-15 at the author's request), a palette of all 24 Elder Futhark runes at the side (drawn as Unicode glyphs, no textures yet). Drag a rune from the palette onto the grid. Click a placed rune to start a line: it follows the cursor one cell at a time, orthogonal steps only, and stops at anything occupied; moving back over it retracts it. Click another rune next to the line's end to connect; right-click cancels. Right-click a rune erases it with all its lines; right-click a line cell erases that line (Claude's addition). A line cell is occupied, so nothing else can be placed on it, and lines can never cross on a sheet.
- Editing needs an inkwell, a quill and a template in the table. **Ink (author, 2026-11-13):** every grid cell drawn costs one durability of the inkwell: a core rune 9, a stabilizer 4, a boost rune 1, a line its length in cells. Erasing is free and gives nothing back (Claude's default). The inkwell never breaks: at 0 durability left nothing more can be drawn. The quill is not used up yet.
- Every edit is a packet; the server validates it with `BlueprintRules` (unit tested) before changing the item.

**Decided (author, 2026-11-13):** runes have a size on the grid. Core runes cover 3x3 cells, the stabilizers (Mannaz, Dagaz) 2x2, the boost rune (Uruz) 1x1. Runes may never touch, not even at a corner. Lines run along the outside of runes and start and end at a rune's edge, never over it. Lines are half a pixel thick and the colour of the rune glyphs. Hovering a rune shows its meaning and role.

Decisions made by Claude (change freely): grid size now 7 x 7 (was 9 x 9, 15 x 11, earlier 12 x 8; 3x3 runes need room; the 3D model's 12 x 8 inlay is now only decoration); a 2x2 rune is anchored at the top left of its middle; the first-version "Click another rune next to the line's end" now means next to any cell of that rune; one layer only; the palette shows all runes because "discovered runes" does not exist yet (`shortcut:` to replace when discovery is designed); parallel lines allowed; a template is not a vanilla paper.

Not built: layers, ink and quill use, the rune and core rules (core table, boost and stabilizer runes, break chances), the altar, discovery, saving templates to the research handbook.

## Forecast model (built 2026-11-13, numbers are Claude's defaults, change freely)

`EnchantForecast` (pure code, unit tested) reads a blueprint. A group of core runes joined by lines that matches a core table row exactly is an enchant (the table is the 26 enchant JSON files, see "Data-driven runes and enchants"). Each Uruz joined to any rune of the group adds one level on top of level 1. Mannaz is needed from level 5, Dagaz from level 10 and one more Dagaz for each further 10 levels, or the level stops below the threshold. A line breaks with 4% plus 1% per path cell (at most 95%); a link between two runes breaks only when all its parallel lines do. If the link inside a two-rune core breaks, the enchant fails. The chances are exact, worked out over every combination of surviving links. Shown in the template item's tooltip (`EnchantTemplate`). Rune sharing between cores is still left out.

## Research tablet, first version (built 2026-11-13, not yet tried in game)

Item `research_handbook` (named Research Handbook to match the earlier notes; creative tab only, no recipe, vanilla knowledge book look). For now it holds fixed pages (author, 2026-11-14): first one page per rune (24) with a short story of the symbol, utility runes also saying their role; then **one enchant per page** with why that combination of runes makes the enchant, what the enchant does, and a drawn example of the runes on their boards joined by copper lines (layout for 1, 2 or, later, 3 runes; a 3-rune page will need the text trimmed to fit). While the player carries one anywhere in the inventory, the scribing table screen shows the pages in a panel on its left, with arrow buttons and mouse wheel to turn them. Later it should hold scanned and discovered runes, and show only what the player has found. The map, pages, tabs, nodes, X and arrow buttons use Fable's textures in `textures/gui/research/` (frame and page are 9-slice, tiled; tab icons sit at pixel (6,6) of a tab), and the item has its own texture (2026-11-14). Holding the tablet and left- or right-clicking (air or block) opens the research map (author, 2026-11-14), modelled on the Thaumonomicon: a draggable map with one node per rune and enchant (50 nodes, all shown until a progression system exists), enchants joined by lines to their runes, split by side tabs styled like creative tabs, icon only with a name on hover: a Runes tab, one Vanilla tab holding all vanilla enchants, and later one tab per other mod that adds enchants (author, 2026-11-14). On the Vanilla map the all-purpose enchants (Unbreaking, Mending) sit in the middle and enchants for one kind of tool (tools, weapons, armor, bows) are clustered round them, each in its own direction; clicking a node opens its page, Esc or right click goes back. Node positions are pure and fixed (`ResearchMap`). In the map, a page has no arrows (it never jumps to another node); an X at the top right of the map closes the tablet and on a page returns to the map. The scribing table's side panel keeps the page turning but shows enchant pages only, in the same tabs minus Runes (an enchant id with a `namespace:` prefix is placed on that mod's tab; not exercised yet), as creative-style tabs on its left, and along its top a row of tool tabs for the open mod (author, 2026-11-14): All enchants first and the default (in case a mod's grouping is not set up), then General, Tools, Weapons, Armor, Bows for the kinds the mod has, so the player narrows the list by the tool they are enchanting; the top tabs reuse the left tab textures turned a quarter and drawn at half size (icons too) so six fit inside the page width, until Fable draws proper ones; a left click on a block is cancelled so it does not break it. The inkwell tooltip now shows "Ink: remaining / maximum"; the inkwell slot itself shows nothing extra yet.

## Altar, first version (built 2026-11-15, not yet tried in game)

`blocks/AltarBlock` and `altar/AltarBlockEntity`: one block with two slots, the item to enchant and an enchant template (one of each). Right click with a template or any other item puts it on; an empty hand takes the template back, then the item; a quill (tag `enchantmod:quills`) starts the enchanting. Each enchant the template's forecast makes is rolled once (`EnchantForecast.roll`, same chances as the tooltip); an enchant the item cannot take, or that clashes with one it has, is skipped; a success upgrades to that level (never lowers a higher one). The result shows on the action bar. The template is kept and the quill is not worn, so both are `shortcut:` choices to revisit with the multiblock, the failure events and the animation described above. `altar/client/AltarRenderer` shows the item turning and the template floating over it. Placeholder art (vanilla enchanting table model, cut copper texture) and a placeholder recipe.

## Template renaming (built 2026-11-15, not yet tried in game)

The author dropped the template tiers (reverted: one `enchant_template` item, whole grid, no level cap). The scribing table has a name box on the parchment above the grid (dark wood with a copper rim, light text; grid cells are 12 px, 2026-11-15): typing sets the template's custom name (`RenameTemplatePayload`, sent on every change, 32 characters at most, empty removes it). A handbook store for templates (a saved list, then a chest with search and slots growing in steps of nine) was tried and dropped by the author on 2026-11-15; do not rebuild it unless asked.

## Template shelf, printing press and enchant cubes (built 2026-11-15, not yet tried in game)

Author's design, answers on 2026-11-15: **Template shelf** (`blocks/TemplateShelfBlock`): six templates standing side by side; click a strip of the front face to put one in or take it out (chiseled bookshelf style); the block state `slot_0` to `slot_5` shows which are full (multipart model, placeholder look). **Enchant cubes** (`items/cube`): tier 0, 1 and 2 hold 1, 3 and 6 templates' worth of pressing ("etc." for later tiers); a cube carries `CubeContents`: for each enchantment the chance of each level. **Printing press** (`blocks/PrintingPressBlock`, `press/`): two blocks tall like the scribing table is two wide (`half` lower/upper; the block entity is in the lower half, the upper forwards clicks, breaking either removes both; author, 2026-11-15); right click opens its inventory (a cube slot in the middle and six template slots in a ring round it, numbered clockwise from the top; the cube's tier opens the top slot (1), the top and the two lower ones (3), or all (6), `PressLayout`; slots the cube does not open are blacked out, and a template left in one can still be taken out); shift and right click presses (swapped by the author, 2026-11-15). Pressing combines the forecasts of the open templates into the cube, replacing what it carried; **the same enchantment made twice adds up** (author, 2026-11-15, replacing "best outcome wins"; each result is an independent roll giving its level or nothing, the cube's level is the sum, so the chance of each total is a convolution; the sum is not capped at the enchantment's usual maximum yet; `CubeContents.combine`, tested). **Templates are kept** by the press. **The altar now takes an enchant cube instead of a template** and rolls its chances; the cube is used up by a fixation, worked or not (author, 2026-11-15; kept only when nothing in it fits the item). **Press art and animation** (Fable pack, 2026-11-15): the base is the lower block's OBJ model (2 blocks tall, the upper block's model is empty); the head, turntable and four side gears are extra models drawn by `press/client/PrintingPressRenderer`, driven by `PressAnimation` (one stroke = head down and up; one second per template; after every stroke but the last the turntable turns 60 degrees). The server sends the stroke count as a block event on pressing; the cube is shown on the turntable (the block entity syncs its contents). The pressing is a server process (`remaining` ticks, saved): it starts only with a cube in the press and a template that gives something, the slots are locked while it runs, and at the end the cube, its tier and the templates are checked again before the result is written (author, 2026-11-16). The GUI is the drawn panel with Fable's slot positions (cube at 80,46; ring offsets 29 and 17/34), unopened slots get a riveted shutter. Placeholder art everywhere (vanilla block textures, the handbook map's GUI art) and guessed recipes (cubes: copper block + lapis, then gold block, then diamond on the cube below).

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

- Ids are tiered and neutral: `ink_tier0/1/2`, `inkwell_tier0/1/2`, `quill_tier0/1/2`, `small_cauldron`. Renaming a tier later means a lang edit only.
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
- Cauldron tiers: only the small cauldron exists. The large cauldron was removed on 2026-11-15 because the author wants to rethink the concept; the earlier design (3x3x3 smooth basalt multiblock, 16000 mB, 32 slots, speed x2, OBJ model) is in git history before that commit.

## Cauldron

The cauldron has one brewing logic (`CauldronBlockEntity`); the numbers that differ by size live in `CauldronSize` (only small exists): fluid 4000 mB, 8 item slots, speed x1.

Brew rules:
- A brew converts the whole tank.
- Each ingredient is needed exactly once per 1000 mB (rounded up). Too few or too many means no brew.
- Recipes are data: type `enchantmod:cauldron_brewing`, fields `ingredients`, `base_fluid`, `result_fluid`, `brew_ticks`, `min_heat`.
- Heat comes from the block directly below the core, via a data map.
- Stored ingredients float above the cauldron; the liquid is drawn by the block entity renderer with eased height and colour, not a block model.
- Finish effect: a burst of small bubbles rising straight up (speed 0.015).
- Mining level and hardness match the vanilla cauldron.
- Idle cost matters: cached item list, no drawing when empty, tickers only while heated or brewing.

## Data-driven runes and enchants

Runes, enchant rows and the forecast numbers are JSON in data packs (author, 2026-11-15), so they can be changed, or added by another mod or a data pack, without code. Files live under `data/<namespace>/enchantmod/`; the shipped ones are in `data/enchantmod/enchantmod/`. Loading is in `scribing/DefinitionLoader` (runs on data load and `/reload`), parsing and checking is `scribing/rules/DefinitionParser`, and the result is held by `scribing/rules/Definitions`. The server sends the raw files to each client when it joins or data reloads (`DefinitionsPayload`); the client parses them the same way. An entry that is wrong in any way is dropped with a warning in the log and the rest still load.

**`rune/<name>.json`** (id `namespace:name`):
- `glyph`: exactly one character.
- `size`: 1, 2 or 3 (grid cells per side; board textures exist for those sizes only).
- `role`: `core` (makes up an enchant), `boost` or `stabilizer`.
- `order`: position in the palette and handbook, then by id (optional, default 0).
- boost: `levels` added by each one joined to a core (default 1).
- stabilizer: `unlock_level` (2 or more: levels from there up need at least one joined) and `extra_levels` (further levels allowed by each copy after the first, default 0). Shipped: Mannaz unlocks level 5; Dagaz unlocks 10 and each extra Dagaz adds 10.

**`enchant/<name>.json`** (id `namespace:name`):
- `enchantment`: the enchantment made, `namespace:path` (a bare id means `minecraft`). Its namespace picks the handbook tab.
- `runes`: one to three different core rune ids (a bare id means `enchantmod`).
- `category`: `general`, `tools`, `weapons`, `armor` or `bows` (the scribing table's tool tabs); anything else is `general`.
- `order`: position in the handbook (optional).
- Two rows with the same runes: the earlier one in order keeps them.

**`forecast.json`**: `line_break` with `base`, `per_cell` and `max` (a line breaks with `base + per_cell * length`, at most `max`; shipped 0.04, 0.01, 0.95). The highest-priority pack wins. Missing or wrong means the shipped numbers.

**Texts stay in lang files**, keyed by id: `rune.<namespace>.<name>` (the name), `.meaning` (the short story) and `.desc` (the role of a boost or stabilizer); `research.<namespace>.<name>` (what the enchant does) and `.why` (why those runes make it) for an enchant row; the enchantment's own name is the usual `enchantment.<namespace>.<path>`. A missing text shows the key.

Saved templates store rune ids; a bare id such as `berkana` means `enchantmod:berkana`, so templates made before runes had namespaces still work. The forecast computes whether the core stays joined exactly for rows of up to three runes.

Decisions by Claude (change freely): the handbook pages are now ordered by `order` then id (they were in the old table order, kept by the shipped `order` values); a data pack cannot yet change the grid size, the ink cost or the line cap. **Not verified in game:** the reload listener, the sync and the client receipt are only compile-checked; check once by opening the handbook, running `/reload` and reading the log for `Enchant definitions (server|client): 24 runes, 26 enchants`.

## Style and art

- Textures start from vanilla textures with tints for development; custom art comes later.
- Glowing inks have no light border.
- 3D models: the inkwell 3D model and per-tier cauldron ink models are deferred ("we will get to that").
- Particles: small and quiet (half-size bubbles).

## Not built yet

- Scribing Table function and GUI (the two-block block exists, it does nothing yet).
- Cauldron tier 3.
- Per-tier cauldron ink models, inkwell 3D model.

## Technical gotchas

- `LevelChunk.setBlockState` has the new state in place before `onRemove(old, ..., newState)` runs; a block entity is reused only if its type is valid for the new block, hence one block entity type per cauldron size and an explicit `moveTo` transfer.
- Server particles with count 0 treat the offsets as a velocity.
- Build with `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew ...` from the project root.
