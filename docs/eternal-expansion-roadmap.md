# PROJECT ETERNAL — ETERNAL EXPANSION ROADMAP

**Document:** `docs/eternal-expansion-roadmap.md`
**Baseline:** `0.1.0-slice` Release Candidate (verified stable via the independent emulator audit)
**Status:** PLANNING ONLY — no gameplay implementation started
**Reviewer:** ChatGPT (architectural/product reviewer between content packs)

This document is the authoritative long-term expansion blueprint. It maps the current game
from the repository (not from prior chat reports), identifies content gaps, proposes a
multi-pack roadmap, fully specifies **Pack 01 — Outer Reaches**, checks the architecture,
flags balance risks, and prescribes an implementation and test sequence.

The original master prompt (`project-eternal-opencode-prompt.md`) and the expansion master
prompt (`project-eternal-eternal-expansion-master-prompt.md`) remain authoritative. This
document extends them; it does not replace them.

---

# TASK 1 — MAP OF THE CURRENT GAME

Everything below is transcribed from the repository (`core-content`, `core-model`,
`core-simulation`, and the feature modules). Where the master prompts describe an ideal and
the repo implements less, the repo is authoritative.

## 1.1 Regions

| Region | Tier | Unlock | Price modifier | Yield multiplier | Nodes (implemented) |
|---|---|---|---|---|---|
| **Hollowreach** | 1 | default | 1.0 | 1.0 | Rustrock Quarry (MINE), Wolf's Woods (FOREST), Riverfield Terraces (FARM), Brindlebrook Shallows (FISHERY), Deep Quartz Grotto (MINE, `quest:deep_mine`), The Sundered Ruin (RUIN, `quest:guardian`), Hollowreach Village (CITY) |
| **Emberreach** | 2 | `region:emberreach` | 1.5 | 1.0 | Emberreach Forge-Seam (MINE), Scorchpine Grove (FOREST), Emberhold Market (TRADE_HUB) |
| **Stormreach** | 3 | `region:stormreach` | 2.0 | **1.5** | Roaring Coast (FISHERY) — **only one node** |
| **Dawnreach** | 4 | `region:dawnreach` | 2.5 | **1.6** | Skybreak Spire (SPECIAL), The Starfall Abyss (RUIN) |

Regional mechanics today: the only *distinct* regional mechanic implemented is Stormreach's
`yieldMultiplier` (1.5×) applied to gathering yields. Emberreach and Dawnreach differ from
baseline only by price/yield modifiers. There is **no** per-region mechanic beyond modifiers.

`NodeType` offers `MONSTER_TERRITORY` but no node uses it — monsters are rendered directly
from `Monsters.inRegion`.

## 1.2 Combat

Monster catalog (6 monsters, 3 bosses):

| Monster | Tier | Region | HP | Role | Guaranteed loot |
|---|---|---|---|---|---|
| Grey Wolf | 1 | Hollowreach | 55 | normal | Wolf Pelt 70 / Wolf Meat 30 |
| Bristle Boar | 1 | Hollowreach | 80 | normal | Boar Meat 60 / Boar Hide 40 |
| Hollowreach Guardian | 2 | Hollowreach | 320 | **boss** | Guardian Core 100 |
| Ash-Spawned Brute | 2 | Emberreach | 150 | normal | Boar Hide 50 / Boar Meat 50 |
| Storm Herald | 4 | Stormreach | 640 | **boss** | Stormheart Core 100 |
| Starfall Wyrm | 5 | Dawnreach | 950 | **boss** | Catalyst 60 / Stormheart Core 40 |

Combat model: transparent formula (`expectedHitDamage`, `hitChance`, crit averaging),
continuous session resolution via `CombatResolver` (kills/hour from DPS, retreat at 20% HP),
loot resolved separately with guaranteed + rare entries. No elites exist. No monster
territory nodes. Boss progression: Guardian → (Emberreach) → Herald → (Dawnreach gate) → Wyrm.

Loot progression today: pelts/meat (T1) → Guardian Cores (T2) → Voidforged Crystals (T3)
→ Stormheart Cores (T3) → Stormbound Catalyst (T4). Ward of Stability drops at 5% from
Ash-Spawned Brutes.

## 1.3 Lifeskills

16 skills implemented:

- **Gathering (5):** Mining, Logging, Herbalism, Farming, Fishing — all `YIELD_PERCENT` 0.5%/lvl
- **Processing (6):** Grinding, Heating, Smelting, Refining, Drying, Milling — `EFFICIENCY_PERCENT` 1%/lvl
- **Crafting (6):** Blacksmithing, Armorcraft, Cooking, Alchemy, Carpentry, Engineering — `EFFICIENCY_PERCENT` 1%/lvl

Skill progression: level XP curve `12·lvl·(lvl+1)`; max level 99 (data). Mastery perks are
flat per-level multipliers; no specialization trees, no mastery perks beyond the single
`perkPerLevel`. Herbalism has **no dedicated node** (it is only a secondary skill on
Wolf's Woods and Scorchpine Grove). Excavation, Tailoring, and Jewelry from the master
prompt §11 are **not implemented**.

## 1.4 Industry

Processing recipes (8):

- Iron chain: Grind Iron Ore → Iron Fragment; Smelt Iron Ingot; Refine Iron (Refining 1);
  Forge Steel (Refining 2); Distill Stormbound Catalyst (Refining 5, `recipe:refine_catalyst`)
- Wood chain: Dry Lumber (Heating)
- Harvest chain: Dry River Carp (Drying); Mill Grain to Flour (Milling)

Crafting recipes (12, one duplicated — see §9.1):

- Carpentry: Twist Hemp Rope; Build Stout Fishing Rod; Band the Oak Shield
- Engineering: Fabricate Machinery Part; Shape Ward of Stability
- Cooking: Roast Wolf Meat; Bake Bread
- Alchemy: Brew Lesser Vitality Draft
- Blacksmithing: Forge Bronze Sword; Forge Steel Sword; Forge Journeyman's Pickaxe
- Armorcraft: Stitch Hide Armor; Stitch Hide Hood

Resource chains today:

```
Iron Ore → Iron Fragment → Iron Ingot → Refined Iron → Steel → [Machinery Part]
                                  ↘ Bronze Sword / Pickaxe       ↘ Steel Sword
Iron Ingot + Coal → (Forge Steel)
Wood → Lumber → [Oak Shield / Fishing Rod] ; Wood → Rope → [Fishing Rod / Shield]
Grain → Flour → Bread
River Carp → Dried River Fish
Wolf Meat → Roasted Meat ; Brightleaf → Lesser Vitality Draft
Steel + Lumber → Machinery Part ; Crystal + Steel → Ward of Stability
Stormheart Core + Voidforged Crystal ×2 → Stormbound Catalyst
Pelt Wolf ×3 + Wood → Hide Armor ; Pelt Wolf ×2 → Hide Hood
```

Chain depth is thin in the mid/late tiers: wood and pelt chains terminate at T1–T2 gear;
there is no leather, no glass, no gem/jewelry chain, no accessory line, no glove/boot line,
and no T3 base equipment.

## 1.5 Equipment

Implemented gear (7 definitions, 4 slots actually used):

| Item | Slot | Tier | Stats | Durability |
|---|---|---|---|---|
| Journeyman's Pickaxe | TOOL | 1 | atk 1 (mining +25%) | 100 |
| Stout Fishing Rod | TOOL | 1 | atk 1 (fishing +25%) | 90 |
| Bronze Sword | MAIN_WEAPON | 1 | atk 8, acc 5 | 80 |
| Steel Sword | MAIN_WEAPON | 2 | atk 18, acc 8 | 110 |
| Oak Shield | SECONDARY_WEAPON | 1 | def 6, acc −2 | 120 |
| Hide Armor | CHEST | 1 | def 4, maxHp 20 | 70 |
| Hide Hood | HELMET | 1 | def 2, maxHp 8 | 60 |

Slots **unused**: `GLOVES`, `BOOTS`, `ACCESSORY_1`, `ACCESSORY_2`. There is **no T3
equipment** and nothing between Steel Sword (T2) and the +31 Transcendent gate. Gear
progression is: pickaxe → bronze sword → steel sword (only real weapon upgrade).

Durability/repair: 0.4/gather action on the tool, 0.5/kill on weapon, 0.2/kill on armor;
repair consumes a Repair Kit (costs 5 Marks + 1 kit, restores 20 durability per kit,
requires `ceil(deficit/20)` kits).

Enhancement bands (data-driven):

| Band | Levels | Material | Failure | Stat/level |
|---|---|---|---|---|
| BASE | +0..+15 | Resonance Shards | durability-only below +11; downgrade 1 at ≥ +11 | 0.15 |
| ADVANCED | +16..+30 | Voidforged Crystal (Emberreach) | durability-loss / downgrade 2 | 0.15 |
| TRANSCENDENT | +31..+45 | Stormbound Catalyst (`recipe:refine_catalyst`) | **shatter to +31** on unprotected failure | 0.20 |
| ASCENDANT | +46..+60 | none granted | downgrade 2 | 0.25 |

Protection: Oil of Preservation (blocks downgrade), Ward of Stability (full negation).
Resolve: +1/+2/+3 per fail per band, capped bonus 25/30/35%. ASCENDANT carries
`tier:ascendant`, a token **no content grants** — the band is architecture-only (intentional).

## 1.6 Workers (Retainers)

Catalog: starter Aldo (MINER, free) + 6 recruits:

| Recruit | Spec | Cost | Unlock |
|---|---|---|---|
| Ode | LUMBERJACK | 350 | `screen:workers` |
| Mara | FARMER | 400 | `screen:workers` |
| Selkie | FISHER | 450 | `recipe:craft_fishing_rod` |
| Sable | CRAFTER | 700 | `region:emberreach` |
| Helga | MINER | 800 | `region:emberreach` |
| Voss | FORAGER | 1200 | `region:stormreach` |

Mechanics: retainers only **gather** at assigned nodes (they cannot process or craft).
Stamina (1/action, 0.02/s regen) caps long-run output (~72/hr vs player 120/hr). Traits
(milestones 3/7/12/18/25): Eager (+15% speed), Lucky Hands (+20% luck), Deep Focus (+10%
speed), Sharp Senses (+10% luck), Unyielding (+25% speed / −10% luck). `CRAFTER`
specialization's `productionSpeed` has **no effect** because there are no workshop nodes or
retainer-processing.

## 1.7 Economy

- Currency: **Marks**. Starting 50.
- Market: single global market. `currentBuyPrice = buyPrice × bestRegionModifier × drift`;
  `currentSellPrice = sellPrice × bestRegionModifier × [processedDemandPremium for
  MATERIAL/CONSUMABLE/EQUIPMENT] / drift`. `processedDemandPremium` = 1 + 0.05 ×
  furthest region tier (≤ 1.25). `priceDrift` per item, drift ∈ [0.8, 1.25], moves 0.004/trade.
- `demandPressure` field exists in `MarketState` but is **never written** (dead field).
- Sinks today: Repair Kits (cheap), Oil/Ward (craftable), worker hires (350–1200), market
  buys (Ward 630, Catalyst 2250, Steel 130, Machinery Part 240). Sources: quest rewards,
  selling gathered/processed goods, notable offline events (merchant 5–25 Marks).

## 1.8 Quests

14 quests:

- **Main/regional chain:** `q_intro` (equip tool + gather 3 ore) → `q_first_ore` (10 ore →
  sword recipe + `screen:crafting`) → `q_first_sword` (craft+equip sword → `quest:hunt` +
  `screen:enhance`) → `q_hunt_wolves` (8 wolves → `quest:deep_mine`) → `q_deep_mine`
  (discover grotto → `quest:guardian` + `screen:workers`) → `q_guardian` (kill Guardian →
  `region:emberreach`) → `q_emberreach_ash` (10 Ash-Spawned → `region:stormreach`) →
  `q_stormreach_herald` (kill Herald → `recipe:shape_ward` + `recipe:refine_catalyst`) →
  `q_light_beyond` (reach +31 AND hold a Catalyst → `region:dawnreach`).
- **Side:** `q_side_armor`, `q_side_boar`, `q_side_forger`, `q_side_harvest` (mill/bake),
  `q_side_fisher` (carp → rod recipe).

Objective types supported (all data-driven): HAVE_ITEMS, GATHER, CRAFT, KILL,
DISCOVER_NODE, REACH_CHAR_LEVEL, REACH_SKILL_LEVEL, HAVE_MARKS, ENHANCE_ANY_TO,
EQUIP_SLOT, VISIT_REGION. `EQUIP_SLOT` is pinned (one-time). No repeatable/daily quests.

## 1.9 Offline simulation

- One shared rate path (`Rates`) for foreground and offline.
- `OfflineSimulator`: closed-form batch math; steps per-second below 90 s; **12 h cap**
  (`MAX_OFFLINE_SECONDS`); report threshold 15 s; notable events capped at 3 rolls.
- Supported activities: GATHERING, COMBAT, PROCESSING, CRAFTING (single active activity).
- Retainer offline production: gathering only, stamina-budgeted, deterministic.
- Limitations: a new activity type would need offline wiring; retainer processing/crafting
  is not supported; no multi-activity parallelism.

## 1.10 Persistence

- Room stores a single atomic JSON blob: `save_slot` table with LIVE + BACKUP rows,
  SHA-256 checksum, version; temp-write → swap; backup rollback on checksum failure.
- `SaveSchema.CURRENT = 1`. Explicit migration registry (none yet needed).
- Foreground save: ~1 Hz tick flush + 3 s debounce + immediate flush on pause.
- Offline timestamp clamping to 12 h; future-timestamp tolerance via clamping.

---

# TASK 2 — MAP OF CURRENT PLAYER PROGRESSION

Fresh character (50 Marks, starter pickaxe equipped, 2 potions, 3 shards, 1 repair kit,
1 oil, retainer Aldo) → the main chain resolves automatically to a fixpoint:

```
Create → Equip tool → Gather 3 Iron Ore (q_intro)
  → Gather 10 ore (q_first_ore) → [recipe:sword_bronze, crafting]
  → Smelt ingots → Forge + Equip Bronze Sword (q_first_sword) → [hunting, enhance]
  → Kill 8 wolves (q_hunt_wolves) → [deep mine]
  → Discover Deep Quartz Grotto (q_deep_mine) → [Guardian, workers]
  → Kill Hollowreach Guardian (q_guardian) → [Emberreach]
  → Kill 10 Ash-Spawned Brutes (q_emberreach_ash) → [Stormreach]
  → Kill Storm Herald (q_stormreach_herald) → [ward + catalyst recipes]
  → Reach +31 enhancement + hold Catalyst (q_light_beyond) → [Dawnreach]
  → Farm Skybreak Spire / farm the Starfall Wyrm → (nothing further)
```

Per-system assessment of where progression is today:

- **Strong:** the fresh→bronze-sword→wolf chain is genuinely interconnected (gather→process→
  craft→equip→fight→quest), with visible quest rewards and unlock cascades. The offline loop
  (12 h, deterministic, clean report) is a healthy idle curve. Enhancement is data-driven and
  has a real failstack/shatter identity.
- **Shallow:** the entire mid-game is essentially Emberreach → Stormreach in three quest
  hops. Emberreach has 1 monster; Stormreach has **1 node and 1 monster**; Dawnreach is 2
  nodes + 1 boss reached via a single +31 gate.
- **Repetitive:** every gathering node uses the same mechanics; no region has a distinctive
  interaction; combat is 6 stat profiles; equipment is 7 items.
- **Underdeveloped:** Herbalism (no node), Fishing rod/`CRAFTER` retainer (no effect),
  secondary weapon (only shield), armor (only hide set), and the entire +16..+30 ADVANCED
  band is reachable but has almost nothing new to chase (gear is T2 or nothing until +31).
- **Blocked by intentional gates:** Dawnreach (`+31` + catalyst) and ASCENDANT (no token —
  deliberate).
- **Missing meaningful goals:** after `q_light_beyond` the game ends; there is no content
  above T2 gear until the transcendent gate, and nothing beyond it. Marks accumulate with
  few compelling sinks. There are no long-horizon objectives in the T3/T4 space.

**Net:** the game is a strong vertical slice and an honest early game, but the mid-game is
a corridor. Pack 01 must convert that corridor into a connected mid-game.

---

# TASK 3 — CONTENT GAPS (design space, not solutions)

1. **Regions without identity.** Stormreach (1 node) and Dawnreach (capstone) are skeletal.
   No region has a mechanic beyond modifiers.
2. **Thin resource chains.** Wood/pelt/harvest chains terminate at T1–T2. No leather,
   glass, gem, or advanced alloy chains. No mid/late raw materials that feed both gear and
   enhancement.
3. **Lifeskill depth.** Herbalism has no dedicated node; no accessories/jewelry; no
   leather/tailoring line; no crafting for the unused GLOVES/BOOTS/ACCESSORY slots.
4. **Equipment gap.** No T3 base gear; only one real weapon upgrade (bronze→steel). No
   ranged/secondary variety, no gloves/boots/accessories.
5. **Worker specialization gap.** `CRAFTER` is inert; no regional affinity; no
   rare-resource specialist; retainers cannot process.
6. **Economic gap.** No endgame Marks sinks; `demandPressure` dead; no per-region markets;
   nothing expensive and *desirable* beyond the Ward/Catalyst.
7. **Quest gaps.** No enhancement-quest in the mid-game; no exploration quests beyond the
   grotto; no repeatable objectives; nothing teaching ADVANCED enhancement.
8. **Enhancement progression gap.** The +16..+30 stretch and the +31 gate are a dead zone —
   no incremental gear or material path between Steel Sword and Transcendent.
9. **Lack of long-term goals.** Nothing after `q_light_beyond`; no recursive/prestige layer;
   no seasonal/rotational goal.
10. **Progression dead zones.** Emberreach→Stormreach (T2 gear into a T4 boss with nothing
    between) and Stormreach→Dawnreach (+31 grind with no new gear) are the two worst spots.

---

# TASK 4 — LONG-TERM ROADMAP (Packs 01–06+)

Versioning (from expansion master prompt §27): `0.1.0-slice` → `0.2.0` (Pack 01) →
`0.3.0` (Pack 02) → … Each pack ends with tests green + APK build + emulator playthrough +
report, then **STOP** for ChatGPT review.

## Pack 01 — Outer Reaches (`0.2.0`)
- **Purpose:** turn the corridor between Emberreach and the +31 gate into a connected
  mid-game; give every unused gear slot a family; make the ADVANCED band worth chasing.
- **Player progression:** Bronze/Steel → Cindervale T2.5 gear → Stormreach feasibility →
  Frostreach T3.5 gear → a real path into +16..+31 → readiness for Dawnreach.
- **Major systems:** 2 new Reaches (Cindervale, Frostreach) + Stormreach deepening; new
  processing chains (leather, glass, alloys); Jewelry crafting skill; accessory/glove/boot
  gear families; rare-regional resource mechanics; 2 worker specializations; enhancement
  material depth (frostvein as a second ADVANCED source + a mid-tier ward).
- **Dependencies:** content catalogs only; one new crafting skill; no schema change required
  (see Task 6).

## Pack 02 — Mastery (`0.3.0`)
- **Purpose:** deepen lifeskills, workers, industry, specialization.
- **Progression:** skill mastery milestones/perks per skill; worker specialization trees;
  production buildings.
- **Major systems:** mastery perks (per-skill, not flat), retainer trait trees + regional
  affinity, workshop/processing nodes for `CRAFTER`-type workers, refined crafting (batch
  bonus, quality tiers).
- **Dependencies:** Pack 01 content base; requires new simulation logic (mastery perks,
  retainer processing) and likely a `WORKSHOP` node type + `ActivityType` (schema v2).

## Pack 03 — World Depth (`0.4.0`)
- **Purpose:** expand regions/combat/economy/exploration.
- **Progression:** 1–2 further Reaches (T4/T5), larger monster ecosystems, elites, deeper
  economy.
- **Major systems:** new regional mechanics (dangerous gathering, region markets), elites +
  rare encounters, per-region market drift, more side/exploration quests, repeatable
  objectives.
- **Dependencies:** Pack 02; requires market model changes (per-region drift) → schema v2/v3.

## Pack 04 — Transcendence (`0.5.0`)
- **Purpose:** build a real ecosystem around the existing Transcendent band (do not
  duplicate it).
- **Progression:** transcendent-specific gear, catalyst economy, high-end bosses, long-term
  objectives (e.g., "ascend N items", "perfect a +45").
- **Major systems:** transcendent equipment families, catalyst farming loops, high-end
  content in Dawnreach+.
- **Dependencies:** Pack 03; mostly content + one or two new mechanics.

## Pack 05 — Ascendancy (`0.6.0`)
- **Purpose:** implement the ASCENDANT band's token gate and the first soft-prestige layer —
  only after +31..+45 is proven enjoyable.
- **Progression:** a recursive layer consuming max-tier gear/resources for permanent small
  multipliers + a new resource tier.
- **Dependencies:** Pack 04; requires `tier:ascendant` to be granted by a real chain, a
  prestige resource, and schema v4 (permanent multipliers, prestige counters). Explicitly
  **not** before Packs 01–04.

## Pack 06+ — Eternal / Recursive (`0.7.0+`)
- **Purpose:** effectively unlimited progression without a hardcoded ceiling.
- **Progression:** content-driven region expansion, procedural/multiplier-based new Reaches,
  new mastery layers, recursive tiers (Tier A → B → C … consuming prior output).
- **Major systems:** data-driven region templates, prestige layers, cross-layer goals.
- **Dependencies:** the data-first architecture already in place; new model fields for
  recursive layers (schema versioning + migration registry).

Design rule carried through all packs: every pack adds **at least one new sink and one new
source per system** and connects ≥ 2 existing systems (anti-bloat Rules A–J from the
expansion master prompt). Additionally, the **justification rule** (§5.0) applies to every
pack: content must justify itself through progression or system interaction — never add
filler merely to satisfy numerical content targets. Numerical targets in §17 of the
expansion master prompt are ceilings of intent, not quotas.

---

# TASK 5 — FULL DESIGN OF PACK 01 — OUTER REACHES

## 5.0 Design principles applied

- Insert Cindervale and Frostreach **without breaking the existing chain**: existing quests,
  tokens, and rewards stay byte-identical; new regions are gated by *new* tokens granted by
  *new* quests. Existing regression tests must pass unchanged.
- No new regions are reached by re-pointing existing quest rewards.
- Every new resource has ≥ 2 sinks (craft + sell + quest and/or enhancement).
- Every new equipment family occupies an unused or under-used design space (slots, stat
  profiles, material sources), not a bigger-number duplicate.
- Two new regions → one deliberately above and one below the existing Stormreach tier, so
  the corridor is filled from both sides.
- A single new crafting skill (**Jewelry**) is introduced only because accessories need a
  crafting home; leather tanning reuses **Drying** and armor work reuses **Armorcraft**
  (anti-bloat: no skill added without a decision it creates).
- **Justification rule:** *Content must justify itself through progression or system
  interaction. Do not add filler merely to satisfy numerical content targets.* A resource,
  recipe, monster, node, worker, or quest is only included if it creates a player decision
  or feeds a neighboring system (see §5.4 integration and the §5.5 ladder).
- **Regional-mechanic rule:** *Regional mechanics must remain small, legible player-facing
  rules. Do not create an overly generic modifier framework that turns every future region
  into a collection of opaque stat multipliers.* Each Pack 01 mechanic (Smoldering Veins,
  Crystal Snow + hazard, Stormreach's existing yield×1.5 and the processing-speed flavor)
  is a single, named, player-visible rule — not a bucket of hidden multipliers. Any new
  `RegionDefinition` field must be named, capped, and explained in node/help text.

## 5.1 Region: Cindervale (Tier 2) — "the ash-floored highland"

- **Identity:** a scorched, wind-scoured highland beyond Emberreach where the ground smokes
  and buried glass glows at dusk. A broken, beautiful middle country.
- **Purpose:** the T2.5 gear + material bridge that makes Stormreach's T4 Herald feasible.
  Gives Emberreach a second reach to grow into and the player a reason to re-visit
  processing/crafting at scale.
- **Regional mechanic — "Smoldering Veins":** every Cindervale node's yield table carries a
  small guaranteed-or-highly-weighted chance of the signature rare **emberglass**; the
  region also yields 10% more from MINE nodes (a second, distinct mechanic: **Glasscountry
  Flux**). Requires a new `RegionDefinition` field (see Task 6).
- **Unlock condition (no circular gate):** `region:cindervale`, granted by new quest
  `q_cindervale` whose **only** prereq is `q_emberreach_ash` (Emberreach cleared). The
  unlock must NOT require any Cindervale-internal node. Progression is:
  `q_emberreach_ash → q_cindervale → unlock region:cindervale → enter/discover Cindervale
  → discover Ashveil Market → continue Cindervale progression`. This runs parallel to the
  existing Stormreach unlock and does not touch it.

### Nodes (5)

| Node | Type | Skill | Resources | Notes |
|---|---|---|---|---|
| Cinderlode Quarry | MINE | Mining | cinder_ore, ore_iron, emberglass (rare) | |
| Sootbark Woods | FOREST | Logging | sootwood, wood, emberglass (rare) | |
| Sunbaked Shallows | FISHERY | Fishing | ashfish, fish_carp, saltash | |
| Cindervale Terraces | FARM | Farming | ashgrain, grain | |
| Ashveil Market | TRADE_HUB | — | — | discovery + regional market flavor |

### Resources (all with ≥ 2 sinks)

- **cinder_ore** (T2 resource): → (grind) cinder_fragment → (smelt) cinder_ingot →
  (with steel) cinder_steel → T2.5 weapons/armor; sellable.
- **emberglass** (T2 rare regional): → (heating) glass_shard → alchemy potions + (Jewelry)
  ember accessories + Ward-of-Cinders (ADVANCED enhancement consumable); quest objective.
- **sootwood** (T2): → (heating) soot_lumber → carpentry (gloves/bow/structures) + tools.
- **ashfish / saltash** (T2): → (drying) ashcured fish; saltash → cooking/alchemy;
  sellable.
- **ashgrain** (T2): → (milling) ash_flour → bread variants + alchemy binder; sellable.

### Processing chains (explicit)

```
cinder_ore
  → [Grinding] cinder_fragment
  → [Smelting] cinder_ingot
  → [Refining] cinder_steel          (cinder_ingot ×2 + coal)
  → Cindervale weapons / armor / tools / jewelry band

emberglass
  → [Heating] glass_shard
  → [Alchemy] Ember Salve / Smoldering Draught
  → [Jewelry] Ember Ring / Ashveil Amulet
  → [Engineering] Ward of Cinders (ADVANCED full-negation)

sootwood → [Heating] soot_lumber → [Carpentry] Ashbark Gloves / Cinder Longbow / structures

ashgrain → [Milling] ash_flour → [Cooking] Hearth Bread
ashfish → [Drying] Ashcured Fish
saltash → [Cooking] Salted Boar / [Alchemy] reagent
```

### Combat

- **Families:** Cinder Hound (T2, fast/low-def), Scorch Viper (T2, venom-profile: accuracy),
  Ember Jackal (T2.5, pack: crit), **Cinder Wraith** (T3 elite, rare encounter).
- **Boss — Ash Sovereign (T3, 420 HP):** guards the Glass-Pass; guaranteed loot
  `cinder_core` (new T2.5 material) + Voidforged Crystal; unlocks `recipe:glasswork` and the
  Cindervale regional capstone quest.
- **Loot progression:** adds cinder materials + a 4% `emberglass` rare drop + Ward-of-Cinders
  chance, feeding the enhancement ecosystem.

### Equipment (Cindervale family — T2.5)

- **Weapons:** Cinder Sword (MAIN_WEAPON, atk 26 / acc 9 — between Steel and Frost),
  Cinder Longbow (SECONDARY_WEAPON, crit/accuracy profile), Ashbark Shield (SECONDARY).
- **Tools:** Cinder Pickaxe (TOOL, mining +30%).
- **Armor:** Ashbark Gloves (**GLOVES** slot, first use), Ashbark Boots (**BOOTS**, first
  use), Cinder Breastplate (CHEST), Cinder Hood (HELMET).
- **Accessories:** Ember Ring (**ACCESSORY_1**, first use), Ashveil Amulet (ACCESSORY_2).
- **Enhancement relationship:** all enhanceable in BASE band; cinder materials feed
  ADVANCED-band consumables (Ward of Cinders).

### Workers

- **New recruit — Pella (FORGER), cost 950, unlock `region:cindervale`:** a **gathering**
  specialization useful at SPECIAL/industrial resource nodes (Cinderlode Quarry, emberglass
  glass-veins, and workshop-flavor SPECIAL nodes such as Frostforge Foundry), with strong
  gathering bonuses (`gatheringSpeed 1.15`, luck 1.05). FORGER does **NOT** imply retainer
  crafting or processing in Pack 01 — retainers remain gathering-only. Actual retainer
  processing/crafting is explicitly deferred to Pack 02.
- **New specialization — PROSPECTOR:** high rare-yield affinity (emberglass/frostvein);
  implemented as a `RetainerRecruitDef` + trait pool entry (no model change; see Task 6).
  Also gathering-only in Pack 01.
- **Regional affinity (data-only):** MINER recruits gain +5% in their home region, derived
  from specialization→region mapping in content (no schema change).

### Quests

- **Main:** `q_cindervale` (enter/discover Cindervale → `region:cindervale` — no internal
  Cindervale node is required to unlock the region);
  `q_cinder_king` (defeat Ash Sovereign → `recipe:glasswork`, `recipe:jewelry`).
- **Regional/exploration:** discover Ashveil Market as the first in-region step after
  entering (a DISCOVER_NODE objective, fully inside unlocked Cindervale); discover every
  remaining Cindervale node; kill each family; elite hunt.
- **Lifeskill:** `q_tanner` (craft full leather set), `q_jeweler` (craft Ember Ring),
  `q_glazer` (mill glass).
- **Industry:** `q_cinder_steel` (forge cinder steel), `q_ash_cure` (ashcured provisions).
- **Enhancement goal:** `q_advance_begin` (reach +5) — the first explicit enhancement teach,
  and the first rung of the Pack 01 checkpoint ladder (see §5.5).

### Economy

- Market identity: Ashveil Market buys raw Cindervale goods slightly above base
  (`priceModifier 1.6`) but sells processed goods at a premium — reinforces
  process-before-sell.
- **Sinks:** Ward of Cinders (craft cost), Cindervale gear buy prices, jewelry materials.
- **Sources:** new node yields, boss `cinder_core`, sell-vs-process arbitrage.

## 5.2 Stormreach deepening (Tier 3) — existing region, made substantial

Stormreach currently has 1 node + 1 boss. Pack 01 fills it (no chain change).

- **Regional mechanic (already present):** yield ×1.5. Keep. Add flavor: Stormreach
  processing runs are 10% faster (new processing-eff bonus field — see Task 6).
- **Nodes added (4):** Stormscale Quarry (MINE, storm ore + coal), Thunderwood (FOREST,
  thunderwood + hardwood), Stormfield (FARM, stormgrain), Deepstorm Spire (SPECIAL, rare
  stormcrystal).
- **Monsters added (3 + 1 elite):** Storm Boar (T3), Lightning Heron (T3), Storm Ape
  (T3.5), **Stormcaller Elite** (T4 rare). Existing Storm Herald boss unchanged.
- **New chain:** storm_ore → storm_steel; thunderwood → storm_lumber; stormcrystal →
  mid-tier enhancement material + weather-proof gear; a `q_stormreach_expeditions` side
  chain rewarding exploration of the new nodes.

## 5.3 Region: Frostreach (Tier 3) — "the high fells"

- **Identity:** a glacial highland where metal grows cold and old things move under the ice.
  The game's first cold, unforgiving Reach.
- **Purpose:** fill the dead zone between Stormreach and the +31/Dawnreach gate. Provides
  the **frostvein** line (second ADVANCED-band material), the **frostforge** T3.5 equipment
  family, and a mid-game enhancement consumable, so the road to +31 has content.
- **Regional mechanic — "Crystal Snow":** Frostreach nodes/processing have a chance to
  yield **frostvein crystal** (rare), and Frostreach gathering carries a **hazard**: each
  action deals 1 chip damage to the character (a new `hazard` regional flag — new sim
  logic, see Task 6). Trades output for risk; potions/food matter here.
- **Unlock condition (no circular gate):** `region:frostreach`, granted by new quest
  `q_frostreach` whose **only** prereq is `q_stormreach_herald` (Herald cleared). Progression
  is: `q_stormreach_herald → q_frostreach → unlock region:frostreach → enter/discover
  Frostreach → gather frostvein → continue Frost progression`. Gathering frostvein must
  never be required before Frostreach is unlocked. Parallel to, and feeding, the existing
  `q_light_beyond`.

### Nodes (5)

| Node | Type | Skill | Resources | Notes |
|---|---|---|---|---|
| Frostvein Quarry | MINE | Mining | frost_ore, cinder_ore, frostvein (rare) | hazard |
| Iceshard Woods | FOREST | Logging | icewood, sootwood, frostvein (rare) | hazard |
| Frozen Mere | FISHERY | Fishing | glacefish, ashfish | hazard |
| Highland Tundra | — (HERBALISM node, FOREST type) | Herbalism | frostmoss, brightleaf, frostvein (rare) | gives Herbalism its first dedicated node |
| Frostforge Foundry | SPECIAL | — | — | workshop-flavor node for frostforge crafting unlocks |

### Resources & chains (explicit)

```
frost_ore → [Grinding] frost_fragment → [Smelting] frost_ingot
  → [Refining] froststeel (frost_ingot ×2 + cinder_steel)
  → Frostforge weapons / armor / tools
icewood → [Heating] frost_lumber → [Carpentry] frost shield / cold tools
glacefish → [Drying] smoked glacefish
frostmoss → [Alchemy] Frost Draught / [Jewelry] frostvein ring
frostvein crystal → [Enhancement] second ADVANCED-band material
  + [Engineering] Ward of Frost (ADVANCED full-negation)
```

### Combat

- **Families:** Frostmaw Wolf (T3), Ice Lynx (T3, evasion), Frost Titan (T4 elite).
- **Boss — Glacier Warden (T4, 780 HP):** grants `recipe:frostforge`, a batch of frostvein,
  and the `q_frost_warden` capstone; feeds directly into the +31 preparation (froststeel
  weapon at T3.5 makes the transcendent stretch practical).

### Equipment (Frostforge family — T3.5)

- Frostblade (MAIN_WEAPON, atk 34 / acc 11), Frost Shield (SECONDARY), Frost Gauntlets
  (GLOVES), Frostgreave Boots (BOOTS), Frostplate (CHEST), Frost Hood (HELMET),
  Frostvein Ring (ACCESSORY_1), Icebound Amulet (ACCESSORY_2), Frostforge Pickaxe (TOOL,
  mining +35%).
- **Enhancement relationship:** the first gear family *intended* to be pushed into ADVANCED
  (+16..+30); frostvein becomes the accessible ADVANCED material so the +16..+31 stretch is
  a real path, not a wall.

### Workers

- **New recruit — Runa (PROSPECTOR), cost 1400, unlock `region:frostreach`:** luck 1.3,
  gatheringSpeed 1.1 — the rare-resource specialist.
- PROSPECTOR specialization pool: traits favoring rare yields.

### Quests

- **Main:** `q_frostreach` (enter/discover Frostreach → `region:frostreach` — no frostvein
  required to unlock; gathering frostvein is the *in-region* goal that follows);
  `q_frost_warden` (defeat Glacier Warden → `recipe:frostforge`, `region:dawnreach`-prep
  materials).
- **Lifeskill/industry:** `q_froststeel`, `q_frost_draught`, `q_herbalist_highland`
  (first Herbalism quest), `q_frostvein_enhance` (reach +16 using frostvein — teaches the
  ADVANCED band, second ladder rung).
- **Enhancement checkpoints:** `q_advance_begin` (+5) → `q_frostvein_enhance` (+16) →
  `q_advance_mid` (+20) → `q_advance_near` (+30) → the existing `q_light_beyond` (+31).
  Each is a meaningful intermediate goal (material teaching, gear milestone, reward),
  not an arbitrary quest number (see §5.5).

### Economy

- `priceModifier 2.0` (matches Stormreach tier), processed premium reinforced.
- **Sinks:** Frostforge gear buy prices, frostvein-enhancement costs, Ward of Frost,
  Frost Draughts.
- **Sources:** new node yields, Glacier Warden loot, frostvein.

## 5.4 Cross-region integration summary

- **Chain:** Emberreach → (parallel) Cindervale → Stormreach (deepened) → Frostreach →
  +16..+30 (ADVANCED) → +31 (TRANSCENDENT, unchanged) → Dawnreach. All existing quest
  rewards and tests remain untouched; the +31 gate is preserved (see §5.5).
- **Region gates (no circular dependency):** Cindervale and Frostreach are each unlocked by
  a quest whose prereq is an already-cleared prior region (Ash-Spawned Brutes and Storm
  Herald respectively). No internal node of a locked region is ever required to unlock that
  region; Ashveil Market (Cindervale) and frostvein (Frostreach) are in-region goals reached
  after the unlock.
- **Enhancement path:** Steel T2 → Cinder T2.5 → Frost T3.5, with frostvein opening a real
  +16..+30 climb and a second full-negation consumable per ADVANCED tier.
- **Slots:** GLOVES, BOOTS, ACCESSORY_1, ACCESSORY_2 go from unused to populated.
- **Lifeskills:** Herbalism gets a home; Drying gains tanning/leather; Jewelry added as the
  one new crafting skill; every skill sees new recipes.
- **Workers:** +2 specializations (PROSPECTOR, FORGER), +2 recruits, data-only regional
  affinity.
- **Economy:** +2 meaningful sinks per region, +2 new regions of sources, no runaway: rare
  resources are capped by per-action chance and bounded offline simulation.

## 5.5 Intended progression ladder & enhancement checkpoints

The intended Pack 01 progression ladder is fixed and must not be weakened or bypassed:

```
T1  Hollowreach
 → T2   Emberreach
 → T2.5 Cindervale
 → T3   Stormreach (deepened)
 → T3.5 Frostreach
 → ADVANCED  +16..+30
 → TRANSCENDENT +31   (existing q_light_beyond gate — MUST NOT be weakened or bypassed)
 → Dawnreach
```

Enhancement checkpoints are meaningful intermediate goals, not arbitrary numbers. Each rung
teaches a material, unlocks a reward, or bridges to the next region, and leads up to the
unavoidable +31 gate:

```
+5  (q_advance_begin)   — teaches enhancement in Cindervale
 → +16 (q_frostvein_enhance) — frostvein opens the ADVANCED band
 → +20 (q_advance_mid)  — Frostforge gear milestone
 → +30 (q_advance_near) — pre-gate preparation (frostvein + catalyst prep)
 → +31 (q_light_beyond) — existing TRANSCENDENT gate; unchanged and required
```

No Pack 01 content may grant, skip, or soften the +31 requirement. Dawnreach remains gated
by `q_light_beyond` exactly as implemented today.

Scope tallies vs expansion master prompt §17 targets: 2 new Reaches + 1 deepened (target
2–3), 14 new nodes (target 8–12 — slightly above, all meaningful), ~11 new monsters +
2 elites + 2 bosses (target 15–25/3–5; we prefer density over quota), ~10 processing
recipes (target 15–30 — trimmed to chains that matter), ~24 crafting recipes (target
20–40), 2 workers (target 6–12 — added only where specialization earns a slot), quests
and economy per region. Where a target is not hit, the reason is anti-bloat: no filler
(see design rule in §5.0).

---

# TASK 6 — ARCHITECTURE CHECK

## 6.1 Can already be supported by existing architecture (content/data only)

- New regions, nodes, monsters, items, recipes, quests, retainer recruits, traits, and
  enhancement tables — all catalogs are data objects read by generic engines. The UI
  renders regions/nodes/monsters/recipes/quests/skills/gear **generically** from state.
- Unused gear slots (GLOVES/BOOTS/ACCESSORY_1/2) — pure content; `EquippedRow`/`PackItemRow`
  render whatever is equipped.
- New skills (Jewelry) — `Skills` catalog + `Character.skillXp` map; Stats tab iterates
  skills generically.
- Cindervale/Frostreach monsters, bosses, loot, nodes — data.
- New resource chains, processing/crafting recipes — data; `ActivityEngine.advanceRecipe`
  handles them; offline simulation flows through the same path.
- Regional `priceModifier`/`yieldMultiplier` values — data.
- Second ADVANCED material (frostvein) — data (enhancement tables + item defs).

## 6.2 Requires only content/data (list)

- All of §6.1.
- New quests (main/side/regional/lifeskill/industry/enhancement) — generic `QuestEngine`.
- New sell-vs-process advice — `MarketService.sellAdvice` is generic over recipes.
- New worker recruits/traits — data.

## 6.3 Requires new simulation logic

| Change | Why | Affected modules | Risk | Compatibility |
|---|---|---|---|---|
| Regional **mechanic fields** on `RegionDefinition` (e.g., `rareYield: RareYieldConfig?`, `hazardPerAction: Double`, `processingSpeedMultiplier`) | Smoldering Veins / Crystal Snow / Storm processing bonus are per-region rules today's two modifier fields cannot express | `core-content` (data), `core-simulation` (`Rates`, `ActivityEngine` for hazards) | Low — additive fields with defaults; existing regions unchanged | `RegionDefinition` is content, not serialized save state → **no schema change** |
| **Hazard damage** in gathering (Frostreach chip damage) | regional identity | `ActivityEngine.advanceGathering` + `CombatStatsMath` health | Low–moderate; must respect HP floor and offline determinism | No save change |
| **PROSPECTOR rare-yield affinity** | worker specialization decision | `Rates.retainerActionsPerHour` / yield roll | Low — additive multiplier from content mapping | No save change |
| **FORGER SPECIAL/industrial-node affinity** | gives FORGER a gathering identity (SPECIAL/industrial resource nodes) without retainer processing | content mapping spec→node-type + `Rates` | Low — additive; gathering-only in Pack 01 | No save change |
| **Regional affinity** for workers | worker optimization | content mapping spec→region + `Rates` | Low | No save change |

## 6.4 Requires model changes

| Change | Why | Risk | Compatibility |
|---|---|---|---|
| **`RetainerSpecialization`** adds `PROSPECTOR`, `FORGER` | new worker identities — both are **gathering** specializations in Pack 01 (SPECIAL/industrial node affinity + gathering bonuses); they do **not** enable retainer processing/crafting, which is deferred to Pack 02 | Low — enum in `core-model`; saved as name string in JSON, migration tolerant (unknown enum → keep string; add mapping) | `core-model` enum is `@Serializable`; adding enum entries is backward-compatible for existing saves (values stored by name) |
| **`EnhancementBand`** — none needed | bands are data; ADD a new *content* band only if a future pack wants it (not in Pack 01) | — | — |
| New **regional mechanic** config could live on `RegionDefinition` (content) rather than `GameState` | keeps save schema stable | — | — |

**Deliberate decision:** Pack 01 avoids adding new serialized save fields, so
`SaveSchema.CURRENT` stays **1** and no migration is required. The only `core-model` change
is adding enum entries to `RetainerSpecialization`, which is compatible with the existing
JSON (serialized by name).

## 6.5 Requires persistence/schema changes

- **None required for Pack 01**, provided §6.4's enum-only approach is used and regional
  mechanics live on content definitions, not on `GameState`.
- If a future decision wires `demandPressure` (currently dead) or adds per-region market
  drift, that is a **schema v2** migration (add fields with defaults; migration registry
  entry). Deferred deliberately.

## 6.6 Requires UI changes

- **Minimal, all data-driven:** new regions/nodes/monsters/recipes/quests/gear render
  through existing generic screens.
- **Specific small edits:** (a) `CharacterScreen` already shows any equipped slot — no
  change for gloves/boots/accessories; (b) `EconomyScreen` needs no change unless we add
  region tabs (deferred); (c) a **hazard warning** line on Frostreach node cards (small,
  additive); (d) Jewelry skill appears automatically in the Stats tab.
- **No screen redesign.**

## 6.7 Net assessment

Pack 01 is **~80% content/data, ~15% small additive simulation logic, ~5% UI labels**,
with **no save-schema change**. This is the intended shape: the architecture was built
data-first precisely so this pack is low-risk. The only areas needing disciplined
engineering are the regional-mechanic fields and Frostreach hazard (offline determinism +
HP floor safety) and the two new `RetainerSpecialization` enum entries.

---

# TASK 7 — BALANCE RISKS & SAFEGUARDS

1. **Rare-resource bottleneck (emberglass/frostvein).** If rare chances are too low, the
   chain stalls; too high, it trivializes.
   *Safeguard:* rare yields are per-action weighted chances (e.g., 8–15%), visible in node
   text, and quests award a guaranteed seed batch. Offline sim uses expected-value math with
   bounded variance — no infinite farm, no zero-yield streak longer than design tolerance.
2. **Excessive waiting.** New chains add craft time.
   *Safeguard:* reuse short cycle times (3–8 s); cap total chain length at 4 hops; batch
   recipe actions already aggregate in offline sim. Pacing review at playtest, not
   inflation.
3. **Runaway economy.** Processed premium could make Cindervale/Frostreach goods
   always-profitable.
   *Safeguard:* `priceDrift` moves against repeated sells; sinks (gear, wards, workers)
   scale with new income; add per-region buy/sell advice. Rebalance only with data, never
   formula churn.
4. **Worker dominance.** Retainers could become the only gathering source.
   *Safeguard:* stamina cap keeps long-run worker output below player rate; PROSPECTOR
   affinity is additive not exponential; rare yields on worker output are capped by the same
   per-action chance. FORGER/PROSPECTOR grant gathering bonuses only — they cannot replace
   the player's crafting/processing role (retainer processing is Pack 02).
5. **Useless resources.** Every new resource has ≥ 2 sinks by design (§5.1–5.3); content
   integrity tests assert ≥ 1 crafting use + sellable or quest use.
6. **Useless equipment.** Cinder (T2.5) must beat Steel (T2) meaningfully but not obsolete
   Frost (T3.5); Frost is the ADVANCED-band home. A test asserts monotonic stat progression
   and that no new item is a pure sidegrade with identical stats.
7. **Enhancement inflation.** Adding a second ADVANCED material (frostvein) must not cheapen
   the +16..+30 climb.
   *Safeguard:* frostvein is rare; Ward of Frost is expensive to craft; the +31 gate still
   requires a Catalyst. No probability-table changes.
8. **Progression walls.** The +31 gate is the current wall; Pack 01 fills the approach with
   incremental gear but does **not** remove the gate (it is intentional).
   *Safeguard:* new quests grant intermediate goals (reach +5, +16, +20) so the stretch has
   checkpoints.
9. **Dead zones.** Stormreach (1 node) and Dawnreach (capstone) are explicitly filled by
   §5.2/§5.3 and future packs.
10. **Offline exploits.** Hazard damage and rare yields must be deterministic and bounded in
    the offline sim.
    *Safeguard:* `Rates`-shared math; offline tests assert no negative HP and no unbounded
    rare yield at long elapsed.
11. **Complexity creep.** Two new regions + a new skill is already a large pack.
    *Safeguard:* pack gates: region 1 (Cindervale) fully tested before region 2
    (Frostreach); Jewelry added only with the accessory family, never alone.

---

# TASK 8 — IMPLEMENTATION ORDER (Pack 01)

Dependency-aware sequence derived from the repository's data-first architecture. Each step
ends green before the next begins.

1. **Foundation — content schema additions**
   `RegionDefinition` mechanic fields (rare-yield config, hazard, processing multiplier)
   with defaults; `RetainerSpecialization` enum entries (PROSPECTOR, FORGER). No behavior
   yet. Core-content + core-model only.
2. **Integrity guardrails** — extend `ContentIntegrityTest` first (unknown-id, chain, loot
   sums, slot, monotonic-stat invariants) so every later step is validated as it lands.
3. **Region 1 foundation — Cindervale**: region def, nodes (5), items/resources, sellable
   values.
4. **Cindervale processing/crafting** recipes + chains (grind/smelt/refine/heating/alchemy/
   jewelry/engineering), then **resource-chain tests**.
5. **Cindervale equipment family** (weapons/tools/armor/accessories) + **gear-progression
   tests**.
6. **Cindervale combat**: monsters, elite, boss, loot tables + **combat/loot tests**.
7. **Cindervale workers**: FORGER recruit, PROSPECTOR specialization, regional affinity +
   **retainer tests**.
8. **Cindervale quests** (main/regional/lifeskill/industry/enhancement) + **quest-chain
   tests**.
9. **Cindervale economy** (priceModifier, sinks/sources, sell-advice) + **market tests**.
10. **Stormreach deepening** (nodes, monsters, elite, side quests) + regional tests.
11. **Region 2 — Frostreach**: same order as 3→9 (foundation, chains, gear, combat, workers,
    quests, economy) including **hazard** simulation logic + **offline hazard tests**.
12. **Frostvein enhancement path** (+16 material, Ward of Frost, and the +5 → +16 → +20 →
    +30 checkpoint quests; the existing +31 `q_light_beyond` gate is untouched) +
    **enhancement tests**.
13. **UI passes**: hazard warning, jewelry/alchemy recipe labels, accessory slot display
    sanity; keep to additive text.
14. **Integration** — full JVM suite; resolve any cross-system interplay.
15. **Instrumented** critical-path tests (creation → Cindervale entry → craft → equip →
    fight → offline return).
16. **assembleDebug + emulator playthrough** (fresh save; reach Cindervale and Frostreach).
17. **Report + STOP** for ChatGPT review.

---

# TASK 9 — TEST STRATEGY (Pack 01)

Extend the existing JVM suite (137 tests) with:

- **Content integrity:** extend `ContentIntegrityTest` — every new node/recipe/quest/
  monster/enhancement-material/trait resolves; guaranteed loot sums to 100; every new
  resource has ≥ 2 sinks; no quest grants `tier:ascendant`; new gear stats are strictly
  progressive; regional mechanics fields validate.
- **Resource-chain tests:** cinder/emberglass/sootwood/ash/ice/frost chains convert with no
  loss/duplication; full-chain cost/output asserts.
- **Regional tests:** Cindervale/Frostreach unlock gating; Stormreach deepening quests;
  regional mechanics (yield, rare chance, processing bonus, hazard) applied player +
  retainer; Frostreach hazard respects HP floor and retreat/offline rules.
- **Quest-chain tests:** new chains from prereq to reward to next quest; ensure existing
  chain (through `q_light_beyond`) still resolves unchanged.
- **Combat/loot tests:** new monsters/bosses — expected kills, loot expectations, elite
  rarity, boss reward gating.
- **Processing/crafting tests:** all new recipes — fundability (mirroring the
  `IndustryStartRegressionTest` pattern), skill gates, unlock tokens, auto-end on
  exhaustion.
- **Worker tests:** PROSPECTOR/FORGER recruitment gating + cost, regional affinity math,
  stamina/trait interaction, offline worker output.
- **Economy tests:** price modifiers for the two new regions, processed premium, sell-advice
  for new chains, sink/source balance smoke (no negative marks, no unbounded drift).
- **Enhancement tests:** frostvein as ADVANCED material, Ward of Frost consume-on-fail,
  the +5/+16/+20/+30 checkpoint quests resolve in order and each is achievable at its rung,
  the existing +31 `q_light_beyond` gate is NOT bypassed or softened, and no change to
  BASE/TRANSCENDENT probabilities.
- **Offline simulation tests:** short + long absences with hazard (no HP < floor, no
  negative resources), rare-yield bounding at 12 h, retainer output correctness.
- **Persistence tests:** save round-trip still green with the enum additions (JSON by name);
  schema version still 1.
- **Instrumented critical-path:** extend `EternalCriticalPathTest` with a Cindervale
  entry + craft + equip + fight smoke on the API-36 emulator.

Final gates (per expansion master prompt §23): full JVM suite green, instrumented suite
green, `assembleDebug` builds, representative emulator playthrough passes, no known
release-blocking failure.

---

# SEPARATE OBSERVATIONS (documented during planning; NOT fixed)

Found while reading the repository — none is a runtime bug; all are hygiene or design gaps.
Per the planning constraints, they are recorded here and left alone:

1. **Duplicate `cook_roast` recipe id** in `Recipes.kt` (defined twice, identical). harmless
   (last wins) but should be de-duplicated during Pack 01 content cleanup.
2. **`MarketState.demandPressure` is never written** (dead field). Either wire it in a later
   pack (with schema v2) or document it as reserved.
3. **`CRAFTER` retainer `productionSpeed` has no simulation effect** — retainers only
   gather. Pack 01's FORGER/PROSPECTOR additions give specialization an actual node role via
   **gathering** bonuses (SPECIAL/industrial node affinity), keeping retainers
   gathering-only; true retainer-processing is a Pack 02 topic.
4. **Stormreach is a single-node region** (design gap, addressed in §5.2).
5. **Herbalism has no dedicated node** (design gap, addressed by Highland Tundra in §5.3).
6. **No gear exists for GLOVES/BOOTS/ACCESSORY slots** (addressed in §5.1/§5.3).

No functional or state-corruption bug was found during this planning pass; the release
candidate audit remains valid.

---

# FILES

- **Created:** `docs/eternal-expansion-roadmap.md` (this document), including the
  ChatGPT-approved correction pass (quest-gate fixes, FORGER clarification, progression
  ladder, enhancement checkpoints, design rules).
- **Not modified:** `project-eternal-opencode-prompt.md`,
  `project-eternal-eternal-expansion-master-prompt.md`, `docs/decisions.md`, and all source
  code. No gameplay, content, balance, formula, or schema changes were made.

---

# STATUS

**PLANNING COMPLETE — NO GAMEPLAY IMPLEMENTATION STARTED.**

Correction pass applied to the planning document only. Pack 01 implementation begins only
after ChatGPT reviews the corrected roadmap and explicitly authorizes it.

---

## IMPLEMENTATION STATUS — Pack 01 (Outer Reaches), `0.2.0`

Pack 01 is **implemented and verified** (see `docs/playtest-pack-01.md` and
`docs/decisions.md` §"Pack 01"). Design intent above is unchanged; where implementation
refined the design, the deviation is documented in `docs/decisions.md`:

- **Cindervale** and **Frostreach** implemented as specified (regional mechanics, nodes,
  chains, monsters, bosses, gear, workers, quests, economy).
- **Stormreach** deepened (4 nodes, 4 monsters/elites, expedition quest).
- **Jewelry** skill + ACCESSORY/GLOVES/BOOTS gear families implemented.
- **Enhancement**: frostvein added as an ADVANCED alternate material; +5/+16/+20/+30
  checkpoint quests added; the +31 gate is unchanged.
- **Deadlock audit** found and fixed three skill-level deadlocks (Refining, Engineering,
  Jewelry) — all content-data fixes; a ContentIntegrity guardrail now prevents recurrence.
- Full JVM suite **166 tests / 0 failures**; instrumented **3/3**; `assembleDebug` green;
  fresh-save emulator playthrough performed.

Pack 02 design (Mastery) is intentionally not started.
