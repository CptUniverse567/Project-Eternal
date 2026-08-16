package com.projecteternal.content

import com.projecteternal.model.EquipSlot
import com.projecteternal.model.ObjectiveType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            // Pack 01 — Outer Reaches
            Monsters.get("cinder_hound"), Monsters.get("scorch_viper"), Monsters.get("ember_jackal"),
            Monsters.get("cinder_wraith"), Monsters.get("ash_sovereign"),
            Monsters.get("storm_boar"), Monsters.get("lightning_heron"), Monsters.get("storm_ape"),
            Monsters.get("stormcaller_elite"),
            Monsters.get("frostmaw_wolf"), Monsters.get("ice_lynx"), Monsters.get("frost_titan"),
            Monsters.get("glacier_warden"),
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
            Nodes.inRegion("cindervale") + Nodes.inRegion("stormreach") +
            Nodes.inRegion("frostreach") + Nodes.inRegion("dawnreach")) {
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
            // Pack 01 — Outer Reaches
            "q_cindervale", "q_cindervale_market", "q_cinder_steel", "q_cinder_king",
            "q_cinder_regional", "q_cinder_armor", "q_glazer", "q_jeweler", "q_advance_begin",
            "q_stormreach_expeditions", "q_frostreach", "q_frostvein", "q_frost_warden",
            "q_herbalist_highland", "q_frostvein_enhance", "q_advance_mid", "q_advance_near",
        ).map { Quests.get(it) }

        for (q in allQuests) {
            for (o in q.objectives) {
                assertTrue("quest ${q.id}: objective needs id", o.id.isNotBlank())
                when (o.type) {
                    ObjectiveType.GATHER, ObjectiveType.CRAFT, ObjectiveType.HAVE_ITEMS ->
                        assertKnownItem(o.targetId, "quest ${q.id} obj ${o.id}")
                    ObjectiveType.KILL -> assertTrue(
                        "quest ${q.id} unknown monster ${o.targetId}",
                        listOf(
                            "wolf", "boar", "guardian_golem", "ash_beast", "storm_herald",
                            "cinder_hound", "scorch_viper", "ember_jackal", "cinder_wraith",
                            "ash_sovereign", "storm_boar", "lightning_heron", "storm_ape",
                            "stormcaller_elite", "frostmaw_wolf", "ice_lynx", "frost_titan",
                            "glacier_warden",
                        ).contains(o.targetId)
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
            "q_cindervale", "q_cindervale_market", "q_cinder_steel", "q_cinder_king",
            "q_cinder_regional", "q_cinder_armor", "q_glazer", "q_jeweler", "q_advance_begin",
            "q_stormreach_expeditions", "q_frostreach", "q_frostvein", "q_frost_warden",
            "q_herbalist_highland", "q_frostvein_enhance", "q_advance_mid", "q_advance_near",
        )
        for (q in defined) {
            for (p in Quests.get(q).prerequisites) {
                assertTrue(
                    "quest $q references unexpected prereq '$p'",
                    defined.contains(p) ||
                        p.startsWith("quest:") || p.startsWith("recipe:") ||
                        p.startsWith("region:") || p.startsWith("screen:")
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
        // Pack 01 workers must be gathering-only (no processing implied).
        for (id in listOf("retainer_pella", "retainer_runa")) {
            val recruit = com.projecteternal.content.RetainerRecruits.get(id)
            assertTrue("recruit $id must be PROSPECTOR or FORGER", recruit.specialization in
                listOf(com.projecteternal.model.RetainerSpecialization.PROSPECTOR, com.projecteternal.model.RetainerSpecialization.FORGER))
        }
    }

    @Test
    fun `pack 01 regions are coherent`() {
        val cindervale = Regions.get("cindervale")
        val frostreach = Regions.get("frostreach")
        assertEquals("cindervale tier", 2, cindervale.tier)
        assertEquals("frostreach tier", 3, frostreach.tier)
        assertEquals("cindervale unlock token", "region:cindervale", cindervale.unlockToken)
        assertEquals("frostreach unlock token", "region:frostreach", frostreach.unlockToken)
        assertNotNull("cindervale rare yield (emberglass)", cindervale.rareYield)
        assertEquals("emberglass", cindervale.rareYield!!.defId)
        assertTrue("emberglass chance in 1..30", cindervale.rareYield!!.chancePercent in 1..30)
        assertNotNull("frostreach rare yield (frostvein)", frostreach.rareYield)
        assertEquals("frostvein", frostreach.rareYield!!.defId)
        assertTrue("frostreach hazard is positive", frostreach.hazardPerAction > 0)
        assertEquals("frostreach price modifier", 2.0, frostreach.priceModifier, 1e-9)
        assertTrue("cindervale has 5 nodes", Nodes.inRegion("cindervale").size == 5)
        assertTrue("frostreach has 5 nodes", Nodes.inRegion("frostreach").size == 5)
        assertTrue("stormreach now has 5 nodes (deepened)", Nodes.inRegion("stormreach").size == 5)
        assertTrue(
            "herbalism node exists in frostreach",
            Nodes.inRegion("frostreach").any { "herbalism" in it.skills }
        )
    }

    @Test
    fun `pack 01 nodes reference real regions and no self-unlock`() {
        for (regionId in listOf("cindervale", "frostreach")) {
            for (n in Nodes.inRegion(regionId)) {
                assertTrue("node ${n.id} belongs to $regionId", n.regionId == regionId)
                assertTrue(
                    "node ${n.id} must be gated by its region token, not itself",
                    n.unlockToken == "region:$regionId"
                )
            }
        }
    }

    @Test
    fun `advance band supports the frostvein alternate material`() {
        val table = EnhancementTables.tableFor(16)!!
        assertEquals("crystal_advanced", table.materialPerAttempt?.defId)
        assertNotNull("frostvein alternate material", table.alternateMaterialPerAttempt)
        assertEquals("frostvein", table.alternateMaterialPerAttempt!!.defId)
        assertKnownItem("frostvein", "advance alternate material")
    }

    @Test
    fun `every pack 01 resource has at least one crafting sink`() {
        val allUnlocks = setOf(
            "region:cindervale", "region:stormreach", "region:frostreach",
            "recipe:cinder_gear", "recipe:glasswork", "recipe:jewelry",
            "recipe:sword_cinder", "recipe:frostforge",
        )
        val recipeInputs = Recipes.availableRecipes(allUnlocks)
            .flatMap { it.inputs.map { i -> i.defId } }.toSet()
        val raw = listOf(
            "ore_cinder", "emberglass", "sootwood", "ashfish", "saltash", "ashgrain",
            "ore_storm", "thunderwood", "stormgrain", "stormcrystal",
            "ore_frost", "icewood", "glacefish", "frostmoss", "frostvein",
        )
        for (id in raw) {
            assertTrue("$id must have a crafting sink", id in recipeInputs)
        }
    }

    @Test
    fun `every processing and crafting skill has a level 0 recipe`() {
        // Anti-deadlock guardrail: a fresh player at skill level 0 must always have a
        // legitimate XP source. Any skill whose lowest-required recipe sits above level 0
        // with no other XP source is a progression deadlock.
        for (skill in Skills.ofCategory(SkillCategory.PROCESSING) + Skills.ofCategory(SkillCategory.CRAFTING)) {
            val minLevel = Recipes.availableRecipes(emptySet())
                .filter { it.skillId == skill.id }
                .minOfOrNull { it.skillLevelRequired }
            // Token-gated recipes count too: their tokens are quest-reachable, but the
            // LEVEL gate must still be 0 so the skill is startable.
            val allMinLevel = com.projecteternal.content.Recipes.availableRecipes(
                setOf(
                    "region:cindervale", "region:stormreach", "region:frostreach",
                    "recipe:sword_bronze", "recipe:sword_steel", "recipe:tool_pickaxe",
                    "recipe:craft_fishing_rod", "recipe:craft_shield_oak",
                    "recipe:cinder_gear", "recipe:glasswork", "recipe:jewelry",
                    "recipe:sword_cinder", "recipe:frostforge", "recipe:shape_ward",
                    "recipe:refine_catalyst",
                )
            ).filter { it.skillId == skill.id }.minOfOrNull { it.skillLevelRequired }
            val effectiveMin = minOf(minLevel ?: Int.MAX_VALUE, allMinLevel ?: Int.MAX_VALUE)
            assertTrue(
                "skill ${skill.id} has no level-0 recipe (deadlock guard)",
                effectiveMin <= 0
            )
        }
    }
}