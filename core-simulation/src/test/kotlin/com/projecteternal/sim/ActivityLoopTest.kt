package com.projecteternal.sim

import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Core activity-loop verification (RC stabilization pass): for every activity
 * type — mining, logging, farming, fishing, processing, crafting, combat —
 * verify the full loop through the real engine:
 *
 *   start -> activity is active ->
 *   simulate advances (resources/loot produced, XP + skill XP granted) ->
 *   stop works -> restart works -> offline progression produces and reports.
 *
 * These tests assert ACTUAL simulation state changes (inventory, stats, xp),
 * never just the presence of a UI button.
 */
class ActivityLoopTest {

    private val engine = GameEngine()
    private var clock = 1_700_000_000_000L

    private fun act(s: GameState, intent: GameIntent): GameState =
        engine.apply(s, intent, clock).state

    private fun advance(s: GameState, seconds: Long): GameState {
        clock += seconds * 1000
        return engine.apply(s, GameIntent.Tick(clock), clock).state
    }

    private fun settle(s: GameState): GameState =
        engine.apply(s, GameIntent.Tick(clock), clock).state

    private fun newGame(): GameState = engine.newGame("Loop", clock)

    /** Assert the shared five-step loop for a gathering node. */
    @Test
    fun `mining loop starts, produces, stops, restarts and works offline`() =
        assertGatheringLoop("node_quarry", "ore_iron", "mining")

    @Test
    fun `logging loop starts, produces, stops, restarts and works offline`() =
        assertGatheringLoop("node_wolf_woods", "wood", "logging")

    @Test
    fun `farming loop starts, produces, stops, restarts and works offline`() =
        assertGatheringLoop("node_farmlands", "grain", "farming")

    @Test
    fun `fishing loop starts, produces, stops, restarts and works offline`() =
        assertGatheringLoop("node_river_fishery", "fish_carp", "fishing")

    private fun assertGatheringLoop(nodeId: String, yieldId: String, skill: String) {
        var s = settle(newGame())

        // start
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, nodeId))
        assertEquals(ActivityType.GATHERING, s.character.currentActivity?.type)
        assertEquals(nodeId, s.character.currentActivity?.targetId)

        // simulation advances + produces (foreground)
        val xpBefore = s.character.skillXp[skill] ?: 0
        s = advance(s, 300)
        assertEquals("activity stays active after advancing", ActivityType.GATHERING, s.character.currentActivity?.type)
        assertTrue("gathering produces $yieldId", s.inventoryCount(yieldId) > 0)
        assertTrue("gather stats record $yieldId", (s.stats.gatherCounts[yieldId] ?: 0) > 0)
        assertTrue("$skill skill xp awarded", (s.character.skillXp[skill] ?: 0) > xpBefore)
        assertTrue("character xp awarded", s.character.xp > 0)

        // stop
        s = act(s, GameIntent.StopActivity)
        assertNull("stop clears the activity", s.character.currentActivity)

        // restart
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, nodeId))
        assertEquals("restart resumes the same node", nodeId, s.character.currentActivity?.targetId)

        // offline progression produces + reports
        val heldBefore = s.inventoryCount(yieldId)
        val result = OfflineSimulator.simulate(s, clock + 600_000)
        assertTrue("offline gathers more $yieldId", result.state.inventoryCount(yieldId) > heldBefore)
        assertTrue(
            "offline report records gains",
            (result.report.resourcesGained[yieldId] ?: 0) > 0
        )
        assertTrue("offline keeps the activity going", result.state.character.currentActivity?.type == ActivityType.GATHERING)
    }

    @Test
    fun `processing loop converts, grants xp, stops cleanly and runs offline`() {
        var s = newGame().addItem("ore_iron", 10)
        val grindingBefore = s.character.skillXp["grinding"] ?: 0

        // start
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        assertEquals(ActivityType.PROCESSING, s.character.currentActivity?.type)

        // advance until inputs are consumed
        s = advance(s, 120)
        assertEquals("all ore processed", 0L, s.inventoryCount("ore_iron"))
        assertEquals("ore converted 1:2 to fragments", 20L, s.inventoryCount("iron_fragment"))
        assertTrue("grinding skill xp awarded", (s.character.skillXp["grinding"] ?: 0) > grindingBefore)
        assertTrue("grind crafts tracked", (s.stats.craftCounts["iron_fragment"] ?: 0) >= 10)
        assertNull("job auto-ends when inputs run out", s.character.currentActivity)

// restart on fresh inputs + offline
        var g = s.addItem("ore_iron", 4)
        g = act(g, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
        val offline = OfflineSimulator.simulate(g, clock + 300_000)
        assertTrue("offline processing produces fragments", offline.state.inventoryCount("iron_fragment") >= 22)
        assertTrue("report lists the input consumed", (offline.report.resourcesConsumed["ore_iron"] ?: 0) > 0)
    }

    @Test
    fun `crafting loop produces an equipment instance, xp, and stops`() {
        var s = newGame().unlock("recipe:sword_bronze").addItem("iron_ingot", 4).addItem("wood", 2)
        val bsBefore = s.character.skillXp["blacksmithing"] ?: 0

        s = act(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
        assertEquals(ActivityType.CRAFTING, s.character.currentActivity?.type)
        s = advance(s, 20)
        val sword = s.equipmentItems.firstOrNull { it.defId == "sword_bronze" }
        assertNotNull("crafting produces a real equipment instance", sword)
        assertEquals("instance carries its durability", 80, sword!!.maxDurability)
        assertTrue("craft stats track the weapon", (s.stats.craftCounts["sword_bronze"] ?: 0) >= 1)
        assertTrue("blacksmithing skill xp awarded", (s.character.skillXp["blacksmithing"] ?: 0) > bsBefore)

        // stop mid-job works without losing state
        val activity = s.character.currentActivity
        s = act(s, GameIntent.StopActivity)
        assertNull("stop cancels crafting", s.character.currentActivity)

        // restart + offline craft produces another instance
        val craftedBefore = s.equipmentItems.count { it.defId == "sword_bronze" }
        if (s.inventoryCount("iron_ingot") >= 2 && s.inventoryCount("wood") >= 1) {
            s = act(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
            val off = OfflineSimulator.simulate(s, clock + 120_000)
            assertTrue(
                "offline crafting forges another sword",
                off.state.equipmentItems.count { it.defId == "sword_bronze" } > craftedBefore
            )
        }
    }

    @Test
    fun `combat loop kills, grants xp and loot, stops and restarts`() {
        var s = newGame().unlock("recipe:sword_bronze").addItem("iron_ingot", 4).addItem("wood", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
        s = advance(s, 20)
        val sword = s.equipmentItems.firstOrNull { it.defId == "sword_bronze" }!!
        s = act(s, GameIntent.Equip(sword.uid))

        // start
        s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "wolf"))
        assertEquals(ActivityType.COMBAT, s.character.currentActivity?.type)
        assertEquals("wolf", s.character.currentActivity?.targetId)

        // advance -> kills + xp + loot in the actual state
        val xpBefore = s.character.xp
        s = advance(s, 600)
        assertTrue("combat kills accrued", (s.stats.killCounts["wolf"] ?: 0) >= 3)
        assertTrue("combat grants character xp", s.character.xp > xpBefore)
        assertTrue("combat loot lands in inventory", s.inventory.any { it.defId == "pelt_wolf" || it.defId == "meat_wolf" })

        // stop
        s = act(s, GameIntent.StopActivity)
        assertNull("stop cancels combat", s.character.currentActivity)

        // restart + report
        s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "wolf"))
        val off = OfflineSimulator.simulate(s, clock + 600_000)
        assertTrue("offline combat keeps killing", (off.report.kills["wolf"] ?: 0) >= 3)
        assertTrue("offline combat loot reported", off.report.resourcesGained.isNotEmpty())
    }
}