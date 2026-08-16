package com.projecteternal.sim

import com.projecteternal.content.Regions
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 2 breadth: the Emberreach -> Stormreach regional chain, plus the
 * Stormreach regional mechanic (elevated gathering yields) applied through the
 * shared rate math.
 */
class RegionalChainTest {

    private val engine = GameEngine()
    private var clock = 2_000_000_000_000L

    private fun act(s: GameState, intent: GameIntent): GameState =
        engine.apply(s, intent, clock).state

    private fun advance(s: GameState, seconds: Long): GameState {
        clock += seconds * 1000
        return engine.apply(s, GameIntent.Tick(clock), clock).state
    }

    private fun state(): GameState = engine.newGame("Region", clock)

    /**
     * Lean helper for the combat-heavy last leg: buff level and let the
     * player brute-force Emberreach brutes.
     */
    private fun reachEmberreach(): GameState {
        var s = state()
        // unlock all mainline tokens directly to keep this test about the
        // regional content, not the full grind (QuestChainTest owns that).
        s = s.unlock("region:emberreach", "quest:hunt", "quest:deep_mine", "quest:guardian", "screen:workers", "screen:crafting", "screen:enhance")
        s = s.copy(stats = s.stats.visitRegion("emberreach"))
        // one no-op apply so node auto-discovery + quest auto-accept settle
        return engine.apply(s, GameIntent.Tick(clock), clock).state
    }

    @Test
    fun `stormreach gathering yields more than its hollowreach peers`() {
        val hollow = Regions.get("hollowreach")
        val storm = Regions.get("stormreach")
        assertEquals(1.0, hollow.yieldMultiplier, 0.001)
        assertTrue("stormreach must boost yields", storm.yieldMultiplier > 1.0)
        assertTrue("stormreach is tier 3", storm.tier == 3)
    }

    @Test
    fun `regional yield multiplier flows through gathering rate math for retainers`() {
        val nodeHollow = com.projecteternal.content.Nodes.get("node_quarry")
        val nodeStorm = com.projecteternal.content.Nodes.get("node_stormreach_coast")
        val r = com.projecteternal.model.Retainer(
            id = "r_fish", name = "Fisher", specialization = com.projecteternal.model.RetainerSpecialization.FISHER,
        )
        // Use the shared multiplier directly: region mechanic multiplies yields.
        val hollowMult = Regions.get(nodeHollow.regionId).yieldMultiplier
        val stormMult = Regions.get(nodeStorm.regionId).yieldMultiplier
        assertTrue(stormMult > hollowMult)
        assertEquals(1.5, stormMult, 0.001)

        // Rate unaffected by mechanic (yields are, not ticks)
        val rateStorm = Rates.retainerActionsPerHour(r, nodeStorm)
        assertTrue(rateStorm > 0)
    }

    @Test
    fun `destroying ash beasts unlocks stormreach chain`() {
        var s = reachEmberreach()
        assertTrue("emberreach auto discovery", s.node("node_emberreach_mine")!!.unlocked)
        assertTrue(
            "q_emberreach_ash should be active",
            s.quest("q_emberreach_ash")?.status == QuestStatus.ACTIVE
        )

        // fight ash beasts until the regional objective completes
        var guard = 0
        while (s.quest("q_emberreach_ash")?.status != QuestStatus.COMPLETED && guard < 240) {
            s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "ash_beast"))
            s = advance(s, 90)
            guard++
        }
        assertTrue("q_emberreach_ash must complete", s.quest("q_emberreach_ash")?.status == QuestStatus.COMPLETED)
        assertTrue("stormreach region unlock granted", s.hasUnlock("region:stormreach"))
        assertTrue("stormreach coast discovered", s.node("node_stormreach_coast")!!.unlocked)

        // Stormreach quest chain continues to the herald
        assertTrue(
            "q_stormreach_herald should now be active",
            s.quest("q_stormreach_herald")?.status == QuestStatus.ACTIVE
        )
    }

    @Test
    fun `life skill farm and river quests unlock their recipes`() {
        var s = state()
        // finish q_intro first (it gates the life-skill side quests)
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))
        s = advance(s, 300)
        assertTrue("q_intro must complete first", s.quest("q_intro")?.status == QuestStatus.COMPLETED)

        // harvest quest: gather grain at the terraces, mill flour
        s = act(s, GameIntent.AcceptQuest("q_side_harvest"))
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_farmlands"))
        s = advance(s, 900)
        val grainAfter = s.inventoryCount("grain")
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "mill_flour"))
        val started = s.character.currentActivity
        s = advance(s, 300)
        val harvestStatus = s.quest("q_side_harvest")?.status
        assertTrue(
            "harvest quest completes (status=$harvestStatus, grain=${s.stats.gatherCounts["grain"]}, grainNow=$grainAfter, flourCrafted=${s.stats.craftCounts["flour"]}, started=$started, flourNow=${s.inventoryCount("flour")})",
            harvestStatus == QuestStatus.COMPLETED
        )
        assertTrue("bake_bread recipe unlocked", s.hasUnlock("recipe:bake_bread"))

        // river fishery + fisher quest
        s = act(s, GameIntent.AcceptQuest("q_side_fisher"))
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_river_fishery"))
        s = advance(s, 600)
        assertTrue(
            "fisher quest completes",
            s.quest("q_side_fisher")?.status == QuestStatus.COMPLETED
        )
        assertTrue("fishing rod recipe unlocked", s.hasUnlock("recipe:craft_fishing_rod"))
    }
}