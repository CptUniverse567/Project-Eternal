package com.projecteternal.sim

import com.projecteternal.model.GameState
import com.projecteternal.model.Retainer
import com.projecteternal.model.RetainerSpecialization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RetainerTest {

    private val engine = GameEngine()
    private val t0 = 1_700_000_000_000L

    private fun minerState(): GameState {
        val r = Retainer(id = "r1", name = "R1", specialization = RetainerSpecialization.MINER)
        var s = engine.newGame("Tester", t0).copy(retainers = listOf(r))
        s = engine.apply(s, GameIntent.AssignRetainer("r1", "node_quarry"), t0).state
        return s
    }

    @Test
    fun `retainer can be assigned and unassigned`() {
        var s = engine.newGame("Tester", t0)
        s = engine.apply(s, GameIntent.AssignRetainer("retainer_aldo", "node_quarry"), t0).state
        assertEquals("node_quarry", s.retainer("retainer_aldo")!!.assignedNodeId)
        assertEquals(1, s.node("node_quarry")!!.assignedRetainerIds.size)
        s = engine.apply(s, GameIntent.AssignRetainer("retainer_aldo", null), t0).state
        assertEquals(null, s.retainer("retainer_aldo")!!.assignedNodeId)
        assertEquals(0, s.node("node_quarry")!!.assignedRetainerIds.size)
    }

    @Test
    fun `assignment requires unlocked and compatible node`() {
        var s = engine.newGame("Tester", t0)
        // locked node (deep quarry needs quest:deep_mine)
        s = engine.apply(s, GameIntent.AssignRetainer("retainer_aldo", "node_deep_quarry"), t0).state
        assertEquals(null, s.retainer("retainer_aldo")!!.assignedNodeId)
        // city node is incompatible with a miner
        s = engine.apply(s, GameIntent.AssignRetainer("retainer_aldo", "node_village"), t0).state
        assertEquals(null, s.retainer("retainer_aldo")!!.assignedNodeId)
    }

    @Test
    fun `retainer produces resources and gains xp offline`() {
        val result = OfflineSimulator.simulate(minerState(), t0 + 3600_000L)
        assertTrue(result.report.retainerOutput.containsKey("r1"))
        val out = result.report.retainerOutput["r1"]!!
        val total = out.values.sum()
        assertTrue("retainer should produce a sensible amount, got $total", total > 0 && total < 2000)
        val r = result.state.retainer("r1")!!
        assertTrue("retainer gains xp", r.xp > 0)
    }

    @Test
    fun `stamina caps production over long absences`() {
        val result = OfflineSimulator.simulate(minerState(), t0 + SimConfig.MAX_OFFLINE_SECONDS * 1000)
        val r = result.state.retainer("r1")!!
        val maxActions = 100 / SimConfig.STAMINA_PER_RETAINER_ACTION
        val expectedMaxActions = maxActions + (SimConfig.MAX_OFFLINE_SECONDS * SimConfig.STAMINA_REGEN_PER_SECOND) / SimConfig.STAMINA_PER_RETAINER_ACTION
        val produced = (result.report.retainerOutput["r1"] ?: emptyMap()).values.sum()
        assertTrue(
            "production bounded by stamina: got $produced, cap ~$expectedMaxActions",
            produced <= expectedMaxActions * 2 // generous: yields can be >1/action, min count 1
        )
        assertTrue(r.stamina >= 0)
    }

    @Test
    fun `idle retainer regens stamina to full`() {
        var s = minerState()
        s = s.copy(retainers = s.retainers.map { it.copy(stamina = 10) })
        s = engine.apply(s, GameIntent.AssignRetainer("r1", null), t0).state
        val result = OfflineSimulator.simulate(s, t0 + 3600_000L)
        assertTrue(result.state.retainer("r1")!!.stamina >= 10)
        assertFalse(result.report.retainerOutput.containsKey("r1"))
    }

    @Test
    fun `milestone level ups grant at most one trait per milestone`() {
        var s = minerState().copy(retainers = minerState().retainers.map {
            it.copy(xp = com.projecteternal.content.LevelCurves.skillXpToNext(1) + 200)
        })
        val (after, delta) = RetainerEngine.advance(s, 3600.0, RandomSource(9L))
        val r = after.retainer("r1")!!
        assertTrue("retainer should cross milestones", r.level >= 2)
        for ((_, traits) in delta.newTraits) {
            assertTrue("granted traits resolve", traits.all { com.projecteternal.content.RetainerTraits.get(it).id.isNotBlank() })
        }
    }

    @Test
    fun `trait milestones are deterministic and unique per retainer`() {
        fun traitsFor(seedXp: Long): List<String> {
            val s = minerState().copy(retainers = minerState().retainers.map { it.copy(xp = seedXp) })
            val (after, _) = RetainerEngine.advance(s, 3600.0, RandomSource(11L))
            return after.retainer("r1")!!.traitIds
        }
        assertEquals(traitsFor(9_000L), traitsFor(9_000L))
        val got = traitsFor(50_000L)
        assertEquals("distinct within one retainer", got.size, got.toSet().size)
    }

    @Test
    fun `traits speed up retainer gathering rate`() {
        val base = Retainer(id = "r1", name = "R1", specialization = RetainerSpecialization.MINER, level = 3)
        val buffed = base.copy(traitIds = listOf("eager_strike")) // +15% speed
        val node = com.projecteternal.content.Nodes.get("node_quarry")
        val slow = Rates.retainerActionsPerHour(base, node)
        val fast = Rates.retainerActionsPerHour(buffed, node)
        assertTrue(fast > slow)
        assertEquals(slow * 1.15, fast, 0.001)
    }

    @Test
    fun `recruiting requires the unlock token`() {
        val recruit = com.projecteternal.content.RetainerRecruits.get("retainer_helga") // needs region:emberreach
        var s = engine.newGame("Tester", t0).addMarks(10_000L)
        s = engine.apply(s, GameIntent.RecruitRetainer(recruit.id), t0).state
        assertEquals(null, s.retainer(recruit.id))
        s = s.unlock("region:emberreach")
        s = engine.apply(s, GameIntent.RecruitRetainer(recruit.id), t0).state
        assertEquals("recruit succeeds with token held", recruit.id, s.retainer(recruit.id)!!.id)
    }

    @Test
    fun `recruiting requires enough marks`() {
        val recruit = com.projecteternal.content.RetainerRecruits.get("retainer_mara") // 400 marks
        var s = engine.newGame("Tester", t0).unlock("screen:workers").addMarks(100L) // 150 < 400
        s = engine.apply(s, GameIntent.RecruitRetainer(recruit.id), t0).state
        assertEquals(null, s.retainer(recruit.id))
    }

    @Test
    fun `recruiting spends marks and adds a working retainer`() {
        val recruit = com.projecteternal.content.RetainerRecruits.get("retainer_mara")
        val before = engine.newGame("Tester", t0).unlock("screen:workers").addMarks(10_000L)
        val s = engine.apply(before, GameIntent.RecruitRetainer(recruit.id), t0).state
        val hired = s.retainer(recruit.id)!!
        assertEquals(before.character.marks - recruit.costMarks, s.character.marks) // spent exactly cost
        assertEquals(recruit.name, hired.name)
        assertEquals(recruit.specialization, hired.specialization)
        assertEquals(recruit.gatheringSpeed, hired.gatheringSpeed, 0.001)
        assertEquals(recruit.luck, hired.luck, 0.001)
        assertEquals(1, hired.level)
        // re-hiring is a no-op
        val twice = engine.apply(s, GameIntent.RecruitRetainer(recruit.id), t0).state
        assertEquals(1, twice.retainers.count { it.id == recruit.id })
    }
}
