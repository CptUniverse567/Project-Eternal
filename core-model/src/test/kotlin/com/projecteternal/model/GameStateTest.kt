package com.projecteternal.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    @Test
    fun `add and remove items keeps stacks consistent`() {
        var s = GameState(saveId = "x")
        s = s.addItem("ore_iron", 3)
        s = s.addItem("ore_iron", 5)
        assertEquals(8L, s.inventoryCount("ore_iron"))
        s = s.removeItem("ore_iron", 4)
        assertEquals(4L, s.inventoryCount("ore_iron"))
        s = s.removeItem("ore_iron", 10)
        assertEquals(0L, s.inventoryCount("ore_iron"))
        assertEquals(0, s.inventory.size)
    }

    @Test
    fun `marks and resolve are tracked on character`() {
        var s = GameState(saveId = "x").addMarks(50).spendMarks(20).addResolve(7)
        assertEquals(30L, s.character.marks)
        assertEquals(7L, s.character.resolve)
    }

    @Test
    fun `nextSeed is deterministic and never repeats for consecutive advances`() {
        var s = GameState(saveId = "x", rngSeed = 42)
        val seeds = (0 until 5).map {
            val (next, seed) = s.nextSeed()
            s = next
            seed
        }
        assertEquals(5, seeds.toSet().size)

        var s2 = GameState(saveId = "x", rngSeed = 42)
        val seeds2 = (0 until 5).map {
            val (next, seed) = s2.nextSeed()
            s2 = next
            seed
        }
        assertEquals(seeds, seeds2)
    }

    @Test
    fun `unlock tokens are additive and queryable`() {
        val s = GameState(saveId = "x").unlock("a", "b")
        assertTrue(s.hasUnlock("a"))
        assertTrue(s.hasUnlock("b"))
        assertFalse(s.hasUnlock("c"))
    }

    @Test
    fun `discoverNode unlocks node and records region visit`() {
        val node = WorldNode(id = "n1", regionId = "r1", type = NodeType.MINE)
        val s = GameState(saveId = "x", nodes = listOf(node)).discoverNode("n1")
        assertTrue(s.node("n1")!!.unlocked)
        assertTrue(s.stats.visitedRegions.contains("r1"))
    }

    @Test
    fun `quest accept and complete transitions`() {
        val s = GameState(saveId = "x").acceptQuests(listOf("q1"), 1000)
        assertEquals(QuestStatus.ACTIVE, s.quest("q1")!!.status)
        val done = s.completeQuest("q1", 2000)
        assertEquals(QuestStatus.COMPLETED, done.quest("q1")!!.status)
        assertEquals(1, done.stats.completedQuestCount)
    }
}
