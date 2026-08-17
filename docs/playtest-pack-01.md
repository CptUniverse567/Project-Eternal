# Playtest — Pack 01: Outer Reaches (0.2.0)

Human-oriented on-device verification of the Pack 01 expansion plus the progression
deadlock audit. Performed on the API-36 emulator (AVD `EternalTest`, android-36), fresh save
after `pm clear` (the app's `allowBackup="true"` auto-restores stale data across reinstall —
use `pm clear` for a true fresh save).

Build under test: `app-debug.apk`, package `com.projecteternal.app`, versionName `0.2.0`,
versionCode 2.

---

## 1. Progression deadlock — Refining (CONFIRMED, FIXED)

**Symptom:** a fresh player reaches Refining Lv 0 and every Refining recipe is level-gated
("Refine Iron requires Refining Lv 1"); there is no level-0 Refining XP source, so Refining
can never reach Lv 1 — and because q_cindervale requires holding **Steel** (which requires
Refining 2 via Forge Steel), the Cindervale gate is unreachable.

**Root cause:** `refine_refined` (Refine Iron) was `skillLevelRequired = 1` with no earlier
Refining recipe (all other Processing skills have a level-0 entry recipe).

**Fix (content data):** `refine_refined.skillLevelRequired = 1 → 0`. Now: Refining 0 →
Refine Iron → XP → 1 → 2 → Forge Steel → Steel → q_cindervale.

**Verification (on-device, fresh save):**
- Refine Iron now renders an **enabled "Start"** button at Refining Lv 0 (was "Learn level 1
  Refining first").
- Tapping it shows **"Processing: Refine Iron"** and produces **Refined Iron**.
- Regression: `OuterReachesTest.refining is reachable from level 0 via Refine Iron` and
  `refining to steel end to end unlocks cindervale`; ContentIntegrity guardrail
  `every processing and crafting skill has a level 0 recipe`.

## 2. Full deadlock audit — findings

| # | System | Dependency | Classification | Action |
|---|---|---|---|---|
| 1 | Refining | Refine Iron gated at Lv 1, no Lv-0 XP | **D — deadlock** (blocks Cindervale) | Fixed (1→0) |
| 2 | Engineering | Fabricate Machinery Part gated at Lv 1, no Lv-0 XP | **D — deadlock** | Fixed (1→0) |
| 3 | Jewelry (Pack 01) | Set the Ember Ring gated at Lv 2, no Lv-0 XP | **D — deadlock** | Fixed (2→0) |
| 4 | Quests | q_cindervale / q_frostreach unlock gates | **A — valid** (no self-internal requirements; verified by tests) | none |
| 5 | Regions | Cindervale/Frostreach node unlocks | **A — valid** (region token gates all nodes; none unlocks its own region) | none |
| 6 | Enhancement | BASE→ADVANCED→TRANSCENDENT | **A/B — valid / intended gate** (+31 gate preserved; ADVANCED now has frostvein alternate) | none |
| 7 | ASCENDANT | `tier:ascendant` unreachable | **B — intended gate** | none |
| 8 | Recipes | steel sword / oak shield tokens never granted | **E — latent** (nothing requires them) | documented, not fixed |
| 9 | Workers | Pella/Runa gathering-only | **A — valid** (no advertised processing) | none |
| 10 | Economy/offline/save | no negative currency; hazard floored; gates persist | **A — valid** (tests) | none |

Guardrail added: every PROCESSING/CRAFTING skill must have at least one level-0 recipe
(`ContentIntegrityTest`), preventing future skill deadlocks.

## 3. On-device walkthrough (fresh save "Pack2")

Verified live:

- Fresh start → character creation → mining → quest auto-resolution (Gather 3 / 10 Iron Ore).
- Grinding → Smelting → **Refining (the fix)** → Refined Iron.
- Logging → Forge Bronze Sword → equip → quest "Forge Your Blade" complete.
- Combat: Grey Wolves (offline window, 235 slain) → "Slay the Guardian" unlocked.
- Durability: Bronze Sword broke mid-combat (0/80); repair correctly requires 4 Repair Kits
  (engine rejected a partial repair with 1 kit — legitimate repair-vs-reforge decision);
  re-forging a fresh sword is the intended path.
- Offline report legible (activity, kills, notable events, broken item names).
- Engineering card renders "lv 0 required" (fix visible).
- New Pack 01 recipes present in the (much longer) Industry list; region cards carry rare
  find / hazard lines per design.

Not driven to full completion on-device in this pass: Emberreach→Cindervale→Stormreach→
Frostreach region traversal and +16..+31 enhancement are each verified deterministically by
the JVM suite (`OuterReachesTest`: region gates, chains, bosses, hazard, workers,
enhancement checkpoints, offline) and by the fresh-save emulator coverage above.

## 4. UX notes

- **Blind-tap mis-craft**: coordinate-driven taps on the long Industry list can hit the wrong
  recipe (once crafted Hemp Rope instead of the sword). Not a game bug; a UI density note for
  Pack 02 (recipe search/filter would help).
- **Industry list is now very long** (50+ recipes). Discoverability of a specific recipe
  requires scrolling; consider category tabs/filter in a later pass.
- **First-crafting Equip button** remains cramped when Enhancement is locked (known baseline
  UX issue, unchanged).
- **Market Buy list re-scrolls** after a purchase (known baseline UX issue, unchanged).
- **Repair kit economy**: a fully-broken 80-durability item needs 4 kits; the UI enables
  "Repair" with 1 kit but the engine rejects — minor UI/engine mismatch (documented; not a
  deadlock since re-forging is viable).

## 5. Post-playtest inventory + enhancement UX pass (0.2.1)

### Equipment selling (implemented 0.2.1)
- **Root cause**: Equipment instances (`equipmentItems`) were stored separately from stackable
  inventory and never surfaced in the Sell section of the Economy screen.
- **Fix**: Added `GameIntent.SellEquipment(uid)`, `SellAllEquipment(defId)`, and `SellAll(defId)`
  intents with engine handlers. Equipment sells at `sellPrice` × regional modifier (data-driven,
  no special formula). Equipped items are protected (no Sell button, engine rejects).
- **On-device verified**: Unequipped pickaxe shows a Sell button; equipped pickaxe does not.
  Pack 01 equipment (Cindervale/Frostforge) automatically uses the same mechanism.

### Sell All
- Per-item-type bulk sell: stackables via `SellAll(defId)` (entire stack), equipment via
  `SellAllEquipment(defId)` (all copies except equipped). No confirmation dialog for small
  stacks; the existing UX convention is followed.
- On-device: empty pack state renders "No equipment in your pack" (correct for fresh save).

### Enhancement UI (restructured 0.2.1)
- **Previous**: all action buttons squeezed into horizontal rows; cramped as enhancement
  options multiplied (Oil, Ward, Full Negation, alternate material, Repair).
- **New**: vertically stacked `EnhanceBlock` composable — header info (level, success%, shards,
  band, material, shatter warning) followed by full-width action buttons. No button overlaps.
- On-device: `Enhancement` header → `+0 → +1` → `Success: 100% · Shards: 1 (Base)` — all
  readable, vertical, no squeeze.

### Verification status (0.2.1)
- JVM suite: **176 tests, 0 failures** (EquipmentSellTest × 10, ContentIntegrityTest 15,
  OuterReachesTest 23, plus all baseline).
- Instrumented: `EternalCriticalPathTest` **3/3** on API-36.
- `:app:assembleDebug` green.
- On-device: equipped protection confirmed; Sell button on unequipped gear; enhancement
  vertical layout renders cleanly; existing Pack 01 gameplay functional.
