# PROJECT ETERNAL

> An offline-first Android idle RPG built around gathering, crafting, combat, equipment enhancement, workers, economy, quests, and long-term progression.

Project Eternal is a sandbox-style idle RPG designed around the feeling of progressing through a large MMORPG world — without requiring an internet connection.

The game combines active gameplay with deterministic offline progression, allowing activities to continue while the player is away.

---

## Current Status

**Release Candidate — `0.1.0-slice`**

The current build is a stable vertical slice and serves as the foundation for the game's long-term expansion.

The current release has been independently tested on an Android API 36 emulator with a fresh save and complete progression walkthrough.

### Current verification

- JVM test suite: **137 tests**
- Instrumented Android tests: **3/3 passed**
- Debug APK: **Build successful**
- Fresh-install playthrough: **Completed**
- Save/restart behavior: **Verified**
- Offline simulation: **Verified**
- Gathering: **Verified**
- Processing: **Verified**
- Crafting: **Verified**
- Combat: **Verified**
- Equipment and durability: **Verified**
- Enhancement: **Verified**
- Quests: **Verified**
- Economy: **Verified**
- Workers/retainers: **Verified**

The current build is considered the stable foundation for future development.

---

# Core Concept

Project Eternal is built around a continuous progression loop:

```text
        ┌─────────────┐
        │   Explore   │
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │   Gather    │
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │  Process    │
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │   Craft     │
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │ Equip/Enhance│
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │    Fight    │
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │   Quests    │
        └──────┬──────┘
               ↓
        ┌─────────────┐
        │   Unlock    │
        │  New Reach  │
        └──────┬──────┘
               │
               └──────────────→ Repeat
