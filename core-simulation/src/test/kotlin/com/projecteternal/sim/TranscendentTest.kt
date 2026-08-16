package com.projecteternal.sim

import com.projecteternal.content.EnhancementTables
import com.projecteternal.content.FailureConsequence
import com.projecteternal.model.ActivityState
import com.projecteternal.model.ActivityType
import com.projecteternal.model.Character
import com.projecteternal.model.EnhancementBand
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemInstance
import com.projecteternal.model.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 3A: the Transcendent band. It must be materially more expensive,
 * functionally more powerful and mechanically distinct from ADVANCED, gated
 * behind the Stormbound Catalyst chain, and discoverable+acerbic enough that
 * enhancing near the ceiling is a real strategic decision.
 */
class TranscendentTest {

    private val engine = GameEngine()
    private val clock = 2_000_000_000_000L

    private fun act(s: GameState, intent: GameIntent): GameState =
        engine.apply(s, intent, clock).state

    private fun sword(level: Int, uid: String = "blade") =
        ItemInstance(uid, "sword_bronze", enhancementLevel = level, durability = 100, maxDurability = 100)

    private fun openState(level: Int = 31): GameState =
        GameState(saveId = "t").unlock("screen:enhance", "recipe:refine_catalyst")
            .addItem("shard_resonance", 50_000)
            .addItem("catalyst_storm", 50)
            .addEquipmentItem(sword(level))

    /** Bare state with q_intro already accepted so apply() is a true no-op. */
    private fun settledState(vararg unlocks: String): GameState =
        GameState(saveId = "t").acceptQuests(listOf("q_intro"), clock).unlock(*unlocks)

    // ---- band data ----

    @Test
    fun `transcendent table carries material unlock and its own stat growth`() {
        val table = EnhancementTables.tableFor(31)!!
        assertEquals(EnhancementBand.TRANSCENDENT, table.band)
        assertEquals("catalyst_storm", table.materialPerAttempt?.defId)
        assertEquals(1L, table.materialPerAttempt!!.count)
        assertEquals("recipe:refine_catalyst", table.unlockToken)
        assertEquals(FailureConsequence.SHATTER_TO_BAND_FLOOR, table.failure)
        assertTrue("transcendent must outgrow advanced", table.statMultiplierPerLevel > 0.15)
    }

    // ---- engine: material + gate ----

    @Test
    fun `engine consumes exactly one catalyst per transcendent attempt`() {
        val before = openState(31)
        val after = act(before, GameIntent.Enhance("blade", useProtection = false))
        assertEquals(
            "exactly one catalyst per attempt, success or failure",
            before.inventoryCount("catalyst_storm") - 1, after.inventoryCount("catalyst_storm")
        )
        assertEquals(
            "shards consumed per the table",
            before.inventoryCount("shard_resonance") - EnhancementTables.tableFor(31)!!.shardsPerAttempt[31]!!,
            after.inventoryCount("shard_resonance")
        )
        assertTrue(after.stats.maxEnhanceAchieved >= 31)
    }

    private fun assertEnhanceNoop(before: GameState, after: GameState) {
        assertEquals(before.inventory, after.inventory)
        assertEquals(before.itemInstance("blade")!!.enhancementLevel, after.itemInstance("blade")!!.enhancementLevel)
        assertEquals(before.character.resolve, after.character.resolve)
    }

    @Test
    fun `engine rejects transcendent attempts without the unlock token`() {
        val locked = settledState("screen:enhance")
            .addItem("shard_resonance", 50_000)
            .addItem("catalyst_storm", 50)
            .addEquipmentItem(sword(31))
        val after = act(locked, GameIntent.Enhance("blade", useProtection = false))
        assertEnhanceNoop(locked, after)
    }

    @Test
    fun `ascendant stays architecture only and cannot be entered`() {
        val asc = settledState("screen:enhance", "recipe:refine_catalyst")
            .addItem("shard_resonance", 50_000)
            .addItem("catalyst_storm", 50)
            .addEquipmentItem(sword(46))
        assertEquals(
            EnhancementBand.ASCENDANT,
            EnhancementTables.tableFor(46)!!.band
        )
        // Nothing in content grants "tier:ascendant", so the engine must refuse
        // the transition outright: ASCENDANT is architecture for later content.
        assertEnhanceNoop(asc, act(asc, GameIntent.Enhance("blade", useProtection = false)))
    }

    @Test
    fun `engine stays put at the global enhancement ceiling`() {
        val maxed = settledState("screen:enhance", "recipe:refine_catalyst")
            .addItem("shard_resonance", 50_000)
            .addItem("catalyst_storm", 50)
            .addEquipmentItem(sword(EnhancementTables.maxEnhancementLevel))
        assertEnhanceNoop(maxed, act(maxed, GameIntent.Enhance("maxed", useProtection = false)))
    }

    // ---- the shatter mechanical distinction ----

    @Test
    fun `unprotected failure shatters to the band floor and loses durability`() {
        var rng = RandomSource(99L)
        var failed = false
        var guard = 0
        while (!failed && guard < 400) {
            val o = EnhancementResolver.attempt(
                sword(33), resolve = 0, useProtection = false, hasProtection = false, rng = rng
            )
            if (!o.success) {
                failed = true
                assertEquals("shatter resets to floor", 31, o.newLevel)
                assertEquals(30, o.durabilityLoss)
                assertTrue(o.newResolve > 0)
                assertTrue(o.consumedMaterial)
            }
            guard++
        }
        assertTrue("a failure must occur eventually", failed)
    }

    @Test
    fun `protection blocks a shatter but not the durability loss`() {
        var rng = RandomSource(99L)
        var failed = false
        var guard = 0
        while (!failed && guard < 400) {
            val o = EnhancementResolver.attempt(
                sword(33), resolve = 0, useProtection = true, hasProtection = true, rng = rng
            )
            if (!o.success) {
                failed = true
                assertEquals("protection stops the downgrade", 33, o.newLevel)
                assertEquals("durability still drains on a protected failure", 30, o.durabilityLoss)
                assertTrue(o.consumedProtection)
                assertFalse(o.consumedFullNegation)
            }
            guard++
        }
        assertTrue("a protected failure must occur eventually", failed)
    }

    @Test
    fun `ward fully negates a shatter failure`() {
        var rng = RandomSource(99L)
        var failed = false
        var guard = 0
        while (!failed && guard < 400) {
            val o = EnhancementResolver.attempt(
                sword(33), resolve = 0, useProtection = false, hasProtection = false,
                rng = rng, useFullNegation = true, hasFullNegation = true,
            )
            if (!o.success) {
                failed = true
                assertEquals(33, o.newLevel)
                assertEquals("ward negates durability too", 0, o.durabilityLoss)
                assertTrue(o.consumedFullNegation)
            }
            guard++
        }
        assertTrue("a ward-protected failure must occur eventually", failed)
    }

    // ---- functionally more powerful: per-band stat growth ----

    @Test
    fun `transcendent enhancements grant more stats per level than advanced`() {
        val plus30 = GameState(saveId = "a").copy(
            character = Character("A", equipped = mapOf(EquipSlot.MAIN_WEAPON to sword(30, "x")))
        )
        val plus31 = GameState(saveId = "b").copy(
            character = Character("B", equipped = mapOf(EquipSlot.MAIN_WEAPON to sword(31, "y")))
        )
        val attack30 = CombatStatsMath.effectiveStats(plus30).attack
        val attack31 = CombatStatsMath.effectiveStats(plus31).attack
        // Default base attack 1 + level bonus 0 + sword base 8 scaled:
        //  +30 with 0.15/lv -> 8*5.5 = 44;  +31 with 0.20/lv -> 8*7.2 = 58.
        assertEquals(45, attack30)
        assertEquals(59, attack31)
        assertTrue("the transcendent level must beat the advanced one", attack31 > attack30)
    }

    // ---- progression gate: the light beyond the veil ----

    @Test
    fun `reaching plus 31 and holding a catalyst unlocks dawnreach`() {
        var s = engine.newGame("T", clock).unlock("recipe:refine_catalyst").addItem("catalyst_storm", 1)
        // Simulate the completed Stormreach capstone (its real reward is the catalyst recipe).
        s = s.acceptQuests(listOf("q_stormreach_herald"), clock).completeQuest("q_stormreach_herald", clock)

        s = engine.apply(s, GameIntent.Tick(clock), clock).state
        val active = s.quest("q_light_beyond")?.status
        assertTrue("gate quest should be active once the herald falls", active == QuestStatus.ACTIVE)
        assertFalse("dawnreach must stay locked until transcended", s.hasUnlock("region:dawnreach"))

        // The gate demands the enhancement, not just materials.
        s = s.copy(stats = s.stats.recordEnhance(31))
        s = engine.apply(s, GameIntent.Tick(clock), clock).state

        assertTrue(
            "gate quest completes on +31 + catalyst",
            s.quest("q_light_beyond")?.status == QuestStatus.COMPLETED
        )
        assertTrue("dawnreach unlock granted", s.hasUnlock("region:dawnreach"))
        assertTrue(
            "spire node discovered",
            s.node("node_dawnreach_spire")?.unlocked == true
        )
        assertTrue(
            "abyss node discovered",
            s.node("node_dawnreach_abyss")?.unlocked == true
        )

        // The post-transcendent boss is now fightable and pays in catalysts.
        val fighting = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "starfall_wyrm"))
        assertEquals("starfall_wyrm", fighting.character.currentActivity?.targetId)
        val loot = com.projecteternal.content.Monsters.get("starfall_wyrm").lootTable
        assertTrue("wyrm must pay in its own catalyst", loot.any { it.defId == "catalyst_storm" && it.guaranteed })
    }

    // ---- offline: the catalyst distills while the game is closed ----

    @Test
    fun `offline distillation produces catalyst and consumes exactly the inputs`() {
        val s = GameState(saveId = "t").unlock("recipe:refine_catalyst")
            .addItem("core_storm", 5)
            .addItem("crystal_advanced", 10)
            .copy(
                character = Character("C", skillLevels = mapOf("refining" to 5)),
                lastSavedEpochMs = clock,
            )
            .withActivity(ActivityState.processing("refine_catalyst", clock))

        val result = OfflineSimulator.simulate(s, clock + 3600_000L)
        assertEquals("5 crafts from 5 cores", 5, result.state.inventoryCount("catalyst_storm"))
        assertEquals("cores fully consumed", 0, result.state.inventoryCount("core_storm"))
        assertEquals("crystals fully consumed", 0, result.state.inventoryCount("crystal_advanced"))
        assertEquals(5, result.report.resourcesGained["catalyst_storm"] ?: 0)
    }

    @Test
    fun `offline distillation without the unlock produces nothing`() {
        val s = GameState(saveId = "t")
            .addItem("core_storm", 5)
            .addItem("crystal_advanced", 10)
            .copy(
                character = Character("C", skillLevels = mapOf("refining" to 5)),
                lastSavedEpochMs = clock,
            )
            .withActivity(ActivityState.processing("refine_catalyst", clock))

        val result = OfflineSimulator.simulate(s, clock + 3600_000L)
        assertEquals(0, result.state.inventoryCount("catalyst_storm"))
        assertEquals("cores untouched", 5, result.state.inventoryCount("core_storm"))
        assertNull("gated activity is abandoned", result.state.character.currentActivity)
    }
}