package com.projecteternal.content

import com.projecteternal.model.CombatStats
import com.projecteternal.model.MonsterId

/** Static monster catalog. */
object Monsters {
    private val all: Map<MonsterId, MonsterDefinition> = listOf(
        MonsterDefinition(
            id = "wolf", name = "Grey Wolf", tier = 1, regionId = "hollowreach",
            stats = CombatStats(
                attack = 6, defense = 1, accuracy = 45, evasion = 8,
                critChance = 5, critMultiplier = 1.5, attackSpeed = 0.8, maxHp = 55,
            ),
            xpReward = 8,
            lootTable = listOf(
                LootEntry("pelt_wolf", chancePercent = 70),
                LootEntry("meat_wolf", chancePercent = 30),
                LootEntry("shard_resonance", chancePercent = 2, guaranteed = false),
            ),
            description = "A territorial grey wolf of the old woods.",
        ),
        MonsterDefinition(
            id = "boar", name = "Bristle Boar", tier = 1, regionId = "hollowreach",
            stats = CombatStats(
                attack = 8, defense = 3, accuracy = 42, evasion = 6,
                critChance = 4, critMultiplier = 1.5, attackSpeed = 0.7, maxHp = 80,
            ),
            xpReward = 11,
            lootTable = listOf(
                LootEntry("meat_boar", chancePercent = 60),
                LootEntry("pelt_boar", chancePercent = 40),
                LootEntry("shard_resonance", chancePercent = 1, guaranteed = false),
            ),
            description = "A fat, angry boar. Charges on sight.",
        ),
        MonsterDefinition(
            id = "guardian_golem", name = "Hollowreach Guardian", tier = 2, regionId = "hollowreach",
            boss = true,
            stats = CombatStats(
                attack = 16, defense = 10, accuracy = 60, evasion = 5,
                critChance = 10, critMultiplier = 1.6, attackSpeed = 0.5, maxHp = 320,
            ),
            xpReward = 120,
            lootTable = listOf(
                LootEntry("crystal_guardian", chancePercent = 100),
                LootEntry("crystal_advanced", chancePercent = 25, guaranteed = false),
                LootEntry("shard_resonance", chancePercent = 30, guaranteed = false),
            ),
            description = "An ancient stone sentinel guarding the pass to Emberreach.",
        ),
        MonsterDefinition(
            id = "ash_beast", name = "Ash-Spawned Brute", tier = 2, regionId = "emberreach",
            stats = CombatStats(
                attack = 21, defense = 8, accuracy = 62, evasion = 7,
                critChance = 8, critMultiplier = 1.5, attackSpeed = 0.6, maxHp = 150,
            ),
            xpReward = 26,
            lootTable = listOf(
                LootEntry("pelt_boar", chancePercent = 50),
                LootEntry("meat_boar", chancePercent = 50),
                LootEntry("crystal_advanced", chancePercent = 25, guaranteed = false),
                LootEntry("ward_of_stability", chancePercent = 5, guaranteed = false),
            ),
            description = "A brute pulled from the smouldering soil of Emberreach.",
        ),
        MonsterDefinition(
            id = "storm_herald", name = "Storm Herald", tier = 4, regionId = "stormreach",
            boss = true,
            stats = CombatStats(
                attack = 34, defense = 18, accuracy = 70, evasion = 12,
                critChance = 15, critMultiplier = 1.7, attackSpeed = 1.1, maxHp = 640,
            ),
            xpReward = 420,
            lootTable = listOf(
                LootEntry("core_storm", chancePercent = 100),
                LootEntry("crystal_guardian", chancePercent = 30, guaranteed = false),
                LootEntry("shard_resonance", chancePercent = 40, guaranteed = false),
            ),
            description = "Rides the squall itself on a leash of living lightning.",
        ),
        MonsterDefinition(
            id = "starfall_wyrm", name = "The Starfall Wyrm", tier = 5, regionId = "dawnreach",
            boss = true,
            stats = CombatStats(
                attack = 46, defense = 24, accuracy = 75, evasion = 14,
                critChance = 20, critMultiplier = 1.8, attackSpeed = 1.3, maxHp = 950,
            ),
            xpReward = 900,
            lootTable = listOf(
                LootEntry("catalyst_storm", chancePercent = 60),
                LootEntry("core_storm", chancePercent = 40),
                LootEntry("shard_resonance", chancePercent = 40, guaranteed = false),
                LootEntry("crystal_guardian", chancePercent = 30, guaranteed = false),
            ),
            description = "It fell through the sky and now it considers the pass its own. Its core is a catalyst ripe for the still.",
        ),

        // ================= PACK 01 — OUTER REACHES =================

        // ---- Cindervale (region:cindervale) ----
        MonsterDefinition(
            id = "cinder_hound", name = "Cinder Hound", tier = 2, regionId = "cindervale",
            stats = CombatStats(
                attack = 14, defense = 5, accuracy = 55, evasion = 10,
                critChance = 8, critMultiplier = 1.5, attackSpeed = 1.2, maxHp = 120,
            ),
            xpReward = 22,
            lootTable = listOf(
                LootEntry("meat_boar", chancePercent = 55),
                LootEntry("pelt_boar", chancePercent = 45),
                LootEntry("emberglass", chancePercent = 4, guaranteed = false),
            ),
            description = "A fast, hot-blooded hound of the ash flats.",
        ),
        MonsterDefinition(
            id = "scorch_viper", name = "Scorch Viper", tier = 2, regionId = "cindervale",
            stats = CombatStats(
                attack = 12, defense = 4, accuracy = 62, evasion = 12,
                critChance = 6, critMultiplier = 1.5, attackSpeed = 1.0, maxHp = 100,
            ),
            xpReward = 20,
            lootTable = listOf(
                LootEntry("pelt_boar", chancePercent = 50),
                LootEntry("saltash", chancePercent = 50),
                LootEntry("emberglass", chancePercent = 4, guaranteed = false),
            ),
            description = "Slithers through warm rock, all accuracy and spite.",
        ),
        MonsterDefinition(
            id = "ember_jackal", name = "Ember Jackal", tier = 2, regionId = "cindervale",
            stats = CombatStats(
                attack = 18, defense = 7, accuracy = 58, evasion = 9,
                critChance = 12, critMultiplier = 1.6, attackSpeed = 0.9, maxHp = 170,
            ),
            xpReward = 34,
            lootTable = listOf(
                LootEntry("pelt_boar", chancePercent = 40),
                LootEntry("meat_boar", chancePercent = 40),
                LootEntry("saltash", chancePercent = 20),
                LootEntry("emberglass", chancePercent = 4, guaranteed = false),
            ),
            description = "Hunts in packs; every bite tastes of charcoal.",
        ),
        MonsterDefinition(
            id = "cinder_wraith", name = "Cinder Wraith", tier = 3, regionId = "cindervale",
            stats = CombatStats(
                attack = 26, defense = 12, accuracy = 64, evasion = 14,
                critChance = 15, critMultiplier = 1.6, attackSpeed = 1.0, maxHp = 260,
            ),
            xpReward = 80,
            lootTable = listOf(
                LootEntry("cinder_fragment", chancePercent = 50),
                LootEntry("saltash", chancePercent = 50),
                LootEntry("emberglass", chancePercent = 8, guaranteed = false),
                LootEntry("ward_cinders", chancePercent = 2, guaranteed = false),
            ),
            description = "An ember that learned to walk. Rare, and very patient.",
        ),
        MonsterDefinition(
            id = "ash_sovereign", name = "Ash Sovereign", tier = 3, regionId = "cindervale",
            boss = true,
            stats = CombatStats(
                attack = 30, defense = 14, accuracy = 66, evasion = 10,
                critChance = 15, critMultiplier = 1.7, attackSpeed = 0.9, maxHp = 420,
            ),
            xpReward = 300,
            lootTable = listOf(
                LootEntry("cinder_core", chancePercent = 100),
                LootEntry("crystal_advanced", chancePercent = 40, guaranteed = false),
                LootEntry("emberglass", chancePercent = 4, guaranteed = false),
                LootEntry("ward_cinders", chancePercent = 5, guaranteed = false),
            ),
            description = "The crowned thing that guards the Glass-Pass. Its heart still ticks.",
        ),

        // ---- Stormreach deepening (region:stormreach) ----
        MonsterDefinition(
            id = "storm_boar", name = "Storm Boar", tier = 3, regionId = "stormreach",
            stats = CombatStats(
                attack = 22, defense = 10, accuracy = 62, evasion = 8,
                critChance = 10, critMultiplier = 1.6, attackSpeed = 0.8, maxHp = 220,
            ),
            xpReward = 48,
            lootTable = listOf(
                LootEntry("meat_boar", chancePercent = 50),
                LootEntry("pelt_boar", chancePercent = 50),
                LootEntry("stormcrystal", chancePercent = 6, guaranteed = false),
            ),
            description = "A boar armored in static and bad temper.",
        ),
        MonsterDefinition(
            id = "lightning_heron", name = "Lightning Heron", tier = 3, regionId = "stormreach",
            stats = CombatStats(
                attack = 20, defense = 8, accuracy = 68, evasion = 16,
                critChance = 12, critMultiplier = 1.6, attackSpeed = 1.1, maxHp = 180,
            ),
            xpReward = 44,
            lootTable = listOf(
                LootEntry("meat_boar", chancePercent = 40),
                LootEntry("stormgrain", chancePercent = 60),
                LootEntry("stormcrystal", chancePercent = 6, guaranteed = false),
            ),
            description = "A heron that hunts at the edge of every squall.",
        ),
        MonsterDefinition(
            id = "storm_ape", name = "Storm Ape", tier = 3, regionId = "stormreach",
            stats = CombatStats(
                attack = 28, defense = 12, accuracy = 60, evasion = 9,
                critChance = 12, critMultiplier = 1.6, attackSpeed = 0.8, maxHp = 260,
            ),
            xpReward = 70,
            lootTable = listOf(
                LootEntry("meat_boar", chancePercent = 55),
                LootEntry("pelt_boar", chancePercent = 45),
                LootEntry("stormcrystal", chancePercent = 8, guaranteed = false),
            ),
            description = "Bangs its chest and the sky answers.",
        ),
        MonsterDefinition(
            id = "stormcaller_elite", name = "Stormcaller Elite", tier = 4, regionId = "stormreach",
            stats = CombatStats(
                attack = 36, defense = 16, accuracy = 68, evasion = 14,
                critChance = 18, critMultiplier = 1.7, attackSpeed = 1.0, maxHp = 360,
            ),
            xpReward = 140,
            lootTable = listOf(
                LootEntry("storm_fragment", chancePercent = 60),
                LootEntry("stormcrystal", chancePercent = 40),
                LootEntry("shard_resonance", chancePercent = 30, guaranteed = false),
            ),
            description = "A herald of the squall, rare and crackling with purpose.",
        ),

        // ---- Frostreach (region:frostreach) ----
        MonsterDefinition(
            id = "frostmaw_wolf", name = "Frostmaw Wolf", tier = 3, regionId = "frostreach",
            stats = CombatStats(
                attack = 26, defense = 10, accuracy = 62, evasion = 10,
                critChance = 12, critMultiplier = 1.6, attackSpeed = 1.0, maxHp = 230,
            ),
            xpReward = 52,
            lootTable = listOf(
                LootEntry("pelt_wolf", chancePercent = 55),
                LootEntry("meat_wolf", chancePercent = 45),
                LootEntry("frostmoss", chancePercent = 20, guaranteed = false),
                LootEntry("frostvein", chancePercent = 2, guaranteed = false),
            ),
            description = "A wolf that hunts by the heat you give off.",
        ),
        MonsterDefinition(
            id = "ice_lynx", name = "Ice Lynx", tier = 3, regionId = "frostreach",
            stats = CombatStats(
                attack = 22, defense = 8, accuracy = 70, evasion = 20,
                critChance = 14, critMultiplier = 1.6, attackSpeed = 1.3, maxHp = 190,
            ),
            xpReward = 50,
            lootTable = listOf(
                LootEntry("pelt_wolf", chancePercent = 50),
                LootEntry("meat_wolf", chancePercent = 50),
                LootEntry("frostmoss", chancePercent = 30, guaranteed = false),
            ),
            description = "You see it only when it is already too late.",
        ),
        MonsterDefinition(
            id = "frost_titan", name = "Frost Titan", tier = 4, regionId = "frostreach",
            stats = CombatStats(
                attack = 40, defense = 20, accuracy = 66, evasion = 8,
                critChance = 15, critMultiplier = 1.7, attackSpeed = 0.7, maxHp = 420,
            ),
            xpReward = 150,
            lootTable = listOf(
                LootEntry("frost_fragment", chancePercent = 50),
                LootEntry("frost_lumber", chancePercent = 50),
                LootEntry("frostvein", chancePercent = 5, guaranteed = false),
            ),
            description = "A shard of the old glacier given shoulders.",
        ),
        MonsterDefinition(
            id = "glacier_warden", name = "Glacier Warden", tier = 4, regionId = "frostreach",
            boss = true,
            stats = CombatStats(
                attack = 44, defense = 22, accuracy = 70, evasion = 10,
                critChance = 18, critMultiplier = 1.7, attackSpeed = 0.8, maxHp = 780,
            ),
            xpReward = 520,
            lootTable = listOf(
                LootEntry("frostvein", chancePercent = 100),
                LootEntry("core_storm", chancePercent = 40, guaranteed = false),
                LootEntry("shard_resonance", chancePercent = 40, guaranteed = false),
                LootEntry("crystal_advanced", chancePercent = 25, guaranteed = false),
            ),
            description = "The old thing under the ice. Its waking feeds the foundry.",
        ),
    ).associateBy { it.id }

    fun get(id: MonsterId): MonsterDefinition = all[id]
        ?: error("Unknown monster def '$id' (content gap)")

    fun inRegion(regionId: String): List<MonsterDefinition> =
        all.values.filter { it.regionId == regionId }.sortedBy { it.tier }
}
