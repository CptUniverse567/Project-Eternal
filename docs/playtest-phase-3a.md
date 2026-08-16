# Playtest — Phase 3A Checkpoint

Human-oriented playability audit performed on the API-36 emulator (AVD `EternalTest`, android-36
google_apis x86_64). This doc records what a fresh human player would hit; it is input for the next
design/staging decision, not a backlog list to grind.

Built/tested: `app-debug.apk` — package `com.projecteternal.app`, versionName `0.1.0-slice`,
versionCode 1, minSdk 26, targetSdk 36. Same build in `app\build\outputs\apk\debug\`.

---

## 1. Build tested

- Clean debug build; installed; fresh save (no `pm clear`-level data aside from test save).
- Character name entered via on-screen keyboard and persisted ("Althea") — a prior truncated-entry
  incident was an emulator `input text` flake, not an app bug (retry succeeded).
- Full-screen walking through every accessible screen: creation; Adventure home (nodes + boss);
  Industry (recipes per category); Economy (Buy / Sell+Retainer tabs); Character (Stats / Quests /
  Enhance / Gear); Settings; offline report flow.

## 2. Critical bugs (found, fixed, regression-tested)

1. **Offline quest stall after tool break — CRITICAL, fixed.** `EQUIP_SLOT` objectives were
   re-evaluated from *live* state; a broken (destroyed) tool auto-unequips, so after a long offline
   run q_intro "Equip a tool" regressed, the per-node goal still tallied ("Gather Iron Ore 1016/3"),
   and every later quest in the main chain (and its screen unlocks) stalled forever. Player-facing
   result: the game quietly dead-ends if your tool breaks while you're gone.
   Fix: `QuestEngine` pins `EQUIP_SLOT` objective progress (`maxOf(prev, live)`) and uses the pinned
   value for completion, so equipping once is permanent. Verified on-device: after an 8h10m offline
   run on a fresh save, "Broken: Journeyman's Pickaxe", quests "A Wayfarer's First Step, A Miner's
   Share" complete, and the next hint advances to "Next: Craft a Bronze Sword"; Marks reflect the
   quest rewards (65 across both). Two regression tests added (equip pins after overnight break;
   equip not pinned when never equipped).
2. **Offline report showed raw item ids** ("Broken: uid_start_pickaxe"). Fixed: `OfflineSimulator`
   resolves def id → item display name (fallback uid).
3. **"Levels up" line was unreadable** ("5 (character), 11 (skill:mining)"). Fixed to
   "Character Lv 5, Mining Lv 11".
4. **Economy screen told you to process items that have no recipe** ("process first for more" on
   potions/repair kits). Fixed: the hint only renders when `sellAdvice` has a real better path.

Regression: full JVM suite 110 unique tests / 126 variant runs — 0 failures; instrumented
`EternalCriticalPathTest` 2/2; `:app:assembleDebug` green.

## 3. Usability friction (not bugs)

- **Currency "◎" is never explained.** No name, no glossary, no tooltip anywhere in onboarding; the
  header just shows an icon + amount. A playtester cannot tell this is Marks, how it's earned, or
  that quests are the reliable early source.
- **Market header jargon.** "Best region modifier: 1.0× · processed-goods demand 1.0×" is honest but
  meaningless to a new player with no glossary for "region modifier" or "processed-goods demand".
- **Long Buy list pushes Sell/Retainers below the fold.** Economy's Buy tab is tall; the Sell controls
  and the Workers/Hire block require scrolling past the entire list to discover.
- **"lv 0 required" phrasing.** Recipes show skill level gating as "lv 0 required" on the default
  recipes — reads awkwardly/confusing to a fresh player (it's not wrong, just ugly).
- **Outstanding-hint gap:** after the tool breaks, the header says "Next: Equip a tool" but nothing
  says "Repair/re-equip from the gear tab" — the fix shows the hint never mentions the repair path.
- **Rate mismatch unexplained.** Node card says "120.0/hr" (base rate) while the top bar shows
  "~146/hr" (effective, pickaxe bonus). Two different numbers with no footnote; players may read it
  as a bug.
- **Workers locked hint references unseen content.** "Workers unlock once the Deep Quartz Grotto is
  discovered" names a location the player has not seen yet.

## 4. Progression

- Early chain reads well: gather 3 Iron Ore (with the starter tool) → smelt → craft a Bronze Sword;
  rewards land visibly (+Marks, tool, unlocks).
- After crafting the sword there is a **lull**: the next tier of content is region-gated, and nothing
  in UI signals "go explore the next region" beyond the hint line.
- The offline stall bug (above) made progression feel dead; that is the single highest-impact issue
  found and it is fixed.
- 8h10m output on a fresh save: Lv 1→5, Mining→11, +884 Iron Ore / +150 Resonance Shard /
  +119 Brightleaf, +5950 Mining XP; pickaxe broken in the window. Feels like a healthy idle curve.

## 5. Economy

- Buy/Sell flow works and prices move with the furthest Reach toured; the premium mechanism is real
  but invisible (see jargon above).
- Sell-advice hint is correct and useful where a recipe exists (grain → Flour sells for more), and is
  now correctly absent where it doesn't.
- The ◎→Marks naming gap and reordering above are the main economy-UI asks.

## 6. Offline

- Report now reads well: activity line, "Character XP +1495", "Levels up: Character Lv 5, Mining
  Lv 11", per-skill XP, resource lines, discovery events ("A Resonance Shard tumbled out of a crack
  you never noticed before."), "Broken: Journeyman's Pickaxe" (a named item), and a completed-quests
  line. Continue → resumes cleanly with the next quest hint.
- Window capped at 12h (`SimConfig.MAX_OFFLINE_SECONDS`); report duration shown as "7h 58m".

## 7. Enhancement (not tested to completion this pass)

- Gear tab and enhancement tables inspected at code level in Phase 3A (shatter/protect/ward, band
  gating, Dawnreach chain) and covered by the sim suite; no on-device +31 run performed this
  checkpoint because the fresh save does not reach that tier in-session.

## 8. RPG/idle identity

- The experience is **idle/incremental-first**: dense numeric rows ("120.0/hr", "78%", "+884",
  "12◎/unit") read like a spreadsheet; the RPG soul is mostly in quest prose and combat.
- The wolf boss is the strongest RPG beat: it has tiers, xp, and a drama moment (armor stat "Pelt"
  ramps up mid-fight and the fight text explains why). But players are offered it on the Adventure
  home screen beside T1 gathering nodes with no "this is a boss / expect a long fight" framing, and
  the Pelt mechanic is never pre-explained (it reads mid-fight).
- No glossary/help system at all means jargon accumulates unremediated (◎, region modifier,
  processed-goods demand, Pelt, Deep Quartz Grotto).

## 9. Suggested improvements (design input only; NONE implemented — out of checkpoint scope)

Status: the five "cheap/high-confidence" items were implemented in the post-checkpoint UX pass
(2026-08-17, see `docs/decisions.md` §"Phase 3A post-playtest UX pass"): first-run glossary/help
sheet, currency naming, region-modifier explanation, node-rate reconciliation, and the repair
shortcut. The remaining suggestions (below) are still open design input.

- First-run "How to play" sheet or a glossary: name the currency (Marks/◎), region modifier,
  processed-goods demand. Small, high-leverage.
- Headline rate: show the effective per-hour (with bonuses) on the node card too, or add "(with
  tool)" so card vs top bar reconcile.
- Add a repair shortcut: when the active hint is "Equip a tool", deep-link to the Gear tab.
- Replace "lv 0 required" with "No level req." or fold into a lock style.
- Boss card framing ("ELITE — long fight, gear recommended") and a one-line note on the Pelt ramp.
- Economy tab reorder or an "Sell / Manage" summary so Buy doesn't bury Sell+Workers.
- (Known design space, already flagged in Phase 3A decisions) post-sword lull → give something
  visible to strive toward after "Craft a Bronze Sword".

## 10. Test status at handoff

- JVM suite: 110 unique / 126 variants, 0 fail / 0 error / 0 skip.
- Instrumented: `EternalCriticalPathTest` 2/2 (fresh-start creation + offline-report flow).
- `:app:assembleDebug` green; APK reinstalled and walked on-device post-fix.