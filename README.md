# Project Eternal

**Project Eternal** is a fully offline Android idle-RPG / life-simulation game. You play a
Wayfarer who gathers, processes, crafts, fights, enchants, and hires retainers — all of which
keep progressing even while the app is closed.

- **Platform:** Android only, fully offline (no account, no server, no mandatory ads).
- **Build:** Kotlin 2.2.x, Jetpack Compose (Material 3), Gradle Kotlin DSL, Room persistence.
- **Baseline:** `0.1.0-slice` Release Candidate.

---

## What is implemented

Core gameplay systems (all verified by the automated suite and an independent emulator audit):

- **Character progression** — levels, skills, XP curves, stats.
- **Gathering** — Mining, Logging, Herbalism, Farming, Fishing.
- **Processing** — Grinding, Heating, Smelting, Refining, Drying, Milling.
- **Crafting** — Blacksmithing, Armorcraft, Cooking, Alchemy, Carpentry, Engineering.
- **Equipment** — weapons, armor, tools, shields; durability and repair.
- **Enhancement** — BASE (+0..+15), ADVANCED (+16..+30), TRANSCENDENT (+31..+45);
  Resolve failstack mechanic, protection oil, Ward of Stability, shatter consequences.
  ASCENDANT is architecture-only (intentionally unreachable in this baseline).
- **Workers (Retainers)** — recruitment, traits, node assignment, offline production.
- **Regions (Reaches)** — Hollowreach, Emberreach, Stormreach, Dawnreach; regional price and
  yield modifiers; bosses per region.
- **Economy** — Marks currency, buy/sell market, regional price modifiers, processed-goods
  demand premium.
- **Quests** — main progression chain, regional, combat, lifeskill and side quests, all
  data-driven.
- **Offline progression** — closed-form simulation (12 h cap), single legible "While you were
  away" report, retainer production included.
- **Persistence** — atomic save blob (LIVE + BACKUP), SHA-256 checksum, corruption fallback,
  versioned schema.

---

## Modules

| Module | Purpose |
|---|---|
| `:app` | Application shell, DI, navigation, offline report |
| `:core-model` | Pure Kotlin data classes (no Android deps) |
| `:core-simulation` | Pure Kotlin engine: offline simulator, combat, enhancement, economy |
| `:core-content` | Static game content (regions, monsters, recipes, quests, items) |
| `:core-persistence` | Room save storage, versioning, migration |
| `:feature-*` | UI feature modules (character, adventure, industry, economy) |

---

## Building

Requires JDK 17 and the Android SDK (compileSdk/targetSdk 36, minSdk 26).

```sh
# Debug APK
./gradlew.bat :app:assembleDebug

# Release APK (unsigned in this baseline)
./gradlew.bat :app:assembleRelease
```

Release APK output: `app/build/outputs/apk/release/`. The release variant has **no signing
configuration** in this baseline; it is unsigned and not a production-signed consumer build.

## Testing

```sh
# JVM unit suite (core-model, core-content, core-simulation, core-persistence, app)
./gradlew.bat :core-model:test :core-content:test :core-simulation:test :core-persistence:testDebugUnitTest :app:testDebugUnitTest

# Instrumented critical-path suite (requires an API-36 emulator)
./gradlew.bat :app:connectedDebugAndroidTest
```

Verified status at baseline:
- JVM suite: **137 tests, 0 failures, 0 errors**.
- Instrumented (`EternalCriticalPathTest` on API-36): **3/3 passed**.

---

## Documentation

- `project-eternal-opencode-prompt.md` — original engineering master prompt.
- `project-eternal-eternal-expansion-master-prompt.md` — long-term expansion master prompt.
- `docs/decisions.md` — engineering decisions log.
- `docs/playtest-phase-3a.md` — Phase 3A playtest checkpoint.
- `docs/eternal-expansion-roadmap.md` — long-term expansion roadmap and Pack 01 design.

## Known non-blocking UX issues (baseline)

- First-crafting **Equip** button is cramped on the gear pack row (usable, non-blocking).
- Market **Buy list re-scrolls** toward the top after a purchase.
- Enhancement controls only appear after **unequipping** an item.

---

## Expansion boundary

This repository is frozen at the `0.1.0-slice` release candidate. Future development continues
through **Eternal Expansion Packs** starting with **Pack 01 — Outer Reaches** (`0.2.0`), as
specified in `docs/eternal-expansion-roadmap.md`. No expansion content is implemented in this
baseline.
