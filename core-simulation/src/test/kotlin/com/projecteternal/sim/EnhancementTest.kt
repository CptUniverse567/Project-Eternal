package com.projecteternal.sim

import com.projecteternal.content.EnhancementTables
import com.projecteternal.model.EnhancementBand
import com.projecteternal.model.ItemInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnhancementTest {

    private val state = com.projecteternal.model.GameState(saveId = "t").addItem("shard_resonance", 500)

    private fun item(level: Int, durability: Int = 100) =
        ItemInstance("i1", "sword_bronze", enhancementLevel = level, durability = durability, maxDurability = 100)

    @Test
    fun `successProbability rises with resolve and caps`() {
        val table = EnhancementTables.tableFor(5)!!
        val base = EnhancementResolver.successProbability(5, 0, table)
        val mid = EnhancementResolver.successProbability(5, 10, table)
        val capped = EnhancementResolver.successProbability(5, 10_000, table)
        assertTrue("resolve must help", mid > base)
        assertTrue("resolve must cap", capped <= 95.0)
        assertEquals(base + 10.0, mid, 0.001)
    }

    @Test
    fun `higher levels have lower base success`() {
        val low = EnhancementResolver.successProbability(2, 0, EnhancementTables.tableFor(2)!!)
        val high = EnhancementResolver.successProbability(14, 0, EnhancementTables.tableFor(14)!!)
        assertTrue(high < low)
    }

    @Test
    fun `attempts consume shards on success and failure`() {
        val rng = RandomSource(42L)
        val outcome = EnhancementResolver.attempt(item(0), resolve = 0, useProtection = false, hasProtection = false, rng = rng)
        assertEquals(1L, outcome.consumedShards)
        assertTrue(outcome.success) // +0 -> +1 is 100%
        assertEquals(1, outcome.newLevel)
        assertEquals(0L, outcome.newResolve)
    }

    @Test
    fun `failure accumulates resolve and success resets it`() {
        // Force failure: +14 at level 14 (base 12%), use a fixed seed loop
        var rng = RandomSource(7L)
        var success = false
        var attempt: EnhancementResolver.AttemptResult? = null
        var guard = 0
        while (!success && guard < 200) {
            attempt = EnhancementResolver.attempt(item(14), resolve = 0, useProtection = false, hasProtection = false, rng = rng)
            success = attempt.success
            guard++
        }
        assertTrue("eventually succeeds", success)
        val outcome = attempt!!
        if (outcome.success) {
            assertEquals(0L, outcome.newResolve)
            assertTrue(outcome.newLevel >= 13)
        }
    }

    @Test
    fun `high-level failure downgrades unless protected`() {
        val table = EnhancementTables.tableFor(12)!!
        assertEquals(com.projecteternal.content.FailureConsequence.DOWNGRADE_ONE, table.failure)

        // Craft a forced-failure attempt at +12 and check downgrade behavior
        var foundFailure = false
        var rng = RandomSource(99L)
        var guard = 0
        while (!foundFailure && guard < 500) {
            val outcome = EnhancementResolver.attempt(item(12), resolve = 0, useProtection = false, hasProtection = false, rng = rng)
            if (!outcome.success) {
                foundFailure = true
                assertEquals(11, outcome.newLevel)
                assertTrue(outcome.newResolve > 0)
            }
            guard++
        }
        assertTrue("a failure must occur eventually", foundFailure)

        // Protected failure keeps the level
        var protectedFail = false
        rng = RandomSource(99L)
        guard = 0
        while (!protectedFail && guard < 500) {
            val outcome = EnhancementResolver.attempt(item(12), resolve = 0, useProtection = true, hasProtection = true, rng = rng)
            if (!outcome.success) {
                protectedFail = true
                assertEquals(12, outcome.newLevel)
                assertTrue(outcome.consumedProtection)
            }
            guard++
        }
        assertTrue("a protected failure must occur eventually", protectedFail)
    }

    @Test
    fun `engine enhancement is deterministic for a fixed seed`() {
        fun run(): com.projecteternal.model.GameState {
            var s = state
            s = s.addEquipmentItem(item(0))
            s = s.addItem("shard_resonance", 500)
            var guard = 0
            while (s.itemInstance("i1")!!.enhancementLevel < 3 && guard < 100) {
                s = GameEngine().apply(s, GameIntent.Enhance("i1", useProtection = false), 0L).state
                guard++
            }
            return s
        }
        val a = run()
        val b = run()
        assertEquals(a, b)
        assertTrue(a.itemInstance("i1")!!.enhancementLevel >= 3)
    }

    @Test
    fun `no free infinite progression - costs grow and ceiling exists`() {
        val table = EnhancementTables.tableFor(15)!!
        assertEquals(10, table.baseSuccessPercent[15]!!)
        val shardsAt15 = EnhancementTables.tableFor(15)!!.shardsPerAttempt[15]!!
        assertTrue(shardsAt15 > EnhancementTables.tableFor(0)!!.shardsPerAttempt[0]!!)
        assertFalse(EnhancementResolver.successProbability(15, 0, table) >= 100.0)
        // at max enhancement, no table exists -> precondition fails
        val maxed = item(EnhancementTables.maxEnhancementLevel)
        val pre = EnhancementResolver.checkPreconditions(state, maxed)
        assertFalse(pre.ok)
    }

    @Test
    fun `advanced band requires voidforged crystals`() {
        val table = EnhancementTables.tableFor(16)!!
        assertEquals(EnhancementBand.ADVANCED, table.band)
        val material = table.materialPerAttempt
        assertEquals("crystal_advanced", material?.defId)
        val poor = com.projecteternal.model.GameState(saveId = "x")
            .addItem("shard_resonance", 500)
        val inst = item(16)
        val pre = EnhancementResolver.checkPreconditions(
            poor.addEquipmentItem(inst), inst
        )
        assertFalse("no crystals -> cannot attempt", pre.ok)
    }

    @Test
    fun `full negating ward blocks downgrade and durability together`() {
        val table = EnhancementTables.tableFor(12)!!
        assertEquals(com.projecteternal.content.FailureConsequence.DOWNGRADE_ONE, table.failure)

        // Without ward, a failure at +12 downgrades and loses durability.
        var rng = RandomSource(1234L)
        var plainFail = false
        var guard = 0
        while (!plainFail && guard < 400) {
            val o = EnhancementResolver.attempt(item(12), resolve = 0, useProtection = false, hasProtection = false, rng = rng)
            if (!o.success) {
                plainFail = true
                assertEquals(11, o.newLevel)
                assertEquals(10, o.durabilityLoss)
            }
            guard++
        }
        assertTrue("plain failure must occur", plainFail)

        // With the ward, the same seeded failures keep level AND durability.
        rng = RandomSource(1234L)
        var wardFail = false
        guard = 0
        while (!wardFail && guard < 400) {
            val o = EnhancementResolver.attempt(
                item(12), resolve = 0, useProtection = false, hasProtection = true,
                rng = rng, useFullNegation = true, hasFullNegation = true,
            )
            if (!o.success) {
                wardFail = true
                assertEquals(12, o.newLevel)
                assertEquals(0, o.durabilityLoss)
                assertTrue(o.consumedFullNegation)
                assertFalse(o.consumedProtection)
            }
            guard++
        }
        assertTrue("ward-protected failure must occur", wardFail)
    }

    @Test
    fun `engine consumes the ward item only when used`() {
        var s = state.addEquipmentItem(item(12)).addItem("ward_of_stability", 2).addItem("shard_resonance", 5000)
        // force failure +12 by drilling until state no longer succeeds
        var guard = 0
        var lastState = s
        while (guard < 80) {
            lastState = GameEngine().apply(
                lastState, GameIntent.Enhance("i1", useProtection = true, useFullNegation = true), 0L
            ).state
            if (lastState.itemInstance("i1")!!.enhancementLevel == 11) break
            if (lastState.inventoryCount("ward_of_stability") <= 0) break
            guard++
        }
        assertTrue(
            "a ward should be consumed when a failure is negated",
            lastState.inventoryCount("ward_of_stability") < 2
        )
        assertTrue("item stayed at +12 when ward used", lastState.itemInstance("i1")!!.enhancementLevel >= 11)
    }

    @Test
    fun `plain enhance without consumables stays identical to before`() {
        var s = state.addEquipmentItem(item(0))
        val rngA = RandomSource(55L)
        val plainDefault = EnhancementResolver.attempt(item(0), 0, useProtection = false, hasProtection = false, rng = rngA)
        val rngB = RandomSource(55L)
        val negativeDefault = EnhancementResolver.attempt(
            item(0), 0, useProtection = false, hasProtection = false, rng = rngB,
            useFullNegation = false, hasFullNegation = false,
        )
        assertEquals(plainDefault.newLevel, negativeDefault.newLevel)
        assertEquals(plainDefault.consumedFullNegation, negativeDefault.consumedFullNegation)
    }
}
