# PROJECT ETERNAL — MASTER IMPLEMENTATION PROMPT FOR OPENCODE

## ROLE

You are the autonomous lead developer for **Project Eternal**, an offline Android RPG/idle-economy simulation game. You have full engineering authority: inspect the environment, choose the stack, scaffold the project, implement systems, write tests, build the APK, debug failures, and iterate — without stopping to ask permission for ordinary engineering decisions. Only pause if you hit a decision that would be irreversible at the product-vision level (e.g. abandoning offline-only play, adding a mandatory server). Everything else: decide, document the decision briefly in `/docs/decisions.md`, and keep moving.

Do not produce a design document. Produce a working, tested, buildable Android application, built incrementally, starting from a genuinely playable vertical slice.

---

## 1. PROJECT IDENTITY

- **Name:** Project Eternal
- **Genre:** Offline Android RPG / idle-economy simulator / life-simulation RPG
- **Inspiration (systems, not assets):** Black Desert Online's lifeskills/workers/nodes/enhancement, RuneScape's interconnected skill web, idle games' offline progression math, classic RPG questing and equipment progression
- **Hard constraint:** No copyrighted names, art, lore, UI, or code from any existing game. Original terminology throughout (see §4 for a starter vocabulary you may extend).
- **Platform:** Android only, fully offline, no account, no server, no mandatory ads, no forced-engagement energy systems.

---

## 2. PRODUCT VISION

The player starts as an ordinary adventurer with minimal gear and a small starting region, and grows into a character who runs an interconnected personal economy — fighting, gathering, processing, crafting, trading, and automating via workers — that keeps progressing even while the app is closed. The game must never feel like "a number goes up while you're away"; it must feel like "I'm building a system, and I need to keep deciding what it needs next."

---

## 3. DESIGN PILLARS

1. **Interconnection over isolation** — no system is a dead-end XP button; everything feeds something else (see the dependency graph in §17).
2. **Meaningful offline simulation** — elapsed time is mathematically simulated, not brute-force tick-replayed, and always ends in a legible report.
3. **Player agency** — at every stage the player chooses what to fight, gather, process, craft, enhance, assign, or sell. No single dominant strategy.
4. **Long-horizon, non-inflationary progression** — infinite progression via recursive tiers and new mechanics introduced at thresholds, not via multiplying the same stat forever.
5. **Save integrity above all** — this is an offline-only game; a corrupted save is the single worst failure mode and must be engineered against explicitly.
6. **Vertical slice first** — a small, complete, interconnected loop beats a wide, hollow one.

---

## 4. STARTER VOCABULARY (original terms — extend as needed, keep consistent)

- Player class: **Wayfarer**
- Currency: **Marks**
- Enhancement resource: **Resonance Shards**
- Enhancement failstack mechanic: **Resolve**
- Regions: **Reaches** (e.g. "Hollowreach", "Emberreach")
- Node network hub: **Waypost**
- Worker units: **Retainers**
- Top-tier prestige layer: **Ascendancy**

---

## 5. TECHNICAL ARCHITECTURE

### 5.1 Stack decision (verify against the actual environment before committing)

Inspect the environment first (`which`/`--version` checks for Android SDK, Gradle, JDK, Kotlin). Default preference unless the environment dictates otherwise:

- **Language:** Kotlin
- **UI:** Jetpack Compose (Material 3), single-Activity, Compose Navigation
- **Concurrency:** Kotlin Coroutines + Flow
- **Persistence:** Room (SQLite) for structured state, DataStore for settings/small flags
- **DI:** Hilt (or manual factory injection if Hilt setup proves environment-hostile — document the choice)
- **Build:** Gradle (Kotlin DSL), Android Gradle Plugin, target modern `minSdk`/`targetSdk` current at implementation time (verify current stable Android API level rather than assuming)
- **Testing:** JUnit5/JUnit4 + kotlinx-coroutines-test for engine logic, Compose UI tests for critical screens, Robolectric optional for fast unit tests of Android-dependent classes

### 5.2 Module separation (mandatory — do not build one giant UI module)

```
:app                 — Application, DI wiring, navigation shell
:core-model          — Pure Kotlin data classes (no Android deps): Character, Item, Node, Worker, Quest, etc.
:core-simulation     — Pure Kotlin simulation/progression engine, offline calculator, combat resolver, enhancement resolver
:core-persistence    — Room DB, DAOs, save versioning/migration, serializers
:core-content        — Static/seed game content (monsters, recipes, regions, quests) as data, not hardcoded logic
:feature-*           — One module per major UI screen area (combat, gathering, crafting, workers, market, quests, offline-report), each depending only on core-model + core-simulation contracts
```

`:core-simulation` must be testable with zero Android dependencies and zero UI dependencies. This is the module correctness of the whole game hinges on — treat it as the product.

### 5.3 Determinism

- All random rolls (loot, enhancement success, rare events) go through a single seeded `RandomSource` abstraction so simulations are reproducible in tests.
- Offline simulation for a given (state, elapsed-time) pair must be deterministic given the same seed sequence.

---

## 6. DATA MODELS (minimum viable set — expand as systems are added)

Define these as immutable/versioned Kotlin data classes in `:core-model`. Each entity needs a `schemaVersion` field.

- `Character(id, name, level, xpBySkill: Map<SkillId, Long>, stats: CombatStats, equipment: Map<EquipSlot, ItemInstance?>, currentActivity: ActivityState?, marks: Long, ...)`
- `ItemInstance(itemDefId, rarity, enhancementLevel, durability, boundProperties)`
- `ItemDefinition(id, name, slot, baseStats, tier, requiredMaterials, ...)` — content data, not per-save
- `SkillProgress(skillId, xp, level, masteryPerks)`
- `ActivityState(type: Combat|Gathering|Processing|Crafting, targetId, startedAt, ratePerHour, ...)`
- `Retainer(id, level, gatheringSpeed, productionSpeed, stamina, luck, specialization, assignedNodeId?)`
- `WorldNode(id, type: Mine|Forest|Farm|Fishery|Workshop|..., tier, unlocked: Boolean, assignedRetainerIds)`
- `RecipeDefinition(id, inputs: List<ResourceStack>, outputs: List<ResourceStack>, skillRequired, timeSeconds)`
- `QuestDefinition(id, type, prerequisites, objectives, rewards)`
- `QuestProgress(questId, objectiveStates, status)`
- `MarketListing(resourceId, basePrice, regionalModifier, demandCurveState)`
- `SaveGame(schemaVersion, character, inventory, retainers, nodes, quests, market, lastSavedAtEpochMs, checksum)`

All content-defining data (monsters, recipes, items, regions, quests) lives as data in `:core-content` (JSON or Kotlin object tables), not scattered through logic — this is what lets you add regions/tiers later without rewriting systems.

---

## 7. SIMULATION MODEL & OFFLINE PROGRESSION ALGORITHM

### 7.1 Real-time loop (app foreground)
Standard tick loop (e.g. 1 Hz) driving `ActivityState` progress, using the same rate functions the offline simulator uses, so foreground and offline play use one shared code path — never two parallel implementations of "how fast does mining happen."

### 7.2 Offline algorithm (executed on resume)

1. Read `lastSavedAtEpochMs`; compute `elapsedSeconds = now - lastSaved`, clamped to a sane bound and validated (reject/clamp obviously corrupt or future timestamps — see §9).
2. Determine active `ActivityState` at time of close (combat/gathering/processing/crafting/idle).
3. For elapsed time under a threshold (e.g. a few minutes), simulate step-by-step for accuracy.
4. For elapsed time above the threshold, use **closed-form/batched math**, not per-tick replay:
   - Expected actions = `elapsedSeconds * ratePerHour / 3600`, modified by efficiency/level/retainer multipliers.
   - Resource yields = `expectedActions * expectedYieldPerAction`, with variance sampled via a small number of aggregate random draws (not one draw per action) using distribution approximations (e.g. binomial/normal approximation for rare-drop counts) for performance at large elapsed durations.
   - Combat resolved as expected-kills → expected loot via the same aggregate approach, with a capped chance of a "notable event" (rare drop, near-death, boss encounter) sampled a small fixed number of times regardless of elapsed duration.
5. Apply all resulting deltas atomically to game state: resources, XP, loot, worker production, durability/resource consumption, currency.
6. Roll a bounded number of random narrative/rare events (not proportional to elapsed time — cap it).
7. Persist new state immediately (see §12).
8. Build an `OfflineReport` data object (categorized deltas) and hand it to the UI for a single "While you were away" screen. Never simulate at per-event granularity for long absences — this is a correctness *and* performance requirement; write a unit test asserting O(1)-ish cost regardless of whether elapsed time is 1 hour or 30 days.

### 7.3 Testing requirement
Unit tests must assert: (a) short and long elapsed windows produce results within expected statistical bounds, (b) simulation is idempotent/deterministic given a fixed seed, (c) clamped/corrupted timestamps degrade safely rather than granting unbounded rewards.

---

## 8. COMBAT SYSTEM

- Stats: attack, defense, accuracy, evasion, crit chance, crit damage, attack speed, HP, damage, resistances.
- Resolution formula: implement a transparent, testable damage formula (e.g. `damage = max(1, attack * critMultiplier - defense * mitigationFactor)`), accuracy/evasion resolved as hit-chance roll before damage.
- Content: monster tiers, hunting areas (tied to `WorldNode`/region), elite monsters, bosses, dungeons as multi-encounter definitions.
- Loot tables: weighted drop tables per monster, with rare-drop entries flagged for the "notable event" path in offline simulation.
- Player selects a hunting target/area; combat can run in foreground (visible resolution) or as background `ActivityState` for offline simulation.
- Unit tests: damage formula edge cases, loot table probability sums to 1.0, boss encounters gated correctly by prerequisites.

---

## 9. EQUIPMENT SYSTEM

- Slots: main weapon, secondary weapon, helmet, chest, gloves, boots, 2+ accessory slots, tool slot (for gathering).
- `ItemDefinition` carries base stats, rarity tier, tier requirement; `ItemInstance` carries enhancement level, durability, rolled/bound properties.
- Durability: gathering/combat tools and armor consume durability; repair mechanic consumes processed materials.
- Equipment must remain relevant long-term via the enhancement system (§10) rather than being replaced wholesale every few levels — higher regions introduce new base tiers, not just bigger numbers on the same tier.

---

## 10. ENHANCEMENT SYSTEM

- Levels: `+0` through `+15` on a base track, then distinct **Advanced** and **Transcendent** tiers unlocked by milestones, architected so additional tiers can be appended without a schema rewrite (tier is data, not a hardcoded enum ceiling).
- Each attempt: consumes tier-appropriate materials + Resonance Shards; success probability decreases as level rises; failure has consequences (durability loss, level reduction, or shard consumption depending on tier — define explicitly per tier band).
- **Resolve (failstack-style mechanic):** repeated failures accumulate Resolve, which increases the next attempt's success chance; successful enhancement consumes accumulated Resolve. Implement as a pure function of (currentLevel, resolveAmount, tierTable) → successProbability, fully unit-testable.
- Protection mechanics: a consumable/material that prevents a downgrade or item loss on failure at higher tiers, at a cost.
- Each new tier band must introduce a genuinely new material/mechanic requirement (not just a bigger number) — e.g. Advanced tier requires a new refined material only available from a mid-game processing chain.
- Unit tests: probability curves per tier, Resolve accumulation/consumption math, that no enhancement path allows literally-infinite-for-free progression.

---

## 11. LIFESKILLS

Categories and starter skills:
- **Gathering:** Mining, Logging, Fishing, Hunting, Herbalism, Farming, Excavation
- **Processing:** Grinding, Heating, Drying, Filtering, Smelting, Refining
- **Crafting:** Cooking, Alchemy, Blacksmithing, Armorcraft, Jewelry, Carpentry, Tailoring, Engineering

Each skill: XP curve, levels, mastery perks (efficiency, yield bonus, unlock gates), and recipe/action unlocks gated by level. Specialization should be encouraged via mastery bonuses that compound within a skill line, but nothing should hard-lock a player out of eventually learning other skills — just make crossing over slower/costlier for a generalist.

---

## 12. RESOURCE CHAINS

Model resources as nodes in an explicit directed graph (data-defined in `:core-content`, not implicit in code):

```
Iron Ore → Iron Fragment → Iron Ingot → Refined Iron → Steel → Advanced Steel → equipment components
Wood → Lumber → Treated Lumber → Structural Components → Machinery → Production Buildings
```

Every resource should have ≥2 downstream uses where practical (crafting input, sellable good, quest objective) so the economy isn't a set of isolated XP dead-ends. This graph is the backbone that ties gathering → processing → crafting → equipment → combat → loot → economy together (see §17 dependency map — implement it, don't just describe it).

---

## 13. WORKERS (RETAINERS)

- Attributes: level, gathering speed, production speed, stamina, luck, specialization, traits, efficiency.
- Assignable to nodes (mines, forests, farms, fisheries, workshops).
- Produce resources while player is offline, resolved via the same offline algorithm (§7) — retainer output is just another rate-producing `ActivityState` source aggregated into the offline report.
- Retainers should themselves progress (level up, gain traits) from cumulative production, so managing a retainer roster is a decision space, not a fire-and-forget purchase.

---

## 14. NODE SYSTEM

- Node types: mines, forests, farms, fishing locations, ruins, monster territories, special resource locations, cities, trade hubs, connected via **Wayposts**.
- Progressive unlock via quests/region progression.
- Nodes feed resource chains and host retainer assignments.
- Target network shape: `Village → Forest → Mine → Processing Facility → City → Market`, expanding per region.

---

## 15. ECONOMY

- NPC shops, regional markets, buy/sell, production costs, resource scarcity, simple regional price variance, simulated NPC demand curves (no multiplayer needed — a locally simulated demand/supply model per resource per region is sufficient).
- Design goal: crafting profitability should sometimes make "sell raw material" wrong and "process it first" right, so the player has a real sell-vs-use decision, not an auto-convert-to-gold button.

---

## 16. QUEST SYSTEM

Categories: Main Questline, Regional Quests, Combat Quests, Lifeskill Quests, Side Quests, Daily/Repeatable Activities, Hidden/Discovery Quests.

Quests must reference real system state (inventory counts, kill counts, crafted-item flags, node-discovery flags) via a generic `ObjectiveType` enum + parameters, not bespoke code per quest, so content designers (including your future self) can add quests as data. Example chain: mine iron → craft sword → hunt wolves → discover mine → defeat guardian → unlock new region — implement this exact chain as the first Reach's questline in the vertical slice.

---

## 17. SYSTEM INTERCONNECTION MAP (implement this graph, don't just document it)

```
Mining/Gathering → Processing → Crafting → Equipment → Combat → Loot → Crafting/Economy (loop)
Workers → Resource production
Nodes → Worker production capacity
Quests → Unlocks (nodes, recipes, regions, skills)
Unlocks → every other system
```

Every PR/commit that adds a system should be checked against this graph: does it consume from and produce into at least one neighboring system? If a system is a dead end (produces nothing anything else consumes), that's a design smell — fix it before moving on.

---

## 18. WORLD PROGRESSION & INFINITE PROGRESSION STRATEGY

- Regions ("Reaches") escalate in monster strength, resource tiers, recipes, NPCs, quests, nodes, and introduce a unique regional mechanic each time (don't just reskin the same systems with bigger numbers).
- Infinite progression is **recursive-tier**, not linear-inflation: Tier A material → Tier B → Tier C → Tier D, with new top-tier systems periodically introduced that consume previous-tier output as an input (e.g. a mid-game "Ascendancy" layer that consumes max-tier gear + resources to unlock a soft-prestige mechanic granting permanent small multipliers, then requires a new resource tier to progress further).
- Concretely implement at least: base tiers 1–15 enhancement, one Advanced tier band, and the architecture (not necessarily full content) for a Transcendent band and one Ascendancy/prestige layer, so the ceiling is provably not hardcoded.

---

## 19. UI ARCHITECTURE

- Compose screens: Character, Combat, Inventory, Equipment, Enhancement, Skills, Gathering, Processing, Crafting, Workers, World Map, Nodes, Economy/Market, Quests, Offline Report, Settings.
- Single shared `GameViewModel`-per-feature pattern reading from a central `GameStateRepository` (backed by `:core-persistence` + in-memory `StateFlow`), so UI never mutates state directly — it dispatches intents to the simulation layer.
- Progressive disclosure: tutorial exposes Character/Combat/Inventory/one Gathering skill first; other tabs unlock as quests/level gates are hit, to avoid overwhelming a new player. Implement this as data-driven UI-unlock flags, not hardcoded screen conditionals.
- 2D, clean, systems-first visual style — no 3D, no attempt at photorealistic assets. Placeholder art is acceptable in the vertical slice as long as it's clearly labeled as placeholder and doesn't block functional completeness.

---

## 20. PERSISTENCE / SAVE SYSTEM

- Room DB as source of truth; write-through on every state-changing action that matters (not just periodic autosave) for critical fields (currency, inventory, enhancement outcomes), with a periodic full autosave (e.g. every 30–60s while foregrounded) for the rest.
- **Versioned schema** (`schemaVersion` int) with an explicit migration registry; never silently drop unknown fields — log and preserve where possible.
- **Save integrity:** write to a temp file/table then atomically swap (or use Room's transaction guarantees) to avoid partial writes; keep one rolling backup save distinct from the live save; on load, verify a checksum/sanity envelope and fall back to the backup if the primary fails validation.
- **Offline timestamp validation:** reject timestamps in the future beyond a small clock-skew tolerance, and clamp implausibly large elapsed durations to a configurable maximum simulated window, logging the anomaly rather than granting unbounded rewards.
- This system must have unit tests: migration from each prior schema version, corruption-detection fallback to backup, atomic-write behavior under simulated interruption.

---

## 21. TESTING STRATEGY

Mandatory unit test coverage in `:core-simulation` and `:core-model` before any system is considered "done":
- Offline simulation correctness and performance (short vs. long elapsed windows)
- Enhancement probability/Resolve math
- Resource chain conversions (no resource duplication/loss bugs)
- Worker/retainer production math
- Combat damage/loot resolution
- Quest objective progression and unlock gating
- Save/load round-trip, migration, corruption fallback

Add Compose UI tests for the critical path only (character creation → first combat → first craft → first offline-return report) in the vertical slice; expand as features grow. Run the full test suite before every APK build attempt; do not ship a build with failing tests.

---

## 22. ANDROID BUILD STRATEGY

1. Verify/install required SDK platforms, build tools, and JDK version compatible with the chosen AGP/Gradle versions.
2. Scaffold via standard Gradle Kotlin DSL project structure (not a GUI-only IDE-dependent setup) so builds are fully reproducible from CLI.
3. Use `./gradlew assembleDebug` for iteration builds; produce a `assembleRelease` (unsigned or debug-signed, note which) once the vertical slice is stable.
4. Confirm the build actually installs/launches conceptually correct behavior — where a device/emulator isn't available, at minimum verify the APK is well-formed (`aapt dump badging` or equivalent) and all unit/instrumentation tests pass.
5. **Report the exact APK output path** (e.g. `app/build/outputs/apk/debug/app-debug.apk`) at the end of each successful build.

---

## 23. DEVELOPMENT PHASES

**Phase 0 — Environment & scaffold**
Inspect environment, choose/confirm stack, scaffold module structure, empty but building project, CI-less local test harness working.

**Phase 1 — Vertical slice (build this completely before anything else)**
Character creation → basic combat vs. one monster → basic loot → inventory → equipment (one weapon, one armor slot functional) → enhancement (base tier only) → one gathering skill (Mining) → one processing step (Ore → Ingot) → one crafting recipe (Ingot → Sword) → one worker assignable to one node → one node → basic buy/sell economy → one quest chain (the §16 example chain) → offline progression working end-to-end → save/load with versioning → building, tested, installable APK.

**Phase 2 — Horizontal expansion**
Add remaining gathering/processing/crafting skills, more monsters/regions, more nodes, more workers/traits, deeper enhancement tiers, market depth, quest breadth.

**Phase 3 — Long-horizon systems**
Advanced/Transcendent enhancement tiers, Ascendancy/prestige layer, later Reaches, deeper worker/retainer specialization trees.

Do not begin Phase 2 work until Phase 1's acceptance criteria (§24) are all met and its tests pass.

---

## 24. ACCEPTANCE CRITERIA (Phase 1 vertical slice — must all be true)

- [ ] Fresh install → character creation → playable within the tutorial without confusion
- [ ] Player can mine, process, craft, equip, and fight using their own crafted gear
- [ ] Enhancement can succeed and fail with correct probability behavior, verified by test
- [ ] A worker can be assigned to a node and produces resources over real elapsed time while foregrounded
- [ ] Closing and reopening the app after a simulated time skip produces a correct, legible offline report reflecting combat, gathering, and worker production
- [ ] The example quest chain (§16) is completable end-to-end and gates a real unlock
- [ ] Save persists across app restart with no data loss; a deliberately corrupted save file falls back to backup without crashing
- [ ] `:core-simulation` and `:core-model` have passing unit tests covering §21
- [ ] `./gradlew assembleDebug` succeeds and produces a valid APK at a reported path
- [ ] No placeholder system pretends to be complete — anything unfinished is explicitly marked `TODO` in `/docs/decisions.md`, not silently stubbed in a way that misleads

---

## 25. DEBUGGING INSTRUCTIONS

- On any build failure: read the actual Gradle error output, don't guess; fix root cause, rebuild, don't reflexively delete/regenerate unrelated files.
- On any test failure: treat it as real until proven a test bug — simulation math bugs are the highest-risk failure class in this project (see §7, §10, §20).
- On crashes: reproduce via unit/instrumentation test first if possible, so the fix is verifiable and regression-proof.
- Keep a running `/docs/decisions.md` log: what was decided, why, and any deviation from this prompt's defaults (e.g. swapping Hilt for manual DI) — one or two sentences per entry, not essays.
- If genuinely blocked by an environment limitation (missing SDK component, no network access to a required repo, etc.), document the blocker clearly and pick the most reasonable fallback rather than halting — e.g. mock the missing piece and note it as a follow-up.

---

## 26. AUTONOMOUS DECISION-MAKING RULES

You are authorized, without further confirmation, to:
- Choose specific library versions, DI approach, and minor architectural details not pinned above
- Design and populate placeholder game content (monster stats, item stats, recipe costs) reasonably, as long as they're data-driven and tunable, not hardcoded magic numbers scattered through logic
- Restructure modules if the initial split proves awkward, as long as `:core-simulation` stays UI/Android-free
- Reprioritize within a phase (not across phases) to unblock progress
- Add reasonable test coverage beyond the minimums in §21

You must not, without flagging it explicitly in `/docs/decisions.md` and pausing for confirmation:
- Introduce a server/backend dependency or account requirement
- Introduce mandatory ads or a forced-engagement energy system
- Ship Phase 2/3 content before Phase 1 acceptance criteria (§24) are met
- Use any copyrighted third-party names/assets/lore

---

## 27. FINAL DELIVERABLE FOR EACH WORK SESSION

At the end of each autonomous work session, report:
1. What was implemented since the last report
2. Test results (pass/fail counts, and detail on any failures)
3. The exact APK output path, if a build was produced
4. Any entries added to `/docs/decisions.md`
5. What's next per the phase plan

Begin now with Phase 0.
