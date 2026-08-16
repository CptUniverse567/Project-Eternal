# PROJECT ETERNAL --- ETERNAL EXPANSION MASTER PROMPT

**Document:** `project-eternal-eternal-expansion-master-prompt.md`\
**Purpose:** Long-term content expansion and progression roadmap for
Project Eternal\
**Current baseline:** `0.1.0-slice` Release Candidate\
**Execution model:** OpenCode autonomous implementation, with ChatGPT
acting as architectural/product reviewer between content packs

------------------------------------------------------------------------

# 1. ROLE

You are **OpenCode**, acting as the autonomous lead engineer and content
systems engineer for **Project Eternal**.

Project Eternal is an **offline-first Android idle RPG** inspired by the
progression fantasy of large-scale MMORPGs:

-   combat
-   monsters and bosses
-   gathering
-   processing
-   crafting
-   equipment
-   enhancement
-   workers/retainers
-   regional economies
-   quests
-   offline progression
-   long-term character progression
-   effectively unlimited progression

The game must remain **purely offline**.

There is no requirement for:

-   multiplayer
-   server-side progression
-   live events
-   online trading
-   cloud-dependent simulation
-   mandatory accounts
-   real-time player interaction

The existing repository is already a functioning, tested game. This
document governs **expansion**, not reconstruction.

------------------------------------------------------------------------

# 2. READ THESE BEFORE DOING ANYTHING

Before implementing any expansion work, read:

1.  `project-eternal-opencode-prompt.md`
2.  `docs/decisions.md`
3.  `docs/playtest-phase-3a.md` if present
4.  `docs/release-candidate.md` if present
5.  The complete current source tree
6.  Existing tests, especially:
    -   simulation tests
    -   content integrity tests
    -   quest tests
    -   enhancement tests
    -   activity/industry tests
    -   retainer tests
    -   offline simulation tests
    -   persistence tests

The original master prompt remains authoritative for:

-   architecture
-   module boundaries
-   offline simulation
-   persistence
-   testing philosophy
-   existing gameplay rules
-   existing content
-   engineering constraints

**This document does not replace the original master prompt.**

It adds the long-term expansion strategy.

If this document conflicts with the original engineering architecture,
preserve the existing architecture unless a deliberate architectural
decision is documented.

------------------------------------------------------------------------

# 3. CURRENT BASELINE

The current game has already passed a substantial release-candidate
audit.

The proven baseline includes:

-   character creation
-   quests
-   combat
-   gathering
-   mining
-   logging
-   farming
-   fishing
-   herbalism
-   processing
-   crafting
-   equipment
-   durability
-   repair
-   enhancement
-   BASE enhancement
-   ADVANCED enhancement
-   TRANSCENDENT enhancement
-   Resolve/failstack mechanics
-   oil protection
-   Ward of Stability/full-negation protection
-   shatter mechanics
-   enhancement catalysts
-   workers/retainers
-   worker recruitment
-   worker traits
-   node production
-   regional economy
-   market buying/selling
-   processed-goods demand premium
-   regional modifiers
-   offline progression
-   save/restart persistence
-   quest gating
-   region progression
-   first-run help
-   UI state/navigation
-   API-36 instrumented coverage
-   extensive JVM regression coverage

Current implemented regions include:

-   Hollowreach
-   Emberreach
-   Stormreach
-   Dawnreach architecture/content gate

The current release candidate has been manually and automatically
verified.

**Treat the current implementation as a proven foundation.**

Do not casually rewrite it.

------------------------------------------------------------------------

# 4. EXPANSION OBJECTIVE

The objective is to transform the current release candidate into a
**large, interconnected, long-term offline RPG** capable of providing
meaningful progression for months and eventually supporting effectively
unlimited progression.

Do NOT interpret this as:

> "Generate as many items, monsters, or recipes as possible."

Instead:

> **Build a coherent world in which systems feed one another and every
> major content addition creates meaningful player decisions.**

The player should continually have reasons to:

-   gather
-   process
-   craft
-   fight
-   enhance
-   explore
-   recruit
-   optimize
-   trade
-   complete quests
-   unlock regions
-   pursue better equipment
-   specialize
-   return after offline time

------------------------------------------------------------------------

# 5. CORE DESIGN PRINCIPLES

## 5.1 Interconnection over volume

New content should connect multiple existing systems.

A resource is more valuable when it can participate in several chains:

``` text
Resource
  ↓
Processing
  ↓
Crafting
  ↓
Equipment
  ↓
Combat
  ↓
New Region
  ↓
New Resource
  ↓
Enhancement
```

Avoid isolated content.

------------------------------------------------------------------------

## 5.2 Every region needs identity

A new region must not merely be:

> Existing region + stronger numbers.

Each meaningful region should have some combination of:

-   distinct resources
-   distinct monsters
-   unique processing chains
-   regional economic behavior
-   special gathering behavior
-   worker opportunities
-   unique equipment
-   quest identity
-   regional mechanic
-   unique boss
-   unique progression gate

A region should answer:

> **Why does this place exist?**

------------------------------------------------------------------------

## 5.3 Meaningful progression, not artificial waiting

Do not inflate progression by arbitrarily increasing:

-   health
-   resource requirements
-   timers
-   enhancement costs
-   XP requirements

The player should spend time making decisions, optimizing systems, and
pursuing goals.

Idle duration can be part of the design, but **waiting must not be the
only gameplay.**

------------------------------------------------------------------------

## 5.4 Offline-first

Every new system must work correctly when the app is:

-   foregrounded
-   backgrounded
-   closed
-   reopened after elapsed time

Avoid mechanics that require a live server.

If a mechanic depends on time, it must be compatible with the existing
offline simulator.

------------------------------------------------------------------------

## 5.5 No placeholder content

Never create:

-   fake systems
-   dummy items
-   empty regions
-   placeholder bosses pretending to be complete
-   recipes with no meaningful purpose
-   unlocks that lead nowhere
-   UI promises with no underlying implementation

If content is presented as available, it must actually work.

------------------------------------------------------------------------

## 5.6 Preserve simulation authority

The simulation engine is authoritative.

UI validation may improve UX, but the engine must enforce gameplay
invariants.

For every new activity:

``` text
UI
 ↓
Intent
 ↓
GameController
 ↓
Simulation Engine
 ↓
GameState
 ↓
StateFlow / Persistence
 ↓
UI
```

Do not rely solely on UI guards.

This rule exists because the existing Industry Start bug demonstrated
how a UI-only invariant can allow an invalid state into the simulation.

------------------------------------------------------------------------

# 6. LONG-TERM PROGRESSION MODEL

Project Eternal should eventually support progression through multiple
layers.

A conceptual model:

``` text
CHARACTER
  ↓
SKILLS
  ↓
EQUIPMENT
  ↓
ENHANCEMENT
  ↓
INDUSTRY
  ↓
WORKERS
  ↓
REGIONS
  ↓
ADVANCED EQUIPMENT
  ↓
TRANSCENDENT
  ↓
ASCENDANT
  ↓
ASCENDANCY / PRESTIGE
  ↓
ETERNAL / RECURSIVE PROGRESSION
```

This is a design direction, not permission to implement every layer
immediately.

The architecture must allow future tiers without hardcoded ceilings.

------------------------------------------------------------------------

# 7. REACH / REGION FRAMEWORK

Regions are the backbone of long-term content.

Every major Reach should define:

-   `RegionDefinition`
-   tier
-   unlock token
-   regional mechanic
-   price/economic modifier where appropriate
-   gathering nodes
-   resource families
-   monsters
-   elite encounters where appropriate
-   boss
-   quests
-   equipment
-   processing chains
-   crafting recipes
-   worker opportunities
-   enhancement materials
-   progression gates

A Reach should normally introduce at least one meaningful new
interaction.

Examples of possible regional mechanics:

-   gathering yield modification
-   resource specialization
-   dangerous gathering
-   rare-resource chance
-   production bonus
-   market specialization
-   monster loot specialization
-   worker affinity
-   processing efficiency
-   enhancement-material concentration

Do not reuse the same mechanic across every region.

------------------------------------------------------------------------

# 8. CONTENT ECOSYSTEMS

New content should form ecosystems.

Example:

``` text
Copper Ore
   ↓
Copper Fragment
   ↓
Copper Ingot
   ↓
Copper Alloy
   ↓
Weapon / Armor
   ↓
Combat
   ↓
New Monster Materials
   ↓
Advanced Equipment
```

Another:

``` text
Herb
   ↓
Extract
   ↓
Potion
   ↓
Combat sustain
```

Another:

``` text
Monster Hide
   ↓
Leather
   ↓
Armor
   ↓
Equipment progression
```

Prefer chains that create multiple sinks and sources.

------------------------------------------------------------------------

# 9. LIFESKILL EXPANSION

Existing lifeskills must be expanded with depth rather than duplicated
labels.

Possible long-term areas include:

### Gathering

-   Mining
-   Logging
-   Farming
-   Fishing
-   Herbalism
-   future gathering disciplines if justified

### Processing

-   Grinding
-   Smelting
-   Drying
-   Milling
-   Refining
-   future processing disciplines

### Crafting

-   Weapons
-   Armor
-   Tools
-   Consumables
-   Accessories
-   Production equipment
-   Enhancement components
-   future specialized crafting

New skills should only be introduced when they create meaningful
gameplay.

------------------------------------------------------------------------

# 10. COMBAT EXPANSION

Combat content should scale in depth.

Add:

-   normal monsters
-   stronger variants
-   elites
-   bosses
-   regional bosses
-   rare encounters
-   meaningful loot tables

Do not simply multiply HP and damage.

New enemies should be differentiated by combinations of:

-   durability
-   damage
-   loot
-   regional role
-   encounter frequency
-   progression relevance
-   special behavior where supported by the existing architecture

Bosses should feel like actual milestones.

------------------------------------------------------------------------

# 11. EQUIPMENT EXPANSION

Equipment should create meaningful decisions.

Potential categories:

-   weapons
-   armor
-   tools
-   accessories
-   specialized gear
-   production equipment

Equipment should interact with:

-   combat
-   gathering
-   lifeskills
-   enhancement
-   durability
-   economy

Avoid producing dozens of statistically identical items.

Prefer meaningful equipment families.

------------------------------------------------------------------------

# 12. ENHANCEMENT EXPANSION

Enhancement is a major long-term progression system.

Future tiers should be data-driven.

Avoid hardcoding an eventual ceiling.

Higher bands may introduce:

-   new materials
-   new failure states
-   protection mechanics
-   new resource sinks
-   new strategic choices

Do not make higher enhancement merely:

> lower success chance + higher material cost.

Every major band should have a reason to exist.

------------------------------------------------------------------------

# 13. WORKER / RETAINER EXPANSION

Workers should eventually become a meaningful optimization layer.

Expand:

-   worker catalog
-   traits
-   specialization
-   node affinity
-   production bonuses
-   speed
-   luck
-   regional bonuses
-   milestone progression

Avoid simply adding workers with identical statistics.

A worker should create a decision:

> Which worker is best for this node and my current objective?

------------------------------------------------------------------------

# 14. ECONOMY EXPANSION

The economy should remain understandable while becoming deeper.

Potential systems:

-   regional demand
-   processed-good premiums
-   resource scarcity
-   specialization
-   production arbitrage
-   market sinks
-   expensive equipment
-   worker investment
-   crafting profitability

Do not make the economy require online players.

It must remain deterministic/offline.

Never create meaningless price manipulation purely for complexity.

------------------------------------------------------------------------

# 15. QUEST EXPANSION

Quests should teach and motivate systems.

Use several types:

### Main quests

Advance the world and unlock major regions.

### Regional quests

Explain regional identity.

### Lifeskill quests

Teach gathering/processing/crafting.

### Combat quests

Introduce monster families and bosses.

### Industry quests

Create production goals.

### Enhancement quests

Create long-term goals.

### Exploration quests

Encourage discovering nodes/content.

Avoid quest chains that require actions the player cannot reasonably
perform.

Historical completion objectives must not regress.

------------------------------------------------------------------------

# 16. CONTENT PACK SYSTEM

Do not attempt to implement the entire long-term roadmap in one
uncontrolled pass.

Use **Content Packs**.

Each pack must be:

-   coherent
-   independently testable
-   internally interconnected
-   playable
-   documented
-   reported to ChatGPT
-   stopped at completion

Recommended roadmap:

``` text
CURRENT RELEASE CANDIDATE
        ↓
PACK 01 — OUTER REACHES
        ↓
PACK 02 — MASTERY
        ↓
PACK 03 — WORLD DEPTH
        ↓
PACK 04 — TRANSCENDENCE
        ↓
PACK 05 — ASCENDANCY
        ↓
PACK 06+ — ETERNAL / RECURSIVE
```

These names are provisional.

The content and progression purpose are more important than the names.

------------------------------------------------------------------------

# 17. PACK 01 --- OUTER REACHES

The first expansion should convert the current early game into a
substantial mid-game.

Target scope:

### World

-   approximately 2--3 substantial new Reaches
-   approximately 8--12 meaningful new nodes
-   distinct regional mechanics

### Combat

-   approximately 15--25 new monsters
-   approximately 3--5 meaningful bosses
-   optional elite/special encounters where justified

### Gathering

-   new resources across existing lifeskills
-   rare gathering resources
-   regional specialization

### Processing

-   approximately 15--30 meaningful new recipes/chains

### Crafting

-   approximately 20--40 meaningful recipes across relevant categories

### Equipment

-   new equipment family/families
-   meaningful progression over existing gear

### Workers

-   approximately 6--12 new workers
-   expanded trait pool
-   specialization opportunities

### Quests

-   continuation of the main chain
-   regional questlines
-   lifeskill quests
-   industry quests
-   enhancement goals

### Economy

-   new regional economic interactions
-   useful new market sinks/sources

### Progression

-   extend the meaningful progression ceiling
-   add at least one new long-term goal
-   preserve existing progression

These are **scope targets, not mandatory quotas**.

If hitting a numerical target would require filler, do not hit it.

Quality and interconnectedness take priority.

------------------------------------------------------------------------

# 18. FUTURE CONTENT PACKS

## Pack 02 --- Mastery

Focus:

-   deeper lifeskill progression
-   worker specialization
-   production optimization
-   advanced recipes
-   resource mastery
-   meaningful specialization decisions

## Pack 03 --- World Depth

Focus:

-   additional Reaches
-   stronger regional identities
-   larger monster ecosystems
-   bosses
-   deeper economy
-   rare resources
-   advanced equipment

## Pack 04 --- Transcendence

Focus:

-   high-end enhancement
-   advanced materials
-   deeper equipment progression
-   high-end bosses
-   long-term objectives

Do not duplicate the already implemented Transcendent system; expand its
ecosystem.

## Pack 05 --- Ascendancy

Focus:

-   Ascendant progression
-   prestige/recursive systems if validated by design
-   new resource layers
-   long-term optimization

Do not implement until the prerequisite progression is proven enjoyable.

## Pack 06+

Focus:

-   recursive progression
-   procedural/content-driven expansion
-   effectively unlimited Reaches
-   new mastery layers
-   new systems only when justified

------------------------------------------------------------------------

# 19. ANTI-BLOAT RULES

## Rule A --- No filler

Never create content merely to increase counts.

## Rule B --- Resources need sinks

Every significant resource should have at least one useful sink.

Prefer multiple sinks.

## Rule C --- Systems should interact

A good addition affects more than one existing system.

## Rule D --- Regions need identity

Do not produce reskinned regions.

## Rule E --- Avoid meaningless rarity

Do not create "Rare Iron Ore" that is functionally identical to Iron Ore
except for a lower drop chance.

## Rule F --- Avoid artificial timers

Do not extend progression solely by making activities slower.

## Rule G --- Avoid complexity for complexity's sake

A mechanic must create a meaningful decision or solve a design problem.

## Rule H --- No online dependency

Expansion remains offline-first.

## Rule I --- No fake infinity

Do not claim infinite progression while secretly hardcoding an arbitrary
ceiling.

## Rule J --- No silent architectural debt

If expansion requires a significant architecture change, stop and
document the decision before implementing it.

------------------------------------------------------------------------

# 20. BALANCE PHILOSOPHY

Do not optimize solely for theoretical progression duration.

Evaluate:

-   decision density
-   reward frequency
-   meaningful upgrades
-   resource usefulness
-   player comprehension
-   offline satisfaction
-   long-term goals

A player should frequently have something they understand and want to
pursue.

Avoid:

> "The next upgrade takes 400 hours because we need the game to last."

Prefer:

> "There are several things I could pursue during those 400 hours."

------------------------------------------------------------------------

# 21. PLAYTEST PHILOSOPHY

Automated tests prove correctness.

They do not prove fun.

After each major Content Pack:

1.  Build APK.
2.  Run automated tests.
3.  Run instrumented tests.
4.  Perform representative emulator playthrough.
5.  Produce Progress Report.
6.  Stop.

Human playtesting should then determine whether the next pack should:

-   expand
-   rebalance
-   simplify
-   deepen
-   or replace a weak mechanic.

------------------------------------------------------------------------

# 22. ENGINEERING SAFETY

Before modifying shared systems:

-   inspect existing implementation
-   identify authoritative state
-   identify existing invariants
-   add regression coverage
-   preserve existing behavior

Never solve a simulation bug solely in the UI.

Never duplicate simulation logic unnecessarily.

Never break save compatibility without a documented migration.

Never modify unrelated systems while implementing content unless
required.

------------------------------------------------------------------------

# 23. TEST REQUIREMENTS FOR EVERY CONTENT PACK

Every pack must include appropriate tests for:

-   content integrity
-   quest gates
-   resource chains
-   recipes
-   combat/loot
-   worker behavior
-   economy
-   enhancement where applicable
-   offline progression
-   persistence where applicable

At the end of the pack:

-   full JVM suite must pass
-   instrumented critical path must pass
-   debug APK must build
-   representative emulator playthrough must pass

No known release-blocking failure may remain.

------------------------------------------------------------------------

# 24. CONTENT IMPLEMENTATION WORKFLOW

For each Content Pack:

## Phase A --- Design

Define:

-   regions
-   progression
-   resources
-   chains
-   monsters
-   bosses
-   equipment
-   workers
-   quests
-   economic effects
-   new mechanics

Do not code yet.

## Phase B --- Architecture Check

Determine whether existing architecture supports the content.

If yes, use it.

If no, document the required architectural change.

## Phase C --- Implementation

Implement content in coherent groups.

Prefer data-driven definitions.

## Phase D --- Tests

Add integrity and behavioral tests.

## Phase E --- Integration

Verify cross-system interactions.

## Phase F --- Emulator

Perform a representative playthrough.

## Phase G --- Report

Produce the ChatGPT Progress Report.

## Phase H --- STOP

Do not automatically begin the next Content Pack.

------------------------------------------------------------------------

# 25. CHATGPT PROGRESS REPORT PROTOCOL

At the end of every Content Pack, provide:

# PROJECT ETERNAL --- EXPANSION PROGRESS REPORT

## Status

-   Pack:
-   Version:
-   Status:
-   Previous progression ceiling:
-   New progression ceiling:

## World

-   New Reaches:
-   New nodes:
-   Regional mechanics:

## Combat

-   New monsters:
-   New elites:
-   New bosses:

## Lifeskills

-   Skills expanded:
-   New resources:

## Industry

-   Processing chains:
-   Crafting recipes:

## Equipment

-   New equipment:
-   New enhancement materials:
-   New progression:

## Workers

-   New workers:
-   New traits:
-   New specializations:

## Quests

-   Main quests:
-   Regional quests:
-   Lifeskill quests:
-   Other objectives:

## Economy

-   New market behavior:
-   New sinks/sources:

## Systems

-   New mechanics:
-   Architecture changes:

## Testing

-   JVM tests:
-   Failures:
-   Instrumented tests:
-   Emulator result:
-   APK build result:

## Playtest

-   What worked:
-   What felt weak:
-   UX issues:
-   Functional bugs:
-   Balance concerns:

## Save / Offline

-   Persistence result:
-   Offline result:

## Remaining Work

List only actual remaining work.

## Recommendation

Recommend the next logical development direction, but **do not
automatically implement it**.

STOP after the report.

------------------------------------------------------------------------

# 26. PROGRESS REPORT RULES

The report must distinguish:

### Implemented

Actually present and tested.

### Partially implemented

Functional but intentionally incomplete.

### Planned

Not implemented.

Never present planned content as existing.

Never claim a system is complete merely because its data structures
exist.

------------------------------------------------------------------------

# 27. VERSIONING

Content Packs should use clear versioning.

Example:

``` text
0.1.0-slice     Current release candidate
0.2.0           Expansion Pack 01
0.3.0           Expansion Pack 02
...
```

Do not change versioning arbitrarily.

Document version decisions.

------------------------------------------------------------------------

# 28. GIT / CHECKPOINTING

Before each major Content Pack:

-   ensure the working tree is understood
-   create a clean checkpoint
-   commit the previous stable state

At the end of each Content Pack:

-   commit the completed pack
-   ensure tests are green
-   provide the exact commit/version in the Progress Report

Do not rewrite history unnecessarily.

------------------------------------------------------------------------

# 29. CURRENT TASK --- DO NOT IMPLEMENT YET

The immediate task after receiving this document is **planning only**.

Do NOT implement Pack 01 yet.

First:

1.  Read this document completely.
2.  Read `project-eternal-opencode-prompt.md`.
3.  Read `docs/decisions.md`.
4.  Read relevant playtest/release documentation.
5.  Inspect the current repository.
6.  Map the existing content and progression.
7.  Identify content gaps.
8.  Design the multi-pack expansion roadmap.
9.  Fully specify **Pack 01 --- Outer Reaches**.
10. Identify any architectural prerequisites.
11. Identify risks to the existing stable core.

Then produce a **Planning Progress Report for ChatGPT**.

Do not modify gameplay code during this planning task.

------------------------------------------------------------------------

# 30. PLANNING PROGRESS REPORT

The planning report must include:

## Current Content Map

-   regions
-   lifeskills
-   resources
-   processing chains
-   crafting
-   equipment
-   monsters
-   bosses
-   workers
-   quests
-   enhancement bands
-   economic systems

## Current Progression Map

Show how the player currently advances.

## Content Gaps

Identify where progression becomes thin or repetitive.

## Expansion Roadmap

Propose Content Packs 01--05+.

For each:

-   purpose
-   major systems
-   regions
-   progression
-   dependencies

## Pack 01 Detailed Design

Specify:

-   each proposed Reach
-   regional identity
-   resources
-   resource chains
-   monsters
-   bosses
-   nodes
-   equipment
-   workers
-   quests
-   economic mechanics
-   progression gates
-   enhancement interactions

## Architecture Assessment

State whether current architecture can support Pack 01.

If changes are needed:

-   why
-   where
-   risk
-   migration considerations

## Balance Risks

Identify likely:

-   progression bottlenecks
-   resource bottlenecks
-   runaway economy
-   excessive idle waiting
-   overpowered workers
-   enhancement inflation

## Testing Plan

Describe what tests Pack 01 will require.

## Recommended Implementation Order

Give a dependency-aware sequence.

Then STOP.

------------------------------------------------------------------------

# 31. FINAL PRINCIPLE

Project Eternal should become **large because its systems are
interconnected**, not because it contains a large number of database
rows.

The desired player experience is:

> "There is always something meaningful I can work toward."

Not:

> "There is always another number to increase."

Build a world.

Build systems that feed one another.

Build progression that creates decisions.

Preserve the offline philosophy.

Preserve the proven core.

Expand deliberately.

And never sacrifice correctness or coherence merely to increase content
volume.
