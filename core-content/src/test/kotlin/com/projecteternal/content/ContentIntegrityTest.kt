package com.projecteternal.content

import com.projecteternal.model.EquipSlot
import com.projecteternal.model.ObjectiveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentIntegrityTest {

    private fun assertKnownItem(id: String, ctx: String) {
        assertTrue("$ctx references unknown item '$id'", Items.contains(id))
    }

    @Test
    fun `all item definitions resolve`() {
        val ids = Items.allItems().map { it.id }
        assertTrue("item catalog is empty", ids.isNotEmpty())
        assertEquals("duplicate item ids", ids.size, ids.toSet().size)
    }

    @Test
    fun `monster loot guaranteed tables sum to 100 percent`() {
        for (m in listOf(
            Monsters.get("wolf"), Monsters.get("boar"), Monsters.get("guardian_golem"),
            Monsters.get("ash_beast"), Monsters.get("storm_herald"), Monsters.get("starfall_wyrm"),
        )) {
            val guaranteed = m.lootTable.filter { it.guaranteed }
            assertEquals(
                "loot guaranteed table for ${m.id} must sum to 100",
                100, guaranteed.sumOf { it.chancePercent }
            )
            for (e in m.lootTable) assertKnownItem(e.defId, "loot ${m.id}")
        }
    }

    @Test
    fun `node yields reference known items and skills`() {
        for (n in Nodes.inRegion("hollowreach") + Nodes.inRegion("emberreach") +
            Nodes.inRegion("stormreach") + Nodes.inRegion("dawnreach")) {
            for (y in n.yields) assertKnownItem(y.defId, "node ${n.id}")
            for (s in n.skills) assertTrue("node ${n.id} unknown skill $s", Skills.get(s).id == s)
        }
    }

    @Test
    fun `recipe inputs and outputs reference known items and skills`() {
        for (r in Recipes.availableRecipes(emptySet()) + listOf(
            Recipes.get("refine_steel"), Recipes.get("craft_sword_steel"),
            Recipes.get("craft_fishing_rod"), Recipes.get("craft_shield_oak"),
            Recipes.get("shape_ward"), Recipes.get("refine_catalyst"),
        )) {
            for (i in r.inputs) assertKnownItem(i.defId, "recipe ${r.id} input")
            for (o in r.outputs) assertKnownItem(o.defId, "recipe ${r.id} output")
            assertTrue("recipe ${r.id} unknown skill", Skills.get(r.skillId).id == r.skillId)
        }
    }

    @Test
    fun `quest prerequisites and objectives are coherent`() {
        val allQuests = listOf(
            "q_intro", "q_first_ore", "q_first_sword", "q_hunt_wolves",
            "q_deep_mine", "q_guardian", "q_side_armor", "q_side_boar", "q_side_forger",
            "q_side_harvest", "q_side_fisher", "q_emberreach_ash", "q_stormreach_herald",
            "q_light_beyond",
        ).map { Quests.get(it) }

        for (q in allQuests) {
            for (o in q.objectives) {
                assertTrue("quest ${q.id}: objective needs id", o.id.isNotBlank())
                when (o.type) {
                    ObjectiveType.GATHER, ObjectiveType.CRAFT, ObjectiveType.HAVE_ITEMS ->
                        assertKnownItem(o.targetId, "quest ${q.id} obj ${o.id}")
                    ObjectiveType.KILL -> assertTrue(
                        "quest ${q.id} unknown monster ${o.targetId}",
                        listOf("wolf", "boar", "guardian_golem", "ash_beast", "storm_herald").contains(o.targetId)
                    )
                    ObjectiveType.DISCOVER_NODE -> assertTrue(
                        "quest ${q.id} unknown node", Nodes.get(o.targetId).id == o.targetId
                    )
                    ObjectiveType.REACH_SKILL_LEVEL -> assertTrue(
                        "quest ${q.id} unknown skill", Skills.get(o.targetId).id == o.targetId
                    )
                    ObjectiveType.EQUIP_SLOT -> assertTrue(
                        "quest ${q.id} unknown slot ${o.targetId}",
                        EquipSlot.entries.any { it.name == o.targetId }
                    )
                    else -> {}
                }
            }
            for (r in q.reward.items) assertKnownItem(r.defId, "quest ${q.id} reward")
            assertTrue(
                "quest ${q.id} may not grant the ascendant tier",
                "tier:ascendant" !in q.reward.unlocks
            )
        }
    }

    @Test
    fun `quest chain prerequisites are all satisfiable`() {
        val defined = setOf(
            "q_intro", "q_first_ore", "q_first_sword", "q_hunt_wolves",
            "q_deep_mine", "q_guardian", "q_side_armor", "q_side_boar", "q_side_forger",
            "q_side_harvest", "q_side_fisher", "q_emberreach_ash", "q_stormreach_herald",
            "q_light_beyond",
        )
        for (q in defined) {
            for (p in Quests.get(q).prerequisites) {
                assertTrue(
                    "quest $q references unexpected prereq '$p'",
                    defined.contains(p) ||
                        p.startsWith("quest:") || p.startsWith("recipe:") || p.startsWith("region:")
                )
            }
        }
    }

    @Test
    fun `enhancement tables cover 0 to max and never exceed max`() {
        val max = EnhancementTables.maxEnhancementLevel
        for (level in 0..max) {
            val table = EnhancementTables.tableFor(level) ?: continue
            assertTrue("band bounds broken at $level", level >= table.minLevel && level <= table.maxLevel)
            val chance = table.baseSuccessPercent[level]
            assertTrue("no success chance table for level $level", chance != null)
            assertTrue("success chance out of range at $level: $chance", chance in 1..100)
            val shards = table.shardsPerAttempt[level]
            assertTrue("no valid shard cost at $level", shards != null && shards > 0)
            if (table.fullNegationItem != null) {
                assertKnownItem(table.fullNegationItem!!, "enhancement $level full negation")
            }
        }
        assertEquals("BASE table must start at 0", 0, EnhancementTables.tableFor(0)!!.minLevel)
        assertEquals("max table must be reachable", max, EnhancementTables.tableFor(max - 1)!!.maxLevel)
    }

    @Test
    fun `all enhancement consumables resolve and recipes unlock`() {
        assertKnownItem("oil_preservation", "enhancement BASE protection")
        assertKnownItem("ward_of_stability", "enhancement BASE full negation")
        assertTrue(
            "ward recipe should be gated behind its unlock token",
            Recipes.get("shape_ward").unlockToken == "recipe:shape_ward"
        )
        for (trait in com.projecteternal.content.RetainerTraits.allTraits()) {
            assertTrue("trait ${trait.id} has name", trait.name.isNotBlank())
            assertTrue("trait ${trait.id} must affect something", trait.gatheringSpeedMultiplier != 1.0 || trait.luckMultiplier != 1.0)
        }
    }

    @Test
    fun `transcendent band is gated behind the catalyst and an unlock token`() {
        val table = EnhancementTables.tableFor(31)!!
        assertEquals(com.projecteternal.model.EnhancementBand.TRANSCENDENT, table.band)
        assertEquals("catalyst_storm", table.materialPerAttempt?.defId)
        assertEquals("recipe:refine_catalyst", table.unlockToken)
        assertEquals(com.projecteternal.content.FailureConsequence.SHATTER_TO_BAND_FLOOR, table.failure)
        assertTrue(table.statMultiplierPerLevel > 0.15)
        assertKnownItem("catalyst_storm", "transcendent material")
        assertTrue(
            "catalyst must come from a gated recipe",
            Recipes.get("refine_catalyst").unlockToken == "recipe:refine_catalyst"
        )

        // ASCENDANT stays architecture-only: a token no content grants.
        val ascendant = EnhancementTables.tableFor(46)!!
        assertEquals("tier:ascendant", ascendant.unlockToken)
        assertEquals(com.projecteternal.model.EnhancementBand.ASCENDANT, ascendant.band)
    }

    @Test
    fun `recruitable retainers resolve and are coherent`() {
        val all = com.projecteternal.content.RetainerRecruits.allRecruits()
        assertTrue("recruit catalog is empty", all.isNotEmpty())
        assertEquals("duplicate recruit ids", all.size, all.map { it.id }.toSet().size)
        for (recruit in all) {
            assertTrue("recruit ${recruit.id} has name", recruit.name.isNotBlank())
            assertTrue("recruit ${recruit.id} cost must be positive", recruit.costMarks > 0)
            assertTrue(
                "recruit ${recruit.id} unlock token must be a real token shape",
                recruit.unlockToken.startsWith("screen:") || recruit.unlockToken.startsWith("recipe:") || recruit.unlockToken.startsWith("region:") || recruit.unlockToken.startsWith("quest:")
            )
            assertTrue(
                "recruit ${recruit.id} affects something",
                recruit.gatheringSpeed != 1.0 || recruit.productionSpeed != 1.0 || recruit.luck != 1.0 || recruit.description.isNotBlank()
            )
        }
    }
}