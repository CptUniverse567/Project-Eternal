package com.projecteternal.sim

import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.QuestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The §16 example quest chain, driven end to end through the real engine:
 * mine iron -> craft sword -> hunt wolves -> discover mine -> defeat guardian
 * -> unlock new region. This is the Phase-1 vertical slice acceptance test.
 */
class QuestChainTest {

    private val engine = GameEngine()
    private var clock = 1_700_000_000_000L

    private fun act(s: GameState, intent: GameIntent): GameState =
        engine.apply(s, intent, clock).state

    private fun advance(s: GameState, seconds: Long): GameState {
        clock += seconds * 1000
        return engine.apply(s, GameIntent.Tick(clock), clock).state
    }

    private fun state(): GameState = engine.newGame("Chain", clock)

    private fun ensureIngots(s: GameState, needed: Long): GameState {
        var g = s
        while (g.inventoryCount("iron_ingot") < needed) {
            if (g.inventoryCount("ore_iron") < 2) {
                g = act(g, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))
                g = advance(g, 600)
            } else if (g.inventoryCount("iron_fragment") < 2) {
                g = act(g, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_iron"))
                g = advance(g, 60)
            } else {
                g = act(g, GameIntent.StartActivity(ActivityType.PROCESSING, "smelt_ingot"))
                g = advance(g, 60)
            }
        }
        return g
    }

    @Test
    fun `full quest chain completes and unlocks emberreach`() {
        var s = state()
        assertNotNull(s.quest("q_intro"))
        assertEquals(QuestStatus.ACTIVE, s.quest("q_intro")!!.status)

        // --- step 1: mine iron ---
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))
        s = advance(s, 300) // 5 min
        assertTrue("q_intro should complete", s.quest("q_intro")?.status == QuestStatus.COMPLETED)
        assertTrue("q_first_ore auto-accepts", s.quest("q_first_ore")?.status == QuestStatus.ACTIVE)

        s = advance(s, 900) // 15 more minutes
        assertTrue("q_first_ore should complete", s.quest("q_first_ore")?.status == QuestStatus.COMPLETED)
        assertTrue("sword recipe unlock", s.hasUnlock("recipe:sword_bronze"))
        assertTrue("crafting screen unlock", s.hasUnlock("screen:crafting"))

        // --- step 2: wood + ingots, craft + equip sword ---
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_wolf_woods"))
        s = advance(s, 120)
        assertTrue("need wood", s.inventoryCount("wood") >= 1)

        s = ensureIngots(s, 2)

        s = act(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_sword_bronze"))
        s = advance(s, 30)
        val sword = s.equipmentItems.firstOrNull { it.defId == "sword_bronze" }
        assertNotNull("sword should be forged", sword)

        s = act(s, GameIntent.Equip(sword!!.uid))
        assertEquals("sword equipped", "sword_bronze", s.character.equipped[com.projecteternal.model.EquipSlot.MAIN_WEAPON]?.defId)
        assertTrue("q_first_sword should complete", s.quest("q_first_sword")?.status == QuestStatus.COMPLETED)
        assertTrue("hunt unlock", s.hasUnlock("quest:hunt"))
        assertTrue("enhance screen unlock", s.hasUnlock("screen:enhance"))

        // Enhance the fresh sword before hunting (repair costs make this natural to do early).
        s = enhanceSword(s)

        // --- step 3: hunt wolves ---
        s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "wolf"))
        s = advance(s, 600)
        assertTrue("8 wolf kills needed", (s.stats.killCounts["wolf"] ?: 0) >= 8)
        assertTrue("q_hunt_wolves should complete", s.quest("q_hunt_wolves")?.status == QuestStatus.COMPLETED)
        assertTrue("deep mine unlock", s.hasUnlock("quest:deep_mine"))

        // --- step 4: discover the deep mine (auto-discovers on token) ---
        assertTrue("deep quarry discovered", s.node("node_deep_quarry")!!.unlocked)
        assertTrue("q_deep_mine should complete", s.quest("q_deep_mine")?.status == QuestStatus.COMPLETED)
        assertTrue("guardian unlock", s.hasUnlock("quest:guardian"))
        assertTrue("workers screen unlock", s.hasUnlock("screen:workers"))
        assertTrue("guardian ruin discovered", s.node("node_guardian_ruin")!!.unlocked)

        // --- step 5: defeat the guardian (loop with healing between retreats) ---
        var guard = 0
        while ((s.stats.killCounts["guardian_golem"] ?: 0) < 1 && guard < 60) {
            s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "guardian_golem"))
            s = advance(s, 180)
            guard++
        }
        assertTrue("guardian must fall", (s.stats.killCounts["guardian_golem"] ?: 0) >= 1)
        assertTrue("q_guardian should complete", s.quest("q_guardian")?.status == QuestStatus.COMPLETED)
        assertTrue("region emberreach unlocked", s.hasUnlock("region:emberreach"))
        assertTrue("emberreach mine discoverable", s.node("node_emberreach_mine")!!.unlocked)
    }

    private fun enhanceSword(s: GameState): GameState {
        var g = s
        val sword = g.equipmentItems.firstOrNull { it.defId == "sword_bronze" }!!
        // stock up on shards so the test is not rng-fragile
        g = g.addItem("shard_resonance", 200)
        val startLevel = sword.enhancementLevel
        var attempts = 0
        while (g.itemInstance(sword.uid)!!.enhancementLevel < startLevel + 3 && attempts < 60) {
            g = act(g, GameIntent.Enhance(sword.uid, useProtection = true))
            attempts++
        }
        assertTrue("sword should reach +3", g.itemInstance(sword.uid)!!.enhancementLevel >= startLevel + 3)
        return g
    }

    @Test
    fun `retainer produces over real foreground time once workers are unlocked`() {
        var s = state()
        s = s.unlock("screen:workers")
        s = act(s, GameIntent.AssignRetainer("retainer_aldo", "node_quarry"))
        val oreBefore = s.inventoryCount("ore_iron")
        s = advance(s, 1800) // 30 min foreground
        assertTrue("retainer output while foregrounded", s.inventoryCount("ore_iron") > oreBefore)
        assertEquals("node_quarry", s.retainer("retainer_aldo")!!.assignedNodeId)
    }

    @Test
    fun `equip objective stays achieved after the tool breaks overnight`() {
        var s = state()
        // Record the "equip a tool" objective while the starter pickaxe is equipped.
        s = advance(s, 1)
        assertTrue("equip objective recorded", (s.quest("q_intro")!!.objectiveProgress["o1"] ?: 0) >= 1)

        // Start gathering, then take a long absence long enough for the tool to break.
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))
        val long = 8 * 3600L
        val result = OfflineSimulator.simulate(s, clock + long * 1000)
        val broke = result.state.equipmentItems.firstOrNull { it.defId == "tool_pickaxe" }?.durability == 0 ||
            result.state.character.equipped[com.projecteternal.model.EquipSlot.TOOL] == null
        assertTrue("starter pickaxe should break over 8h", broke)
        assertTrue("q_intro completes despite broken tool", result.state.quest("q_intro")?.status == QuestStatus.COMPLETED)
        assertTrue("chain continues to q_first_ore", result.state.quest("q_first_ore")?.status == QuestStatus.COMPLETED)
        assertTrue("crafting unlocks offline", result.state.hasUnlock("screen:crafting"))
    }

    @Test
    fun `equip objective not pinned when never equipped`() {
        var s = state()
        // Take the tool off before any objective record is made.
        s = act(s, GameIntent.Unequip(com.projecteternal.model.EquipSlot.TOOL))
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))
        s = advance(s, 300)
        assertTrue("q_intro still waits for a tool", s.quest("q_intro")?.status == QuestStatus.ACTIVE)
    }
}
