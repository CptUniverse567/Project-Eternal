package com.projecteternal.sim

import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Release-candidate regression suite for the Industry Start path. Encodes the
 * exact contract a valid Processing/Crafting start must satisfy, per the manual
 * playtest checkpoint: a valid start is ACCEPTED, creates the active activity,
 * advances over simulated time, consumes inputs, produces outputs, grants XP,
 * and can be stopped; invalid requests are REJECTED (not silently flashed out
 * by the first tick).
 */
class IndustryStartRegressionTest {

    private val engine = GameEngine()
    private var clock = 1_700_000_000_000L

    private fun act(s: GameState, intent: GameIntent): GameState =
        engine.apply(s, intent, clock).state

    private fun advanceOneSecond(s: GameState): GameState {
        clock += 1000
        return engine.apply(s, GameIntent.Tick(clock), clock).state
    }

    // ---- Processing ----

    @Test
    fun `valid processing start is accepted and creates a processing activity`() {
        val s = act(engine.newGame("Tester", clock).addItem("ore_iron", 6),
            GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        assertEquals(ActivityType.PROCESSING, s.character.currentActivity?.type)
        assertEquals("grind_iron", s.character.currentActivity?.targetId)
    }

    @Test
    fun `processing job advances consumes input produces output and grants xp`() {
        var s = act(engine.newGame("Tester", clock).addItem("ore_iron", 6),
            GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        val oreBefore = s.inventoryCount("ore_iron")
        val grindingXpBefore = s.character.skillXp["grinding"] ?: 0

        repeat(90) { s = advanceOneSecond(s) }

        val consumed = oreBefore - s.inventoryCount("ore_iron")
        assertTrue("input consumed", consumed > 0)
        assertEquals("ore converts 1:2 to fragments", consumed * 2, s.inventoryCount("iron_fragment"))
        val grindingXpAfter = s.character.skillXp["grinding"] ?: 0
        assertTrue("grinding skill xp granted", grindingXpAfter > grindingXpBefore)
        assertTrue("character xp granted", s.character.xp > 0)
        assertTrue("craft stats tracked", (s.stats.craftCounts["iron_fragment"] ?: 0) >= consumed * 2)
    }

    @Test
    fun `processing job can be stopped and restarted`() {
        var s = act(engine.newGame("Tester", clock).addItem("ore_iron", 6),
            GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        s = advanceOneSecond(s)
        s = act(s, GameIntent.StopActivity)
        assertNull("stop clears the activity", s.character.currentActivity)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        assertEquals("restart resumes the recipe", "grind_iron", s.character.currentActivity?.targetId)
    }

    @Test
    fun `processing job auto-ends when inputs are exhausted`() {
        var s = act(engine.newGame("Tester", clock).addItem("ore_iron", 3),
            GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        repeat(90) { s = advanceOneSecond(s) }
        assertEquals(0L, s.inventoryCount("ore_iron"))
        assertEquals(6L, s.inventoryCount("iron_fragment"))
        assertNull("job ends rather than flashing or hanging", s.character.currentActivity)
    }

    @Test
    fun `invalid processing start with no materials is rejected`() {
        val s = act(engine.newGame("Tester", clock),
            GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        assertNull("unfundable start must not create an activity", s.character.currentActivity)
    }

    @Test
    fun `invalid processing start below the skill gate is rejected`() {
        val s = act(engine.newGame("Tester", clock).addItem("iron_ingot", 4).addItem("ore_coal", 2),
            GameIntent.StartActivity(ActivityType.PROCESSING, "refine_steel"))
        assertNull("skill-gated start must be rejected", s.character.currentActivity)
    }

    @Test
    fun `invalid processing start without the unlock token is rejected`() {
        val s = act(engine.newGame("Tester", clock).addItem("core_storm", 1).addItem("crystal_advanced", 2),
            GameIntent.StartActivity(ActivityType.PROCESSING, "refine_catalyst"))
        assertNull("token-gated start must be rejected", s.character.currentActivity)
    }

    // ---- Crafting (independent of the processing path) ----

    @Test
    fun `valid crafting start is accepted creates a crafting activity and produces equipment`() {
        var s = act(engine.newGame("Tester", clock).unlock("recipe:sword_bronze").addItem("iron_ingot", 4).addItem("wood", 2),
            GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
        assertEquals(ActivityType.CRAFTING, s.character.currentActivity?.type)
        val blacksmithingXpBefore = s.character.skillXp["blacksmithing"] ?: 0
        repeat(30) { s = advanceOneSecond(s) }

        val sword = s.equipmentItems.firstOrNull { it.defId == "sword_bronze" }
        assertNotNull("crafting produces a real equipment instance", sword)
        assertEquals("instance carries its durability", 80, sword!!.maxDurability)
        assertTrue("blacksmithing skill xp granted", (s.character.skillXp["blacksmithing"] ?: 0) > blacksmithingXpBefore)
        assertTrue("craft stats track the weapon", (s.stats.craftCounts["sword_bronze"] ?: 0) >= 1)
        assertEquals("inputs exhausted so the job ended cleanly", 2, s.equipmentItems.count { it.defId == "sword_bronze" })
        assertNull("job auto-ends when inputs run out", s.character.currentActivity)
    }

    @Test
    fun `crafting job can be stopped mid-job`() {
        var s = act(engine.newGame("Tester", clock).unlock("recipe:sword_bronze").addItem("iron_ingot", 4).addItem("wood", 2),
            GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
        s = advanceOneSecond(s)
        val activity = s.character.currentActivity
        assertNotNull(activity)
        s = act(s, GameIntent.StopActivity)
        assertNull(s.character.currentActivity)
    }

    @Test
    fun `invalid crafting start below the skill gate is rejected`() {
        val s = act(engine.newGame("Tester", clock).addItem("iron_ingot", 4).addItem("wood", 4),
            GameIntent.StartActivity(ActivityType.CRAFTING, "craft_pickaxe"))
        assertNull("skill-gated crafting start must be rejected", s.character.currentActivity)
    }

    @Test
    fun `invalid crafting start without the unlock token is rejected`() {
        val s = act(engine.newGame("Tester", clock).addItem("iron_ingot", 4).addItem("wood", 2),
            GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
        assertNull("token-gated crafting start must be rejected", s.character.currentActivity)
    }
}