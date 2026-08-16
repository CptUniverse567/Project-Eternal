package com.projecteternal.sim

import com.projecteternal.content.Monsters
import com.projecteternal.model.CombatStats
import com.projecteternal.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatTest {

    private val engine = GameEngine()
    private val t0 = 1_700_000_000_000L

    private fun newPlayer(): GameState = engine.newGame("Tester", t0)

    @Test
    fun `damage is always at least 1`() {
        for (attack in 0..3) {
            for (defense in 0..20) {
                val dmg = CombatStatsMath.expectedHitDamage(attack, 0, 1.5, defense)
                assertTrue("damage floor broken at a=$attack d=$defense", dmg >= 1)
            }
        }
    }

    @Test
    fun `accuracy scales hit chance but never exceeds 95`() {
        assertEquals(95, CombatStatsMath.hitChance(500, 0))
        assertEquals(5, CombatStatsMath.hitChance(0, 500))
        assertEquals(50, CombatStatsMath.hitChance(60, 10))
    }

    @Test
    fun `crits raise expected damage`() {
        val noCrit = CombatStatsMath.expectedHitDamage(10, 0, 1.5, 0)
        val crit = CombatStatsMath.expectedHitDamage(10, 100, 2.0, 0)
        assertTrue("crit should increase expected damage", crit > noCrit)
    }

    @Test
    fun `unarmed player can still kill a wolf eventually`() {
        var s = newPlayer()
        val before = s.stats.killCounts["wolf"] ?: 0
        val result = CombatResolver.resolve(s, "wolf", 3600.0, carry = 0.0)
        assertTrue("expected some kills in an hour", result.kills > 0)
        assertTrue("xp follows kills", result.xpGained == result.kills * Monsters.get("wolf").xpReward)
        assertTrue("health never below floor", result.healthFinal > 0)
        s = s.copy(character = s.character.copy(health = result.healthFinal))
        assertTrue(s.character.health >= CombatStatsMath.effectiveStats(s).maxHp / 5)
    }

    @Test
    fun `equipping a sword massively increases kill rate`() {
        var s = newPlayer()
        val bareKills = Rates.killsPerHour(s, "wolf")
        // craft & equip a bronze sword
        s = s.addItem("iron_ingot", 20).addItem("wood", 10).unlock("recipe:sword_bronze")
        val crafted = engine.apply(
            s, GameIntent.StartActivity(com.projecteternal.model.ActivityType.CRAFTING, "craft_sword_bronze"), t0
        ).state
        val after = engine.apply(crafted, GameIntent.Tick(t0 + 30_000), t0 + 30_000).state
        val sword = after.equipmentItems.firstOrNull { it.defId == "sword_bronze" } ?: error("no sword crafted")
        s = engine.apply(after, GameIntent.Equip(sword.uid), t0).state
        val armedKills = Rates.killsPerHour(s, "wolf")
        assertTrue("sword should beat bare hands ($bareKills vs $armedKills)", armedKills > bareKills * 3)
    }

    @Test
    fun `loot resolver distributes exactly K guaranteed items`() {
        val wolf = Monsters.get("wolf")
        val rng = RandomSource(5L)
        val result = LootResolver.roll(1000, wolf.lootTable, rng)
        val guaranteedTotal = wolf.lootTable.filter { it.guaranteed }.sumOf { result.items[it.defId] ?: 0 }
        assertEquals("guaranteed drops must sum to kills", 1000L, guaranteedTotal)
    }

    @Test
    fun `loot resolver rare drops are sparse`() {
        val rng = RandomSource(11L)
        var shards = 0L
        repeat(20) {
            val result = LootResolver.roll(10, Monsters.get("wolf").lootTable, rng)
            shards += result.items["shard_resonance"] ?: 0
        }
        assertTrue("200 wolf kills should drop few shards, got $shards", shards <= 20)
    }

    @Test
    fun `combat determinism for same seed`() {
        fun run(): CombatResolver.CombatResult {
            var s = newPlayer()
            val (s2, rng) = RandomSource.next(s)
            s = s2
            return CombatResolver.resolve(s, "wolf", 3600.0, carry = 0.0)
        }
        val a = run()
        val b = run()
        assertEquals(a, b)
    }

    @Test
    fun `base stats of new player are sane`() {
        val stats = CombatStatsMath.effectiveStats(newPlayer())
        assertTrue(stats.attack >= 1)
        assertTrue(stats.maxHp >= 100)
        assertTrue(stats.attackSpeed > 0)
    }
}
