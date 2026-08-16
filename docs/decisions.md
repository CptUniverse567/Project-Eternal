# Project Eternal — Engineering Decisions Log

Ongoing log of architectural and product decisions, per the master implementation prompt §25.

---

## Phase 0 — Environment & scaffold (decision date: 2026-08-14)

- **Stack confirmed**: Kotlin 2.2.21, Jetpack Compose (Material 3), single-activity, Compose Navigation,
  Kotlin Coroutines + Flow, Gradle KTS. Environment provides JDK 17.0.12, Android SDK at
  `C:\Users\jcoma\AppData\Local\Android\Sdk` (platforms 35/36, build-tools 34–36), network to Google Maven + Central.
- **SDK levels**: compileSdk 36, targetSdk 36, minSdk 26. Chosen because installed SDK tops out at API 36.
- **Versions pinned** (verified against repos on 2026-08-14): Gradle 8.14.3 (wrapper), AGP 8.13.2,
  KSP 2.2.21-2.0.5, Room 2.8.4, Compose BOM 2025.09.00, core-ktx 1.16.0, activity-compose 1.10.1,
  lifecycle 2.9.1, navigation-compose 2.9.1, datastore-preferences 1.1.4.
  - NOTE: Newest androidx releases (compose 1.12, core 1.19, lifecycle 2.11) require compileSdk 37 +
    AGP 9.1; SDK 37 is not installed and AGP 9 is a breaking toolchain change, so we deliberately pin
    one major version back. Upgrade path: install android-37 + AGP 9.x when desired.
- **DI: manual (no Hilt)**. Decided for robustness and build simplicity: a hand-wired `AppContainer`
  with `CreationExtras`-based ViewModel factories. Documented deviation from prompt's Hilt default —
  the environment is not hostile to Hilt, but manual DI keeps the module graph (4 feature modules)
  simple, fully testable, and faster to build. Revisit if DI complexity grows.
- **Persistence: Room stores a single atomic save blob** (one `save_slot` table, LIVE + BACKUP rows)
  containing the serialized `GameState` (kotlinx-serialization JSON) + SHA-256 checksum + version.
  Rationale: offline games need atomic-slot semantics (temp write → swap, checksum, backup rollback);
  a normalized table-per-entity mirror would provide no query benefit since the entire state is
  loaded into memory at startup. Room remains the durable backend (source of truth); DataStore holds
  small settings flags. Deviates from "structured state" wording of §5.1 in shape, not in framework.
- **Module split**: `:app` (shell/DI/nav/creation-tutorial/offline-report/settings), `:core-model`,
  `:core-simulation`, `:core-content`, `:core-persistence`, and 4 feature modules
  (`:feature-character`, `:feature-adventure`, `:feature-industry`, `:feature-economy`).
  Fewer features than §5.2's one-per-screen enumeration; each feature groups related screens so
  the spirit (no single giant UI module, clear boundaries) is preserved.
- **Test stack**: JUnit4 + kotlinx-coroutines-test; pure-JVM modules (`core-model`,
  `core-simulation`, `core-content`) have zero Android deps. Compose UI critical-path tests will be
  written as instrumentation tests (androidTest) to run on the available API-36 emulator AVDs.

## Phase 1 — core-simulation (decision date: 2026-08-14)

- **Bugs found & fixed while green-lighting the sim test suite**:
  1. `RecipeDefinition.skillLevelRequired` defaulted to `1`, making "entry" recipes
     (grind/smelt/craft bronze sword, etc.) unusable at level 0 — chicken-and-egg at creation.
     Fixed by defaulting to `0`; recipes that must be gated (steel sword, pickaxe, refine) set 1+ explicitly.
  2. `OfflineSimulator` treated the `now - lastSaved` millisecond delta as seconds — a 5-minute
     absence simulated 5 *real* hours, and every long absence clamped to the 12h max. Now correctly
     divides by 1000 and advances `lastSavedEpochMs` to the *simulated* endpoint (so a clamped run
     never re-simulates the discarded tail).
  3. Retainer stamina economy was inverted (0.1 cost/action vs 0.03 regen/s ≈ never drained).
     Rebalanced to 1.0 cost/action, 0.02 regen/s, and the engine now includes regen over the window
     when budgeting actions — retainers sustain ~72/hr long-run vs the player's 120/hr at a node.
  4. Enhancement BASE band declared "downgrades at +11..+15" in its note but was
     `DURABILITY_ONLY` everywhere. Added `EnhancementTable.downgradeThreshold`; BASE fails
     durability-only below +11 and downgrades one level at/above it. ADVANCED+ downgrade from band entry.
  5. Enhancement had no hard ceiling (ASCENDANT 46..60 kept returning a table at +60). `tableFor`
     now returns null at the global max, so +60 is uncraftable-into.
  6. Quest chain resolution was single-pass: a reward unlock (e.g. `quest:deep_mine`) discovered a
     node *after* quest processing, so the discovery objective couldn't complete in the same `apply`.
     `GameEngine.apply` now iterates quest-processing + node-discovery to a fixpoint — whole chains
     (including auto-accept cascades) resolve atomically.
  7. Regional price modifier was too weak to matter (Emberreach 1.15 → 3→3 sell). Raised to 1.5 so
     higher Reaches measurably improve trade prices.
- **Testing convention confirmed**: JUnit4 message-first arg order; no `kotlin.test`. All sim tests
  use bounded loops (`guard` counters) to prevent infinite-loop hangs from failing assertions.

## Phase 1 — app layer & UI shell (decision date: 2026-08-14)

- **`GameStateRepository` holds a nullable state**: the interface's `state: StateFlow<GameState>`
  was relaxed to `StateFlow<GameState?>`. The controller owns a `Startup` phase
  (`LOADING / NEW_GAME / READY / CORRUPT`) because no state exists before a save is loaded or a
  character is created; features render only in the `READY` phase. UI never mutates state directly —
  it dispatches `GameIntent`s to the repository (unchanged architecture).
- **`GameController` (app module) is the only Android-aware state owner.** It wires the pure
  `GameEngine` to persistence and time:
  - foreground ticker: ~1 Hz `Tick(now)` while the activity is started (`onStart`/`onStop`);
  - offline-on-resume: `onResume` runs `OfflineSimulator.simulate(state, now)` over the gap since the
    last saved tick, surfaces `pendingOfflineReport`, then resumes the ticker;
  - debounced self-coalescing save (3 s) for ticks/activity, immediate flush on `onPause`.
  The pure-sim/game layers stay 100% Android-free; time is passed in as `Long` epoch-ms by the caller.
- **Manual DI confirmed in code**: `EternalApplication` builds an `AppContainer` holding Room
  `EternalDatabase` → `RoomSaveStore` → `SaveManager` → `GameController`; `MainActivity` reads the
  controller off the Application. No Hilt/koin; matches Phase 0 decision.
- **App shell** (`app/ui/EternalApp.kt`): single `Scaffold` with a live character header (name/level/
  marks/HP bar/current-activity + Stop), 5-tab bottom nav (Adventure/Industry/Economy/Character/
  Settings), a Snackbar host for quest-complete events, and an `OfflineReportDialog` shown once when
  `pendingOfflineReport` is present (dismiss dispatches `DismissOfflineReport`).
- **Feature screens are thin renderers**: each takes `(GameStateRepository, GameState)` and dispatches
  intents; no per-screen ViewModels (state already lives in one `StateFlow`). The economy screen
  reuses `MarketService` for live buy/sell prices; the industry screen drives action type from the
  recipe's skill category (PROCESSING vs CRAFTING); the character screen uses `EnhancementTables` and
  `QuestEngine.currentValue` directly for display.
- **`recipes` skill-gating in the UI mirrors the engine**: locked recipes render disabled with the
  required skill level; `Materials` check uses `GameState.inventoryCount`.

## Phase 2 — breadth, market, enhancement, retainers (decision date: 2026-08-15)

- **Content breadth (core-content)**: added the two remaining gathering skills (farming, fishing), two
  processing skills (drying, milling) and two crafting skills (carpentry, engineering), with a full
  harvest → mill → bake → dry → fish → rod/shield → engineering gear chain of items, nodes and recipes.
  New side quests gate the rod/bread recipes (`q_side_fisher`, `q_side_harvest`).
- **Third region with a real regional mechanic**: `Stormreach` (tier 3) is unlock-gated behind the
  regional chain `q_emberreach_ash` (kill 10 Ash-Spawned Brutes) → `region:stormreach` →
  `q_stormreach_herald`. Its mechanic is a `yieldMultiplier` on `RegionDefinition` (1.5×), applied to
  *yields* (player + retainer) through the shared rate math rather than per-tick speed, so Stormreach
  raising gathering output is visible in registries and tests.
- **Market depth**: sell prices are no longer linear — materials/consumables/equipment carry a
  `processedDemandPremium` (up to 1.25×) that scales with the furthest Reach toured, so "process then
  sell" beats dumping raw ore. `MarketService.sellAdvice` surfaces the best process-before-sell path
  in the economy UI (e.g. "grain → Flour sells for 12◎/unit").
- **Enhancement depth**: new `fullNegationItem` on `EnhancementTable` (BASE + ADVANCED =
  `ward_of_stability`, crafted via engineering `shape_ward` after `q_stormreach_herald`). It negates
  the *entire* failure — no downgrade AND no durability loss — distinct from `protectionItem` (oil)
  which only blocks the downgrade. Resolver + engine consume it only when used; the gear tab renders
  plain/oil/ward buttons based on what the player holds.
- **Retainer depth**: new `RetainerTraits` content catalog with per-trait speed/luck multipliers, and a
  deterministic milestone trait grant in `RetainerEngine` (levels 3/7/12/18/25). Traits flow into
  `Rates.retainerActionsPerHour` (speed) and the yield roll (luck); the retainer card shows trait
  chips and the next milestone.
- **UX polish**: activity progress bar (real carry fraction) + live rate + "next goal" hint in the
  top bar; enhance buttons gated behind `screen:enhance`; workers section gated behind
  `screen:workers`; sell-vs-process hints in the market.
- **Tests**: extended `ContentIntegrityTest` (new monsters/regions/quests/enhancement consumables/
  traits); new `RegionalChainTest` (Stormreach chain + regional mechanic + life-skill recipes);
  new ward/market-premium/retainer-trait cases in the sim suite.

## Phase 2b — retainer recruitment (decision date: 2026-08-15)

- **Retainer roster is no longer a dead end**: `screen:workers` previously unlocked a wine-style
  section with *only* Aldo (a free MINER) and a dangling "discover the grotto to recruit workers"
  hint. New `RetainerRecruits` content catalog ($core-content) adds 6 hireable workers — Mara (FARMER),
  Ode (LUMBERJACK), Selkie (FISHER), Helga (MINER), Sable (CRAFTER), Voss (FORAGER) — each gated
  behind a held unlock token (`screen:workers`, `recipe:craft_fishing_rod`, `region:emberreach`,
  `region:stormreach`) and a Marks cost (350–1200), and each carrying bespoke speed/luck stats.
- **Engine**: new `GameIntent.RecruitRetainer(defId)` handler in `GameEngine` — no-ops unless the
  token is held and the player has the Marks, spends exactly the cost, appends a fresh level-1
  `Retainer` (stats from the def, empty trait pool), and refuses duplicate hires.
- **UI**: the retainer section of `EconomyScreen` gains a "Hire Workers" block rendering every
  recruit whose unlock is held and who isn't already hired, with a `Hire` button disabled while the
  player is short on Marks.
- **Tests**: `ContentIntegrityTest` cover checks catalog resolution/uniqueness/cost/token shape;
  `RetainerTest` covers token-gating, insufficient-Marks, exact-cost spend, stat copying and
  duplicate-hire no-op. Full JVM suite + `:app:assembleDebug` green.

## Phase 3A — Transcendent progression (decision date: 2026-08-16)

- **The dead TRANSCENDENT band is now real.** Previously it existed as an un-gated,
  architecturally unreachable placeholder (the §16-style "looks implemented but isn't"). It now
  differs from ADVANCED on every axis the master prompt demands:
  - *Materially more expensive*: 1 Stormbound Catalyst per attempt (new tier-4 MATERIAL), distilled
    via a new `refine_catalyst` processing recipe — core_storm (Stormreach) + crystal_advanced
    (Emberreach) → catalyst, requiring Refining 5 and gated behind the existing `q_stormreach_herald`
    capstone (its reward now also grants the catalyst recipe).
  - *Functionally more powerful*: stat growth is now data-driven per band
    (`EnhancementTable.statMultiplierPerLevel`; 0.15 BASE/ADVANCED, 0.20 TRANSCENDENT), wired through
    `CombatStatsMath.effectiveStats`, so a +31 actually out-performs a +30.
  - *Mechanically distinct*: new `FailureConsequence.SHATTER_TO_BAND_FLOOR` — an unprotected failure
    at +31..+45 resets the item to +31 (waste of existing value), vs ADVANCED's -2 downgrade.
    Protection (oil) blocks the shatter but not durability loss; the ward blocks both. Resolve keeps
    working as the pity ladder across the band.
  - *Gated properly*: `EnhancementTable.unlockToken` (default "") gates the whole band behind a held
    token; resolver preconditions + `Engine.enhance` both refuse locked bands (and ASCENDANT carries
    `tier:ascendant`, which no content grants — true architecture-only, i.e. no fake tier pretend
    implemented).
- **Late-game progression gate**: new regional quest `q_light_beyond` (auto-accepts after the
  Stormreach capstone) demands `ENHANCE_ANY_TO` +31 *and* holding a Stormbound Catalyst, then
  unlocks the fourth region **Dawnreach** (tier 4, highest price/yield modifiers). Dawnreach holds a
  farming node (Skybreak Spire) and a rental boss (`starfall_wyrm`) whose guaranteed loot pays
  catalyst_storm, closing the loop: region → core → catalyst → +31 → next region → more catalyst.
- **Offline/automation**: refining the catalyst flows through the single `ActivityEngine.advanceRecipe`
  path, so it continues while closed; tests lock the exact input/output counts and the unlock gate.
- **UI**: the gear tab shows the band, the per-attempt material + owned count, a hard "locked" state
  (with disabled buttons) and the shatter-failure warning; the Settings region line now reads through
  Dawnreach.
- **Regression (JVM)**: `GameController.load()` used to run its `_state.value != null` guard BEFORE
  the suspending `saveManager.load()`, so a StartGame dispatched mid-load could be clobbered by the
  late NoSave branch. The result is now resolved first and the guard re-checked after the suspension —
  deterministic first-write-wins. `GameControllerTest` ran 6/6 clean repeats.
- **Tests**: `ContentIntegrityTest` now covers the starfall_wyrm loot sums, the refine_catalyst recipe,
  q_light_beyond coherence and its the "no quest may grant the ascendant tier" invariant; new
  `TranscendentTest` (core-simulation) covers material cost, unlock gating, shatter/protect/ward,
  per-band stat growth, the Dawnreach gate chain, and offline distillation. Full JVM suite green;
  `:app:assembleDebug` green. No schema change (content only + one engine guard), so schema stays 1.

## Phase 3A playtest checkpoint (decision date: 2026-08-17)

- **`EQUIP_SLOT` objectives are pinned, not re-evaluated.** They were computed from *live* equip
  state each tick; a broken tool auto-unequips, so an offline run regressed q_intro ("Equip a tool")
  and stalled the entire main quest chain and its screen unlocks. `QuestEngine` now pins the max of
  (previous, live) progress for `EQUIP_SLOT` and completes against the pinned value — equipping once
  is permanent. Verified on-device with an 8h10m offline run (chain advances to "Craft a Bronze
  Sword" instead of stalling). Two JVM regression tests added.
- **Bug-fix-only scope confirmed for this checkpoint** (prompt §playtest): only the 4 critical bugs
  fixed (see `docs/playtest-phase-3a.md` §2). All §9 "suggested improvements" are recorded as design
  input for the next phase decision, none implemented.
- **Label fixes**: `OfflineSimulator.itemsBroken` resolves def ids to item display names (fallback
  uid); the offline report "Levels up" line formats as "Character Lv 5, Mining Lv 11"; economy sell
  advice no longer recommends "process first for more" when no recipe exists.

## Phase 3A post-playtest UX pass (decision date: 2026-08-17)

- **Scope: display/UX text and navigation ONLY.** No changes to progression, combat, enhancement
  probabilities, economy formulas, or new gameplay systems. All five fixes are "cheap /
  high-confidence" items from `docs/playtest-phase-3a.md` §9.
- **First-run help sheet**: new `app/ui/HelpDialog.kt` (How to play + glossary). Shown once after a
  brand-new game is created (`GameController.showFirstRunHelp`, set true on `StartGame`), and
  re-openable anytime via a "?" button in the top bar. Pure educational text; no rules live there.
- **Currency naming**: header now reads "N Marks" (was "N ◎"); the help sheet states "◎ is Marks,
  the realm's currency". Compact price rows in the Market keep the symbol.
- **Region-modifier explanation**: Market header rewritten to plain language — explains that touring
  farther Reaches raises selling prices and that refined/crafted goods sell for more than raw
  (values still derived from the same `MarketService` functions; formulas untouched).
- **Node-rate reconciliation**: gathering node cards now show the effective per-hour when it differs
  from the card's base rate ("~150/hr · base 120/hr"), matching the live top-bar rate; non-affected
  nodes keep the plain base.
- **Repair/equip shortcut**: when the first incomplete quest objective is an EQUIP_SLOT objective,
  the top bar shows a "⚒ Gear" button that deep-links to the Character tab + Gear tab.
  (`nextGoal` returns a `(hint, needsGear)` pair; `CharacterScreen` takes a `gearTabRequest`.)
- **Verification**: full JVM suite 110 unique / 126 variants, 0 failures; instrumented
  `EternalCriticalPathTest` 2/2 on API-36; `:app:assembleDebug` green; fresh-save on-device smoke
  confirmed the sheet, header, node rate, and market text render as intended.

## Save/restart hardening — Task 12 (decision date: 2026-08-16)

- **Retrospective-save bug closed.** If the process was killed by the OS (or crashed) *before*
  `onPause`'s immediate flush, any activity advanced during the final few seconds could be missing on
  the next launch — the save was as much as one debounce interval (3 s) stale, and progess made after
  the last write was only recovered if a tag happened to fire. Fix: the foreground ticker now also
  flushes the save on every tick (tick → same `flushingSave` path the debounce uses), which bounds
  save staleness to tick granularity (~1 s) while retaining the debounced write and the immediate
  `onPause` flush. No schema change; test asserts the blob's `flushTimestamp` advances and remains
  valid across a force-stop restart in `< 1200ms` windows.
- **Verification**: JVM suite 110 unique / 126 variants, 0 failures; instrumented
  `EternalCriticalPathTest` 2/2; `:app:assembleDebug` green; on-device fresh-save pass re-confirmed
  first-run help, offline report, quest completion, industry conversion, and economy screens.

## TODO / known placeholders (add as encountered)

- (none yet)
- **Instrumented suite stabilization (2026-08-15)**: the emulator's janky main thread made UI asserts flaky; switched to (a) performTextReplacement (sync) instead of performTextInput, (b) re-clicking btn_set_out until state advances (self-healing), (c) polling raw semantics Text for the header name instead of assertTextContains, (d) polling the debounced SaveManager write instead of one-shot assertNotNull. Both freshStart_creation_toPlayableShell and offlineReturn_showsReport_thenDismisses now PASS deterministically on EternalTest (AVD).
