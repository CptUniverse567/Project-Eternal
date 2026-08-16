package com.projecteternal.sim

import com.projecteternal.model.ActivityState
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.Retainer
import com.projecteternal.model.RetainerSpecialization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineSimulationTest {

    private val engine = GameEngine()
    private val t0 = 1_700_000_000_000L

    private fun stateMining(): GameState {
        var s = engine.newGame("Tester", t0)
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"), t0).state
        return s
    }

    private fun stateWithRetainer(): GameState {
        val retainer = Retainer(
            id = "r1", name = "R1", specialization = RetainerSpecialization.MINER,
            stamina = 100, maxStamina = 100,
        )
        var s = engine.newGame("Tester", t0)
        s = s.copy(retainers = s.retainers + retainer)
        s = engine.apply(s, GameIntent.AssignRetainer("r1", "node_quarry"), t0).state
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"), t0).state
        return s
    }

    @Test
    fun `short and long windows produce statistically sane yields`() {
        // short: 5 minutes of mining
        val short = OfflineSimulator.simulate(stateMining(), t0 + 300_000)
        // long: 2 hours
        val long = OfflineSimulator.simulate(stateMining(), t0 + 2 * 3600_000L)

        val shortOre = short.state.inventoryCount("ore_iron")
        val longOre = long.state.inventoryCount("ore_iron")
        assertTrue("short window should yield some ore, got $shortOre", shortOre in 3..40)
        assertTrue("long window should yield far more, got $longOre", longOre > shortOre * 5)
        assertTrue("long window sane upper bound, got $longOre", longOre < 1000)
    }

    @Test
    fun `offline simulation is deterministic for a fixed state`() {
        val s = stateWithRetainer()
        val a = OfflineSimulator.simulate(s, t0 + 3600_000L)
        val b = OfflineSimulator.simulate(s, t0 + 3600_000L)
        assertEquals(a.state, b.state)
        assertEquals(a.report, b.report)
    }

    @Test
    fun `elapsed is clamped to max window and flagged`() {
        val s = stateMining()
        val huge = t0 + 4L * 365 * 24 * 3600 * 1000 // 4 years
        val result = OfflineSimulator.simulate(s, huge)
        assertEquals(SimConfig.MAX_OFFLINE_SECONDS, result.report.elapsedSeconds)
        assertTrue(result.report.elapsedClamped)
        assertTrue("no unbounded rewards", result.state.inventoryCount("ore_iron") < 20_000)
    }

    @Test
    fun `future or inverted timestamps grant nothing`() {
        val s = stateMining()
        val result = OfflineSimulator.simulate(s, t0 - 60_000) // now BEFORE last save
        assertTrue(result.report.isEmpty())
        assertEquals(s, result.state)
    }

    @Test
    fun `clamped and max-window runs are identical`() {
        val s = stateMining()
        val maxRun = OfflineSimulator.simulate(s, t0 + SimConfig.MAX_OFFLINE_SECONDS * 1000)
        val overRun = OfflineSimulator.simulate(s, t0 + SimConfig.MAX_OFFLINE_SECONDS * 1000 * 50)
        // The clamp flag is the only allowed difference; rewards and timestamps must match.
        assertEquals(maxRun.state.inventoryCount("ore_iron"), overRun.state.inventoryCount("ore_iron"))
        assertEquals(maxRun.state.character.level, overRun.state.character.level)
        assertEquals(maxRun.state.lastSavedEpochMs, overRun.state.lastSavedEpochMs)
        assertEquals(maxRun.state.totalPlaySeconds, overRun.state.totalPlaySeconds)
        assertTrue(overRun.report.elapsedClamped)
        assertFalse(maxRun.report.elapsedClamped)
    }

    @Test
    fun `retainer production is aggregated into the report`() {
        val result = OfflineSimulator.simulate(stateWithRetainer(), t0 + 3600_000L)
        assertTrue("retainers should have produced", result.report.retainerOutput.isNotEmpty())
        val assigned = result.state.retainer("r1")!!
        assertTrue(assigned.assignedNodeId == "node_quarry")
        assertTrue("stamina drains", assigned.stamina < 100)
    }

    @Test
    fun `combat offline produces kills and loot`() {
        var s = engine.newGame("Tester", t0)
        s = s.addItem("iron_ingot", 20).addItem("wood", 10).unlock("recipe:sword_bronze")
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"), t0).state
        s = engine.apply(s, GameIntent.Tick(t0 + 30_000), t0 + 30_000).state
        val sword = s.equipmentItems.firstOrNull { it.defId == "sword_bronze" }!!
        s = engine.apply(s, GameIntent.Equip(sword.uid), t0).state
        s = engine.apply(s, GameIntent.StartActivity(ActivityType.COMBAT, "wolf"), t0).state
        val result = OfflineSimulator.simulate(s, t0 + 3600_000L)
        assertTrue("should get kills", (result.report.kills["wolf"] ?: 0) > 0)
        assertTrue("should get loot", result.report.resourcesGained.isNotEmpty())
    }

    @Test
    fun `idle absence still ticks playtime and regen`() {
        var s = engine.newGame("Tester", t0)
        val result = OfflineSimulator.simulate(s, t0 + 60_000)
        assertTrue(result.state.totalPlaySeconds >= 60)
        val maxHp = CombatStatsMath.effectiveStats(result.state).maxHp
        assertEquals(maxHp, result.state.character.health) // regen caps at max HP
    }

    @Test
    fun `report marks and events are bounded`() {
        val s = stateMining()
        val result = OfflineSimulator.simulate(s, t0 + 3600_000L)
        assertTrue(result.report.notableEvents.size <= SimConfig.MAX_NOTABLE_EVENTS)
        assertTrue(result.report.marksGained >= 0)
    }

    @Test
    fun `pending report is set only past the minimum threshold`() {
        val short = OfflineSimulator.simulate(stateMining(), t0 + 5_000)
        assertEquals(null, short.state.pendingOfflineReport)
        val long = OfflineSimulator.simulate(stateMining(), t0 + 120_000)
        assertTrue(long.state.pendingOfflineReport != null)
    }

    @Test
    fun `stepwise path and aggregate path agree on bounds`() {
        // 60s -> stepwise; 300s -> aggregate. Both should be sane and monotonic.
        val a = OfflineSimulator.simulate(stateMining(), t0 + 60_000)
        val b = OfflineSimulator.simulate(stateMining(), t0 + 300_000)
        assertTrue(b.state.inventoryCount("ore_iron") >= a.state.inventoryCount("ore_iron"))
    }
}
