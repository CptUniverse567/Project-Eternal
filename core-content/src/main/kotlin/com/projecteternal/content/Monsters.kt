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
    ).associateBy { it.id }

    fun get(id: MonsterId): MonsterDefinition = all[id]
        ?: error("Unknown monster def '$id' (content gap)")

    fun inRegion(regionId: String): List<MonsterDefinition> =
        all.values.filter { it.regionId == regionId }.sortedBy { it.tier }
}
