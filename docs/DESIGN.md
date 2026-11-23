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

## Altar, first version (built 2026-11-15, not yet tried in game; superseded by "Altar redesign" below)

`blocks/AltarBlock` and `altar/AltarBlockEntity`: one block with two slots, the item to enchant and an enchant template (one of each). Right click with a template or any other item puts it on; an empty hand takes the template back, then the item; a quill (tag `enchantmod:quills`) starts the enchanting. Each enchant the template's forecast makes is rolled once (`EnchantForecast.roll`, same chances as the tooltip); an enchant the item cannot take, or that clashes with one it has, is skipped; a success upgrades to that level (never lowers a higher one). The result shows on the action bar. The template is kept and the quill is not worn, so both are `shortcut:` choices to revisit with the multiblock, the failure events and the animation described above. `altar/client/AltarRenderer` shows the item turning and the template floating over it. Placeholder art (vanilla enchanting table model, cut copper texture) and a placeholder recipe.

## Template renaming (built 2026-11-15, not yet tried in game)

The author dropped the template tiers (reverted: one `enchant_template` item, whole grid, no level cap). The scribing table has a name box on the parchment above the grid (dark wood with a copper rim, light text; grid cells are 12 px, 2026-11-15): typing sets the template's custom name (`RenameTemplatePayload`, sent on every change, 32 characters at most, empty removes it). A handbook store for templates (a saved list, then a chest with search and slots growing in steps of nine) was tried and dropped by the author on 2026-11-15; do not rebuild it unless asked.

## Template shelf, printing press and enchant cubes (built 2026-11-15, not yet tried in game)

Author's design, answers on 2026-11-15: **Template shelf** (`blocks/TemplateShelfBlock`): six templates stacked in a column, slot 0 at the top (changed from side by side by the author, 2026-11-16); click the strip of the front face at the height you point at; pointing at a full strip shows that template's tooltip beside the crosshair (the block entity syncs its contents to clients). Art (Fable pack, 2026-11-16): dark wood frame with brass runners, six flat gold-copper cards 13 x 1.9 x 12 px at the click heights, one 64x64 sheet; the template item is a gold-copper plate (`textures/item/enchant_template.png`); the shelf block is `noOcclusion()` because its front is open to put one in or take it out (chiseled bookshelf style); the block state `slot_0` to `slot_5` shows which are full (multipart model, placeholder look). **Enchant cubes** (`items/cube`): tier 0, 1 and 2 hold 1, 3 and 6 templates' worth of pressing ("etc." for later tiers); a cube carries `CubeContents`: for each enchantment the chance of each level. **Printing press** (`blocks/PrintingPressBlock`, `press/`): two blocks tall like the scribing table is two wide (`half` lower/upper; the block entity is in the lower half, the upper forwards clicks, breaking either removes both; author, 2026-11-15); right click opens its inventory (a cube slot in the middle and six template slots in a ring round it, numbered clockwise from the top; the cube's tier opens the top slot (1), the top and the two lower ones (3), or all (6), `PressLayout`; slots the cube does not open are blacked out, and a template left in one can still be taken out); shift and right click presses (swapped by the author, 2026-11-15). Pressing combines the forecasts of the open templates into the cube, replacing what it carried; **the same enchantment made twice adds up** (author, 2026-11-15, replacing "best outcome wins"; each result is an independent roll giving its level or nothing, the cube's level is the sum, so the chance of each total is a convolution; the sum is capped by the cube: tier 0 carries levels up to 5, tier 1 up to 10, tier 2 up to 20, and a higher total counts as the cap (`CubeContents.capped`, author 2026-11-16); the cap does not follow the enchantment's vanilla maximum, on purpose: exceeding the vanilla cap is one of the mod's goals; `CubeContents.combine`, tested). **A cube that already carries enchants is refused by the press** (author, 2026-11-16); a pressed cube alone in a grindstone comes out blank, for no experience (`items/cube/CubeGrindstone`, the cube says `canGrindstoneRepair` once pressed). **Templates are kept** by the press. **The altar now takes an enchant cube instead of a template** and rolls its chances; the cube is used up by a fixation, worked or not (author, 2026-11-15; kept only when nothing in it fits the item). **Press art and animation** (Fable pack, 2026-11-15): the base is the lower block's OBJ model (2 blocks tall, the upper block's model is empty); the head and four roller gears are extra models drawn by `press/client/PrintingPressRenderer`, driven by `PressAnimation` (one stroke = head down and up; one second per template; after every stroke but the last the cube lifts 2 px, rolls a quarter turn about X or Z so a new face is on top, and settles; the roller gears on the sides spin while it rolls. Six faces: X, X, X, Z, then a half turn about Z. The cube returns to its upright pose when the pressing ends, which only matters once the cube art has distinct faces). The server sends the stroke count as a block event on pressing; the cube is shown on the anvil (the block entity syncs its contents). The pressing is a server process (`remaining` ticks, saved): it starts only with a cube in the press and a template that gives something, the slots are locked while it runs, and at the end the cube, its tier and the templates are checked again before the result is written (author, 2026-11-16). The GUI is the drawn panel with Fable's slot positions (cube at 80,46; ring offsets 29 and 17/34), unopened slots get a riveted shutter. Placeholder art everywhere (vanilla block textures, the handbook map's GUI art) and guessed recipes (cubes: copper block + lapis, then gold block, then diamond on the cube below).

## Spinning wheel, first step: blocks and forming (built 2026-11-17, not yet tried in game)

Author's concept (2026-11-17): a new multiblock for the altar, a giant spinning wheel. The enchant cube goes in the wheel core; a pedestal stands in front of the wheel, 2 blocks away. Once activated the wheel spins up; at enough speed it pulls the enchanting power of the enchanted books in nearby chiseled bookshelves and spins it into the item on the pedestal, following the cube's patterns. The first version is a plain upright 9 x 9 x 1 structure that forms into one 3D model.

Built so far (package `wheel/` plus `blocks/`; the pedestal of the first version was folded into the altar):
- **Wheel core** (`wheel_core`, `WheelCoreBlock`): faces the player who placed it. Holds nothing since the altar redesign (see below).
- **Wheel body** (`wheel_body`, `WheelBodyBlock`, display name Spinning Wheel Frame): 24 of them form an upright ring 7 across round the core (corners cut) plus straight beams from the ring to the core (author, 2026-11-17, grown from 5 across to fit Fable's 7-wide model), in the plane at right angles to the core's facing; the core is 3 blocks above the bottom row (`WheelLayout.PATTERN`, pure and tested; author, 2026-11-17, after trying 3 x 3 and 5 x 5, replacing the 9-wide ring below, which is kept for a later tier, likely the tier 3 altar). Front view, `#` frame, `C` core, `B` smooth basalt:

```
...###...
..#.#.#..
.#..#..#.
B###C###B
B#..#..#B
B.#.#.#.B
B..###..B
```

**Tier 3 candidate (author, 2026-11-17, not built):** a ring 9 across with 8 beams (44 frames) and two 5-block smooth basalt pillars, 11 wide, the shape the art was drawn for:

```
...###...
.##.#.##.
.##.#.##.
#..###..#
####C####
#..###..#
.##.#.##.
.##.#.##.
...###...
```

**Pillars** (author, 2026-11-17): a column of smooth basalt (`minecraft:smooth_basalt` only; corrected by the author the same day from plain basalt) standing on a gold block (the bottom block of each pillar, author 2026-11-17) on each side, one block out from the ring, from the bottom row up to the core's height (4 blocks each, 8 in all; 5 each on the 9-wide tier), so the structure is 9 wide. When the wheel forms, the smooth basalt is swapped for the invisible `wheel_pillar` block (`WheelPillarBlock`, state `base` for the gold bottom: no item, drops and pick-blocks what it was) so Fable's pillar model can be drawn there; when the wheel is released (core, frame or pillar broken) the pillar blocks turn back into smooth basalt and gold. Placing smooth basalt calls no code of ours, so an unformed core tries to form once a second (up to a second late); everything else forms or releases at once. Unforming clears the core first so the parts it changes find no owner and do not release it twice. 
- **Pedestal:** removed on 2026-11-17; the altar stands in its place (see "Altar redesign" below).
- **Forming:** a core forms when both pillars stand and all 24 frame positions hold body blocks that are not already formed (so two wheels cannot share blocks); placing the core or the last body block forms it, and breaking the core or any formed body releases the whole wheel. Block state `formed` on core and body.

Decisions by Claude (change freely): the core's facing is the front, and the altar's spot is on the bottom row (the ground), centred under the core, 4 blocks out on the front side (`WheelLayout.PEDESTAL_DISTANCE`: 2 at first, 3 with the altar redesign, 4 from 2026-11-17 by the author). The altar now stands on that spot (see "Altar redesign"). Superseded 2026-11-22: the altar links a wheel anywhere in its tier 0 area.

**Later idea (author, 2026-11-17, not designed yet):** upgraded altar tiers get a bigger wheel built as a circle (the blocks placed in a ring, not a square), with 8 support beams joining the rim to the core. The 9-wide ring with 8 beams above is the first such candidate (tier 3); the current wheel is the 7-wide one. The model is round (Create-style 16-gon).

**Art (Fable pack, 2026-11-17):** no stand; the wheel is one free part (`wheel_rotor.obj`: hub with the cube cradle, 8 spokes, a wooden gear ring, a 16-segment round rim with a different rune carved dark on each segment (third pack, 2026-11-17: the runes, rim groove and spoke channels glow only while the wheel is active, through the separate `wheel_rotor_glow.obj` drawn at full brightness), radius 3.97 blocks, 256x256 sheet `spinning_wheel.png`). It is authored facing north in the floating pose, axis through the core's centre; at rest it stands on its rim half a block lower and may land at any angle. Second pack (2026-11-17): the wheel floats a whole block, from lift -0.5 (resting) to +0.5 (spinning) about the authored pose, ending level with the pillar crystals. Planned animation (Fable's suggestion): rise 1 block, spin up, hold, spin down, stop, sink. **Pillar models:** `wheel_pillar.obj` (1 x 5.5 x 1 blocks: copper foundation, a smooth-basalt-style stone shaft with 20 carved runes, brass rings and a crystal on the face toward the wheel, so it stands half a block above the core row) drawn twice, the east one turned round; `wheel_pillar_glow.obj` is a glow layer to draw over it only while the wheel is active (copied in, not drawn yet). Beam points for later (full-size model frame; scale them with the wheel): crystal tips at (-4.0125, 1.0, 0.5) and (5.0125, 1.0, 0.5), hub centre floating at (0.5, 1.0, 0.5), all from the core's corner, north-facing. The cube sits in an 8 px socket just in front of the hub (0.5, 0.5, -0.25 from the core's corner, floating pose); the thread is to be drawn by code straight from the cube to the pedestal's eyelet at (0.5, -3.594, -1.181) (stale: given for the full-size model with the pedestal 2 out; recompute for the scaled wheel and the altar 3 out). The pedestal is Fable's one-block model (`pedestal.obj`, 64x64 `wheel_pedestal.png`, brass rest at 12 px, eyelet on its +Z side toward the wheel); the core's item model is `wheel_item.obj`. `wheel.mtl` is shared (`m_1` glows). Rendering: `WheelRenderer` draws Fable's 7-wide pack (`wheel_small_rotor`, `wheel_small_rotor_glow`, `wheel_small_pillar`, `wheel_small_pillar_glow`, sheet `spinning_wheel_small.png`; rebuilt at true pixel size, radius 2.97 blocks, pillars 4.5 blocks tall at 4 out, hub and socket unchanged) at true size (`SCALE` is 1; it rescales if the layout and model radius differ); the 9-wide models stay in the mod for the tier 3 candidate but are not drawn. Formed core and bodies use an empty block model and are `noOcclusion()`; `wheel/client/WheelRenderer` draws the wheel (resting, or rising and spinning while its altar enchants), the altar's cube and both pillars, with a render box covering 11 x 9 round the core; the unformed core and frames use Fable's plain 16x16 textures (`wheel_frame.png` on all faces of a frame; `wheel_core_front`, `_side`, `_top` on the orientable core; 2026-11-17). Formed frames keep their full collision box, so the wheel's solid footprint is the ring and beams, not the model's exact shape.

Not built: the thread, bookshelf power draw. Activation, spinning and the enchanting are done through the altar (see "Altar redesign").

## Altar redesign: tier 0 altar and tier 1 spinning wheel (built 2026-11-17, not yet tried in game)

Author's design (2026-11-17), replacing the separate pedestal block: **the altar is the pedestal.**
- **Tier 0 altar** (`altar`, same id, `AltarBlock`, `AltarBlockEntity`): a pillar with a small four-arm flyer on top that spins on a vertical axis (turntable style, no facing); the enchant cube is the flyer's bobbin in the middle and the item floats above the block. Same slots and clicks as before (cube, item, empty hand takes back, cube first); a quill starts the enchanting.
- **Enchanting timing** (`altar/AltarSpin`, pure, tested; author, 2026-11-17, replacing the first fixed 10 seconds): spin up for 3 s; then every picked book drains at once, each for 1 s per enchant level on it; when the last book is done the cube's enchants are rolled (the old altar's logic: same chances, cube used up, result on the starting player's action bar) and the spin slows down for 2 s. Nothing can be put on or taken off while it runs; a second quill click says the altar is busy. The server counts down `remaining` of a synced `duration` (both saved); the client counts down too for the animation and gets resynced on start, when a book finishes and at the end.
- **Tier 1** (author, 2026-11-17: the wheel altar is tier 1): a formed spinning wheel linked by the altar's scan. Since 2026-11-22 (author) the wheel can stand anywhere and face any way, as long as its core is in the altar's tier 0 area (see "Armillary sphere"); the fixed spot 4 blocks out in front (`PEDESTAL_DISTANCE`) is gone. The cube and the item stay in the altar; the cube is shown both on the altar and in the wheel's hub (author, 2026-11-17: mirrored, not moved), and the wheel rises (lift from resting to floating with the spin speed) and spins with the altar's timing; the pillar glow layer shows while it spins. Tier 1 also scans a larger cylinder for bookshelves (see "Altar enchant power").
- **The wheel core holds nothing** (author's choice): it has an empty block entity (`WheelCoreBlockEntity`) only so the wheel can be drawn; clicks on the core or the frames do nothing.
- The `pedestal` block is removed, and so is Fable's pedestal model. **Altar art (Fable pack, 2026-11-17):** `altar_base.obj` is the block model (copper-banded foundation, basalt shaft with one carved rune per side, brass bearing plate, y 0 to 12 px, same from all four sides); `altar_flyer.obj` is drawn by `AltarRenderer` turning about the vertical axis through the block's centre (octagonal brass ring with four upright wooden arms, y 12 to 22 px, no spindle: the cube is the hub, socket x/z 4 to 12, y 12 to 20 px, and turns with the flyer); `altar_flyer_glow.obj` and `altar_base_glow.obj` are drawn at full brightness only while enchanting; `altar_item.obj` is the item icon; one 64x64 sheet `altar.png`, shared `wheel.mtl`. The item floats at (0.5, 1.625, 0.5).

## Altar enchant power (built 2026-11-17, not yet tried in game)

Author's design (2026-11-17), numbers are first guesses to tune:
- **Scan:** shift and right click the altar with a quill. The altar links the nearest formed spinning wheel core in its tier 0 area and the nearest formed glass sphere core in its tier 1 area (author, 2026-11-22), takes its tier (2 with both, 1 with the wheel only, otherwise 0; a sphere alone does nothing) and links every chiseled bookshelf in the tier's cylinder (`altar/AltarPower`, pure, tested). Areas (author, 2026-11-22, widened from radius 4 rows 0 to 2 and radius 8 rows -1 to 7, then from 5/7/9 the same day): tier 0 radius 8, rows -13 to +13; tier 1 radius 16, rows -15 to +15; tier 2 radius 32, rows -17 to +17, all relative to the altar. The scan reads the block entity lists of the loaded chunks the area touches (bookshelves and cores all have block entities), not every cell, so even radius 32 is instant on the server thread (author asked for no freeze, 2026-11-22; a worker thread was not used because the world must only be read on the server thread). Unloaded chunks are skipped, never loaded. The wheel remembers the altar that linked it (synced) to show its cube and spin; it draws only if that altar still links it back, so the last altar to scan wins. The link list is saved; shelves built later count only after a new scan. The action bar reports tier, shelves and power.
- **Book power:** an enchanted book is worth, over its stored enchants, level times the enchantment's weight. Weights come from rarities in `altar/altar.json` (author: "the combination of" levels and rarity, and "driven by json"; see "Balance files"): `rarities` (name to power per level), `default_rarity` and `enchantments` (id to rarity name; a bare id means `minecraft`). Shipped: common 1, uncommon 2, rare 4, very rare 8, and each vanilla enchantment placed by its vanilla anvil cost (Sharpness V = 5, Mending I = 4). No rarities: every level is worth 1.
- **Display:** holding a quill (either hand) and pointing at an altar shows "Tier N" and "Enchant power: P" beside the crosshair, or a hint to scan. The power is the server's sum over the linked shelves, refreshed once a second while idle and synced (the client cannot see inside chiseled bookshelves).
- **Cost:** the sum, over the cube's enchants the item can take, of the highest level each can roll (Claude's first formula; the cost is not weighted by rarity yet).
- **Paying:** on the quill click the linked shelves are shuffled; a shelf that is gone is skipped, and from each of the rest one random enchanted book is picked (one book per shelf, author 2026-11-17) until their power covers the cost. Not enough: the altar refuses and reports "have of needed". Enough: the spin starts and all picked books drain together (see the timing above); each becomes a plain book in the same slot when its own drain ends (any power above the cost is lost). While a book drains, the client sends a wave of vanilla enchant glyphs (our `enchantmod:drain_glyph` particle with the vanilla glyph sprites, 0 to 3 every tick, each leaving from a random point in the shelf's block for a random point near the target; author 2026-11-17: each glyph wanders on its own path but always reaches the target: `particle/GlyphPath`, pure and tested, sways on two random waves across the line, scaled by sin(pi t) so the sway is zero at both ends; life 20 ticks plus 5 per block, at most 80, plus up to 10) from its shelf to the altar's cube, or at tier 1 to the wheel's hub, with the hub sending waves on to the altar; a book's trail stops when it is drained. If a picked book is gone when its drain ends, the enchanting stops: no roll, the cube is kept, books drained so far stay drained (Claude's default).

## Armillary sphere (glass sphere): tier 2 altar (built 2026-11-22, not yet tried in game)

Name (author, 2026-11-22): **Armillary Sphere**, core block **Armillary Core** (display names only; ids, classes and files stay `sphere_*`). Called the glass sphere in older notes.

Author's design (2026-11-22): the parts of the final altar tier. A glass sphere covered in copper gears, with gears orbiting it as a ring (concept image ready for Fable). **Tier 2 = a formed spinning wheel and a formed glass sphere**, both linked by the altar's scan.
- **Shape** (`sphere/SphereLayout`, pure, tested): a core block (`sphere_core`, Armillary Core) in the middle of a hollow shell of `minecraft:glass` at radius 4 (210 blocks) and three rings of `minecraft:waxed_copper_block` at radius 5, one in each axis plane (78 blocks; author 2026-11-22, shrunk from 5 and 6 the same day; the rings cross on the axes). A cell is on radius r when its distance from the core rounds to r. Strict blocks only (author): no stained or tinted glass, no other copper stage. The shell is closed face to face (diagonal gaps are normal for a block sphere). No facing. Fixed size.
- **Placement** (author): the wheel and the sphere can stand anywhere round the altar, any way round, as long as the wheel core is in the altar's tier 0 area (radius 8, rows -13 to +13) and the sphere core in its tier 1 area (radius 16, rows -15 to +15; author, 2026-11-22). The sphere is 11 across, so a core within 4 of the altar puts the altar inside it.
- **Forming:** like the wheel's pillars: an unformed core checks once a second (placing glass or copper calls no code of ours). When every cell holds the right block, they are swapped for the invisible `sphere_part` block (state `ring`; no item, drops and pick-blocks what it was: glass or waxed copper, so formed glass drops without silk touch) and the core turns `formed`. Breaking the core or any part releases the sphere and turns the parts back into glass and copper.
- **Art (Fable pack, 2026-11-22):** a glass ball shaped like a C80 fullerene cage (42 panes in a brass frame, 7 with an etched rune; empty inside since the second pack the same day, which removed the heart crystal and axles, also from the item; nothing is modelled within 3.1 blocks of the centre), 38 gears on the panes, and three broken rings (a, b, c: copper with teeth, brass band, thin copper cog) that tumble round it. Models `sphere_shell`, `sphere_ring_a/b/c`, `sphere_gear_large/medium/small`, each with a `_glow` layer; one 256x256 sheet `spinning_sphere.png`; gear placements in `assets/enchantmod/sphere/gears.json` (resource packs can change them). Third pack (2026-11-22): a smaller ball, panes 3.125 blocks from the centre, gears out to 3.46; rings c 3.63 to 4.04, b 4.22 to 4.69, a 4.88 to 5.48, inside the 11-block box. The pack's baked `sphere_shell_gears` model was left out (not needed with turning gears). Unformed core: `cube_column` with `sphere_core_side`/`_top`; the item is Fable's `sphere_core_item.obj`; the formed core is invisible.
- **Animation** (`sphere/client/SphereRenderer`, numbers from Fable's README; author 2026-11-22: always moving, each gear turns, clock tick): the shell turns about the vertical once a minute; every gear ticks one tooth a second with an eased snap (ratios 1, -1.5, +2/-2 so neighbours turn opposite ways); each ring spins on its own axis (14, -22, 35 degrees a second), its tilt swings (20 +-15, 65 +-25, -45 +-40 degrees over 23, 17, 13 s) and its plane precesses round the vertical (5, -8, 11 degrees a second). The core keeps one animation clock on the client (`SphereCoreBlockEntity`), advanced each tick at 0.5x at rest (author, 2026-11-22: half of Fable's speeds; so the shell turns once in two minutes and the gears tick a tooth every 2 s), up to 4x with the linked altar's spin speed (accumulated, so nothing jumps). Glow layers are drawn at full brightness while the altar enchants. Drawn with the block cutout shader and no back-face culling (own render type `sphere_cutout_no_cull`) so the far panes show through the glass; the entity no-cull type was tried first and shaded faces by normal, which made the sphere dark (2026-11-22). Lit by the brighter of the core's cell and the cell just above the top ring. About 3700 faces a frame, 4600 while glowing. The sphere follows the altar that linked it (the altar link now lives in `guide/GuidedCoreBlockEntity`, shared with the wheel) and only if that altar still links it back.
- Not built: the sphere showing the cube, glyph trails through the sphere (at tier 2 they still go shelf, wheel hub, altar), a recipe.

## Faint light (built 2026-11-23, not yet tried in game)

A formed wheel core and a formed sphere core give light level 7, like the altar (which copies the enchanting table). Unformed cores give none. The light comes from the core block only, at the multiblock's centre.

## Build guide (built 2026-11-22, not yet tried in game)

Author's request (2026-11-22), to help build the big multiblocks: right click an unformed spinning wheel core or glass sphere core with a quill to show where each block goes; click again to hide it. Forming the multiblock hides it too. The guide draws a small (0.6 size), half see-through, fully lit copy of the wanted block in every cell that does not hold it yet, so it shrinks as you build (`guide/GuidedCoreBlockEntity` holds the synced, saved switch; `guide/client/GhostBlocks` draws it from the core's renderer). A wrong block in a cell hides its ghost inside it. The action bar says "Build guide shown" or "hidden". A quill on a formed core does nothing.

## Balance files (built 2026-11-17, not yet tried in game)

Author's request (2026-11-17): the tunable numbers are JSON. Package `balance/`: `Balance` (pure, tested) parses, `BalanceLoader` reads on data load and `/reload`, `BalancePayload` sends the merged files to clients (the client animates with them and shows cube numbers). Files are grouped by feature (author, 2026-11-17): `data/enchantmod/enchantmod/altar/altar.json`, `items/cubes.json`, `items/quills.json`, `press/press.json`, `cauldron/heat.json` (the namespace is always `enchantmod`: a pack overrides them by putting its copy at the same path); every pack's copy is merged key by key in load order (objects merge, anything else, arrays too, is replaced), so a pack can change one value. A wrong value is dropped with a warning in the log and the shipped default stays. Shipped values equal the earlier hard-coded ones.
- **`altar.json`:** `spin` (`ramp_up_ticks` 60, `ramp_down_ticks` 40, `drain_ticks_per_level` 20), `cost.power_per_level` (1; cost = this times the sum of each fitting enchant's highest level), `books_per_shelf` (1), `scan` (one entry per tier, index = tier, a higher tier uses the last: `radius`, `min_y`, `max_y` relative to the altar; shipped tier 0 radius 8 rows -13 to 13, tier 1 radius 16 rows -15 to 15, tier 2 radius 32 rows -17 to 17; radius capped at 32; area 0 is also where the wheel core must be, area 1 the sphere core), and the book weights (`rarities`, `default_rarity`, `enchantments`; replaces `altar_power.json`).
- **`cubes.json`:** by item id, `capacity` (templates the press presses into it, 1 to 6) and `max_level` (cap on one enchantment's level). Shipped: tier 0 1/5, tier 1 3/10, tier 2 6/20. An unlisted cube keeps its class's numbers.
- **`quills.json`:** by item id, `power` (vanilla enchanting power of the book rite) and `xp_levels` (its cost). Shipped: 10/3, 20/6, 30/10. An unlisted quill keeps its class's numbers.
- **`press.json`:** `ticks_per_template` (20); the stroke animation (`PressAnimation`, 20 ticks) is played faster or slower to fit.
- **`cauldron/heat.json`:** block id to `tier` (1 or more) and `requires_lit` (only for blocks with a LIT property). Shipped: torches 1, lit campfires 2, lava 3.
- Kept in code on purpose: the wheel's and the sphere's shapes (the art is drawn for them), item durability (fixed at registration), screen layouts, colours and the 7 x 7 grid.

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
- Display names use the material: Lapis Inkwell, Copper Quill, Copper Cauldron, Dragon Ink and similar.
- Enchant-related naming is preferred for new things.

## Tiers

Three tiers each for ink, inkwell, quill and cauldron.

| Tier | Ink | Recipe base | Min heat |
|------|-----|-------------|----------|
| 0 | ink_tier0 | ink sac | 1 |
| 1 | ink_tier1 (glowing lapis look) | glow ink sac | 3 |
| 2 | ink_tier2 (dragon breath, darker than tier 1) | dragon breath | 3 |

- Liquid of enchanting is brewed from water and lapis lazuli (heat 1) and is the base of every ink.
- **No crafting recipes** (author, 2026-11-17): every crafting-table recipe (altar, enchant cubes, printing press, quills, scribing table, template shelf) was removed to be designed later; everything is creative-tab only. The cauldron brewing recipes (inks, liquid of enchanting) stay. Earlier guesses, such as the tier 2 quill from obsidian plus ender eye and the cubes from copper block plus lapis, are in git history.
- Cauldron tiers: only the small cauldron exists. The large cauldron was removed on 2026-11-15 because the author wants to rethink the concept; the earlier design (3x3x3 smooth basalt multiblock, 16000 mB, 32 slots, speed x2, OBJ model) is in git history before that commit.

## Cauldron

The cauldron has one brewing logic (`CauldronBlockEntity`); the numbers that differ by size live in `CauldronSize` (only small exists): fluid 4000 mB, 8 item slots, speed x1.

Brew rules:
- Model and art (Fable pack, 2026-11-16): a square hammered-copper tub on four brass feet, as an OBJ model with one 64x64 sheet (`textures/block/small_cauldron.png`) and a liquid mask (`small_cauldron_liquid_mask.png`). The floor is at 3 px, the liquid levels stay 9, 12 and 15 px, the rim is at 16 px. The block's click and collision shape is still the vanilla cauldron's.
- The block class is `blocks/CauldronTier0` (renamed from `SmallCauldronBlock`, the id stays `small_cauldron`); it has no crafting recipe for now (author, 2026-11-16), so it is creative-only until one is designed.
- Clicks (author, 2026-11-16): right click with an ingredient puts one in (thrown items still fall in); empty hand right click takes the first stored stack out; shift and right click with an empty hand starts the brew.
- A brew converts the whole tank.
- Each ingredient is needed exactly once per 1000 mB (rounded up). Too few or too many means no brew.
- Recipes are data: type `enchantmod:cauldron_brewing`, fields `ingredients`, `base_fluid`, `result_fluid`, `brew_ticks`, `min_heat`.
- Heat comes from the block directly below the core, listed in the balance file `cauldron/heat.json` (block id to `tier` and optional `requires_lit`; moved from the NeoForge data map `data_maps/block/cauldron_heat.json` on 2026-11-17 so all mod settings sit under `enchantmod/`; block tags as keys are not supported).
- Stored ingredients float above the cauldron; the liquid is drawn by the block entity renderer with eased height and colour, not a block model.
- Finish effect: a burst of small bubbles rising straight up (speed 0.015).
- Mining level and hardness match the vanilla cauldron.
- Idle cost matters: cached item list, no drawing when empty, tickers only while heated or brewing.

## Data-driven runes and enchants

Runes, enchant rows and the forecast numbers are JSON in data packs (author, 2026-11-15), so they can be changed, or added by another mod or a data pack, without code. Files live under `data/<namespace>/enchantmod/scribing/`; the shipped ones are in `data/enchantmod/enchantmod/scribing/` (moved from `enchantmod/` on 2026-11-17 when the data folders were grouped by feature). Loading is in `scribing/DefinitionLoader` (runs on data load and `/reload`), parsing and checking is `scribing/rules/DefinitionParser`, and the result is held by `scribing/rules/Definitions`. The server sends the raw files to each client when it joins or data reloads (`DefinitionsPayload`); the client parses them the same way. An entry that is wrong in any way is dropped with a warning in the log and the rest still load.

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

## Quill on books

Author's design, 2026-11-16 (since 2026-11-22 any plain book tier, trimmed to its cap; see "Book tiers"): with a quill in the main hand and books in the other hand, a right click (in the air, or on a block that does not take the click) spends the player's experience levels and turns one book into an enchanted book. The enchants are rolled by vanilla (`EnchantmentHelper.enchantItem` with the enchanting table's enchantments). What the quill does is its `EnchantQuill.BookRite(power, xpLevels)`, read through `bookRite(stack)` so a custom enchant on the quill can change it later. First guesses: tier 0 power 10 for 3 levels, tier 1 power 20 for 6, tier 2 power 30 for 10. The quill is not worn. Creative players pay nothing and keep the book. Not tried in game.

## Book tiers and the book level cap (built 2026-11-22, not yet tried in game)

Author's design (2026-11-22): a change to vanilla. **An enchanted book holds at most a set number of enchant levels, summed over all its enchants**: tier 0 (the vanilla book) 2 (two enchants at level I, or one at II), tier 1 4, tier 2 16. The cap can be switched off, in the mod config (`config/enchantmod-common.toml`, `bookLimitEnabled`, default true; author 2026-11-22) or in the data; it applies only when both are on. Balance file `items/books.json`: `limit_enabled` (true) and `max_levels` ([2, 4, 16], index = tier, a higher tier uses the last). Package `books/`.
- **Tiers:** `book_tier1` (Bound Book) and `book_tier2` (Grand Tome) are plain books; enchanted they become `enchanted_book_tier1` / `_tier2` (Enchanted Bound Book, Enchanted Grand Tome), which store their enchants in `STORED_ENCHANTMENTS` like the vanilla enchanted book. Vanilla book textures for now, creative tab only (names are Claude's placeholders; a lang edit renames them). All four go in chiseled bookshelves (`minecraft:bookshelf_books` tag) and count for the altar; a drained book becomes a plain book of its own tier.
- **Over the cap** (author: trim, the anvil refuses): the enchanting table (`PlayerEnchantItemEvent`), the quill book rite, villager trades (every listing is wrapped, `VillagerTradesEvent`) and all generated loot (global loot modifier `enchantmod:trim_books`) roll as usual and then trim: enchants keep their levels in order until the cap is used up, the rest are cut (`BookLimit.trim`, pure, tested; Sharpness V becomes II). The anvil shows no result when a combined book would go over its cap (the left book's tier counts).
- **Enchanting tier books** (author: the enchanting table too): the table and the quill take any plain tier book (`TierBook`: enchantable like the vanilla book, every enchantment allowed, `applyEnchantments` turns it into the tier's enchanted book). The anvil combines and applies them, but vanilla's anvil only reads stored enchants from its own enchanted book, so when a tier 1 or 2 enchanted book is on either side `BookEvents` does the whole anvil step with vanilla's rules (levels: equal goes up one, else the higher, capped at the enchantment's maximum; incompatible enchants skipped at 1 level of cost each; cost per level is the enchantment's anvil cost halved, plus both prior-work penalties; renaming; prior-work penalty raised).
- Not covered: books from commands, creative or other mods; books already in a world are not trimmed until something makes a new one.


## Allay capsule and bound allays (built 2026-11-23, not yet tried in game)

Author's design (2026-11-23): a glass capsule catches an allay; bound to an altar, the allay carries dropped books to the altar's shelves. Package `allay/`.
- **Capsule** (`allay_capsule`, Allay Capsule; vanilla glass bottle texture for now, glints when full, creative tab only): use on an allay to catch it (the whole allay, held items included, is saved in the capsule). Use on a block to let it out there, free. Sneak-use on an altar to let it out bound to that altar (author: release at the altar, the capsule is reused). A plain use on the altar puts the capsule on the altar like any item.
- **Bound allay:** picks up dropped books, plain or enchanted, every tier, one at a time, inside the altar's current scan area, and puts each in a random shelf (author, 2026-11-23), picked once per trip from the shelves the altar linked at its last scan that have a free slot. With nothing to do, or no free shelf, it stays within 8 blocks of the altar. It ignores players it liked. It is steered through its brain's walk target, so vanilla flying and wandering still run. The binding is saved on the allay (attachment `enchantmod:bound_altar`) and is dropped when the altar is gone.
- **How many per altar:** `max_allays` in `altar/altar.json`, -1 = no limit (author: configurable, no limit for now). The count only sees bound allays that are loaded.
