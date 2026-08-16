package com.projecteternal.sim

import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessingTest {

    private val engine = GameEngine()
    private val t0 = 1_700_000_000_000L

    @Test
    fun `grinding converts ore to fragments without loss`() {
        var s = engine.newGame("Tester", t0).addItem("ore_iron", 10)
        val pre = s.inventoryCount("ore_iron")
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"), t0).state
        s = engine.apply(s, GameIntent.Tick(t0 + 120_000), t0 + 120_000).state
        val consumed = pre - s.inventoryCount("ore_iron")
        assertEquals(consumed * 2, s.inventoryCount("iron_fragment"))
    }

    @Test
    fun `recipe stops when inputs run out`() {
        var s = engine.newGame("Tester", t0).addItem("ore_iron", 3)
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"), t0).state
        s = engine.apply(s, GameIntent.Tick(t0 + 10 * 60_000), t0 + 10 * 60_000).state
        assertEquals(0L, s.inventoryCount("ore_iron"))
        assertEquals(6L, s.inventoryCount("iron_fragment"))
        assertEquals(null, s.character.currentActivity)
    }

    @Test
    fun `smelt chain produces exactly the expected ingots`() {
        var s = engine.newGame("Tester", t0).addItem("iron_fragment", 6)
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.PROCESSING, "smelt_ingot"), t0).state
        s = engine.apply(s, GameIntent.Tick(t0 + 120_000), t0 + 120_000).state
        assertEquals(3L, s.inventoryCount("iron_ingot"))
        assertEquals(0L, s.inventoryCount("iron_fragment"))
    }

    @Test
    fun `recipe requires unlock token and skill level`() {
        // sword requires recipe:sword_bronze (not unlocked at start)
        val fresh = engine.newGame("Tester", t0)
        val blocked = engine.apply(
            fresh, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"), t0
        ).state
        assertEquals(null, blocked.character.currentActivity)

        // recipe with skill level gate is rejected while underleveled
        val steel = engine.apply(
            fresh, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_steel"), t0
        ).state
        assertEquals(null, steel.character.currentActivity)
    }

    @Test
    fun `crafting produces an equipment instance with durability`() {
        var s = engine.newGame("Tester", t0)
            .addItem("iron_ingot", 4)
            .addItem("wood", 2)
        s = s.unlock("recipe:sword_bronze")
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"), t0).state
        s = engine.apply(s, GameIntent.Tick(t0 + 30_000), t0 + 30_000).state
        val sword = s.equipmentItems.firstOrNull { it.defId == "sword_bronze" }
        assertTrue("sword must exist as an instance", sword != null)
        assertEquals(80, sword!!.maxDurability)
        // stats track crafts
        assertTrue((s.stats.craftCounts["sword_bronze"] ?: 0) >= 1)
    }

    @Test
    fun `processing grants skill xp and levels the skill`() {
        var s = engine.newGame("Tester", t0).addItem("ore_iron", 50)
        val levelBefore = s.character.skillLevel("grinding")
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"), t0).state
        s = engine.apply(s, GameIntent.Tick(t0 + 120_000), t0 + 120_000).state
        val levelAfter = s.character.skillLevel("grinding")
        assertTrue("grinding should level up", levelAfter > levelBefore)
        assertTrue((s.character.skillXp["grinding"] ?: 0) > 0)
    }

    @Test
    fun `foreground per-second ticks accumulate a job and produce`() {
        // Regression: the app ticks once per second. A fractional carry must NOT
        // clear the job on its first tick; output should land once ~3s elapses.
        var s = engine.newGame("Tester", t0).addItem("ore_iron", 3)
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"), t0).state
        assertTrue("job starts", s.character.currentActivity != null)
        repeat(10) { i ->
            s = engine.apply(s, GameIntent.Tick(t0 + (i + 1) * 1000L), t0 + (i + 1) * 1000L).state
        }
        assertEquals("all ore ground across 1s ticks", 0L, s.inventoryCount("ore_iron"))
        assertEquals("fragments produced", 6L, s.inventoryCount("iron_fragment"))
        assertEquals("job ends once inputs run out", null, s.character.currentActivity)
    }

    @Test
    fun `job survives an early fractional tick`() {
        var s = engine.newGame("Tester", t0).addItem("ore_iron", 20)
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"), t0).state
        // One 1-second tick: the recipe needs ~3s, so nothing has produced yet —
        // but the job must keep running, not self-cancel.
        s = engine.apply(s, GameIntent.Tick(t0 + 1_000), t0 + 1_000).state
        assertEquals("activity still active after a partial tick", ActivityType.PROCESSING, s.character.currentActivity?.type)
        assertEquals(0L, s.inventoryCount("iron_fragment"))
    }
}
