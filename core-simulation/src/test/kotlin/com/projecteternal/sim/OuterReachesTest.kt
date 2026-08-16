package com.projecteternal.sim

import com.projecteternal.content.LevelCurves
import com.projecteternal.content.Nodes
import com.projecteternal.model.ActivityType
import com.projecteternal.model.CombatStats
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemInstance
import com.projecteternal.model.QuestStatus
import com.projecteternal.model.RetainerSpecialization
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pack 01 — Outer Reaches regression suite. Covers the Cindervale/Frostreach
 * region gates (no circular dependencies), resource chains, combat/loot,
 * workers (FORGER/PROSPECTOR), the ADVANCED alternate material (frostvein),
 * enhancement checkpoints, the Frostreach hazard, regional rare yields, and
 * offline determinism.
 */
class OuterReachesTest {

    private val engine = GameEngine()
    private var clock = 1_700_000_000_000L

    private fun act(s: GameState, intent: GameIntent): GameState =
        engine.apply(s, intent, clock).state

    private fun advance(s: GameState, seconds: Long): GameState {
        clock += seconds * 1000
        return engine.apply(s, GameIntent.Tick(clock), clock).state
    }

    private fun withSkill(s: GameState, skill: String, level: Int): GameState {
        var xp = 0L
        for (l in 0 until level) xp += LevelCurves.skillXpToNext(l)
        return s.copy(character = s.character.copy(
            skillXp = s.character.skillXp + (skill to xp),
            skillLevels = s.character.skillLevels + (skill to level),
        ))
    }

    @Test
    fun `refining to steel end to end unlocks cindervale`() {
        // The previously-deadlocked path: refining-0 must be able to start Refine Iron,
        // produce refined iron, forge steel, and satisfy q_cindervale (HAVE_ITEMS steel).
        var s = engine.newGame("Tester", clock)
            .unlock("region:emberreach")
            .addItem("iron_ingot", 12).addItem("ore_coal", 3)
        s = act(s, GameIntent.Tick(clock))
        repeat(10) { s = s.copy(stats = s.stats.addKill("ash_beast")) }
        s = act(s, GameIntent.Tick(clock)) // q_emberreach_ash complete

        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "refine_refined"))
        assertEquals("refine accepted at refining 0", "refine_refined", s.character.currentActivity?.targetId)
        s = advance(s, 120)
        assertTrue("refined iron produced", s.inventoryCount("refined_iron") > 0)

        s = withSkill(s, "refining", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "refine_steel"))
        s = advance(s, 120)
        assertTrue("steel forged", s.inventoryCount("steel") > 0)
        assertTrue("q_cindervale resolved via steel", s.hasUnlock("region:cindervale"))
    }

    // ---- Region gates (no circular dependencies) ----

    @Test
    fun `refining is reachable from level 0 via Refine Iron`() {
        // Regression: refine_refined used to require Refining 1 with no level-0 XP source.
        var s = engine.newGame("Tester", clock).addItem("iron_ingot", 4)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "refine_refined"))
        assertEquals("accepted at refining 0", "refine_refined", s.character.currentActivity?.targetId)
        s = advance(s, 60)
        assertTrue("refining xp gained", (s.character.skillXp["refining"] ?: 0) > 0)
        assertTrue("refined iron produced", s.inventoryCount("refined_iron") > 0)
    }

    @Test
    fun `engineering is reachable from level 0 with steel`() {
        var s = engine.newGame("Tester", clock)
            .addItem("steel", 2).addItem("lumber", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_machinery_part"))
        assertEquals("machinery part accepted at engineering 0", "craft_machinery_part", s.character.currentActivity?.targetId)
        s = advance(s, 60)
        assertTrue("engineering xp gained", (s.character.skillXp["engineering"] ?: 0) > 0)
        assertTrue("machinery part produced", s.inventoryCount("machinery_part") > 0)
    }

    @Test
    fun `jewelry is reachable from level 0 once the recipe token is held`() {
        var s = engine.newGame("Tester", clock)
            .unlock("recipe:jewelry")
            .addItem("emberglass", 2).addItem("glass_shard", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.CRAFTING, "craft_ring_ember"))
        assertEquals("ember ring accepted at jewelry 0", "craft_ring_ember", s.character.currentActivity?.targetId)
        s = advance(s, 60)
        assertTrue("jewelry xp gained", (s.character.skillXp["jewelry"] ?: 0) > 0)
        assertTrue("ember ring crafted", s.equipmentItems.any { it.defId == "ring_ember" })
    }

    @Test
    fun `cindervale unlocks without any cindervale-internal node`() {
        var s = engine.newGame("Tester", clock)
            .unlock("region:emberreach")
            .addItem("steel", 1)
        s = act(s, GameIntent.Tick(clock)) // accept q_emberreach_ash
        repeat(10) { s = s.copy(stats = s.stats.addKill("ash_beast")) }
        s = act(s, GameIntent.Tick(clock)) // complete q_emberreach_ash -> q_cindervale -> region
        assertTrue("region:cindervale granted", s.hasUnlock("region:cindervale"))
        assertEquals("q_cindervale completed", QuestStatus.COMPLETED, s.quest("q_cindervale")?.status)
        assertTrue("ashveil market discovered after unlock", s.node("node_ashveil_market")?.unlocked == true)
        assertTrue("cinder quarry discovered after unlock", s.node("node_cinder_quarry")?.unlocked == true)
    }

    @Test
    fun `cindervale stays locked without the steel prerequisite`() {
        var s = engine.newGame("Tester", clock).unlock("region:emberreach")
        s = act(s, GameIntent.Tick(clock))
        repeat(10) { s = s.copy(stats = s.stats.addKill("ash_beast")) }
        s = act(s, GameIntent.Tick(clock))
        assertFalse("no region:cindervale without steel", s.hasUnlock("region:cindervale"))
        assertFalse("no cindervale node without unlock", s.node("node_cinder_quarry")?.unlocked == true)
    }

    @Test
    fun `frostreach unlocks without requiring frostvein`() {
        var s = engine.newGame("Tester", clock)
            .unlock("region:emberreach", "region:stormreach")
            .addItem("cinder_steel", 2)
        s = act(s, GameIntent.Tick(clock))
        repeat(10) { s = s.copy(stats = s.stats.addKill("ash_beast")) }
        s = act(s, GameIntent.Tick(clock)) // q_emberreach_ash done; q_stormreach_herald accepted
        s = s.copy(stats = s.stats.addKill("storm_herald"))
        s = act(s, GameIntent.Tick(clock)) // q_stormreach_herald done -> q_frostreach -> region
        assertTrue("region:frostreach granted", s.hasUnlock("region:frostreach"))
        assertEquals("q_frostreach completed", QuestStatus.COMPLETED, s.quest("q_frostreach")?.status)
        assertTrue("frost quarry discovered", s.node("node_frost_quarry")?.unlocked == true)
        assertEquals("frostvein never required to unlock", 0L, s.stats.gatherCounts["frostvein"] ?: 0)
    }

    // ---- Resource chains ----

    @Test
    fun `cinder steel chain converts ore to fragments to ingots to steel`() {
        var s = engine.newGame("Tester", clock).unlock("region:cindervale")
            .addItem("ore_cinder", 4)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_cinder"))
        s = advance(s, 120)
        assertEquals("grind consumes ore", 0L, s.inventoryCount("ore_cinder"))
        assertEquals("grind yields fragments", 8L, s.inventoryCount("cinder_fragment"))

        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "smelt_cinder"))
        s = advance(s, 120)
        assertEquals("smelt yields ingots", 4L, s.inventoryCount("cinder_ingot"))

        s = withSkill(s, "refining", 2).addItem("ore_coal", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "refine_cinder_steel"))
        s = advance(s, 120)
        assertEquals("refine yields cinder steel", 2L, s.inventoryCount("cinder_steel"))
    }

    @Test
    fun `glass chain vitrifies emberglass into shards and wards`() {
        var s = engine.newGame("Tester", clock).unlock("region:cindervale")
            .addItem("emberglass", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "vitrify_glass"))
        s = advance(s, 60)
        assertEquals("glass shards produced", 2L, s.inventoryCount("glass_shard"))
    }

    @Test
    fun `froststeel chain requires cinder steel from cindervale`() {
        var s = engine.newGame("Tester", clock).unlock("region:frostreach")
            .addItem("ore_frost", 4)
        s = withSkill(withSkill(s, "grinding", 3), "smelting", 3)
        s = act(s, GameIntent.Tick(clock)) // auto-discover frostreach nodes
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "grind_frost"))
        s = advance(s, 120)
        assertEquals(8L, s.inventoryCount("frost_fragment"))
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "smelt_frost"))
        s = advance(s, 120)
        assertEquals(4L, s.inventoryCount("frost_ingot"))
        s = withSkill(s, "refining", 4).addItem("cinder_steel", 2)
        s = act(s, GameIntent.StartActivity(ActivityType.PROCESSING, "refine_froststeel"))
        s = advance(s, 120)
        assertEquals("froststeel forged", 2L, s.inventoryCount("froststeel"))
    }

    // ---- Combat / loot ----

    @Test
    fun `ash sovereign drops cinder cores`() {
        var s = engine.newGame("Tester", clock).unlock("region:cindervale")
        s = act(s, GameIntent.Tick(clock)) // auto-discover cindervale nodes
        s = withCombatPower(s)
        s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "ash_sovereign"))
        s = advance(s, 600)
        assertTrue("boss looted", s.inventoryCount("cinder_core") > 0)
        assertTrue("cindervale rare drop plausible", s.stats.killCounts["ash_sovereign"]!! > 0)
    }

    @Test
    fun `glacier warden drops frostvein`() {
        var s = engine.newGame("Tester", clock).unlock("region:frostreach")
        s = act(s, GameIntent.Tick(clock)) // auto-discover frostreach nodes
        s = withCombatPower(s)
        s = act(s, GameIntent.StartActivity(ActivityType.COMBAT, "glacier_warden"))
        s = advance(s, 600)
        assertTrue("warden looted frostvein", s.inventoryCount("frostvein") > 0)
    }

    private fun withCombatPower(s: GameState): GameState {
        var st = com.projecteternal.sim.Progress.charXp(s, 200_000)
        st = st.addEquipmentItem(ItemInstance("pw", "sword_cinder", enhancementLevel = 25, durability = 999, maxDurability = 999))
        return st.copy(character = st.character.copy(
            equipped = st.character.equipped + (EquipSlot.MAIN_WEAPON to st.itemInstance("pw")!!),
            health = 5000,
            baseStats = st.character.baseStats.copy(maxHp = 5000),
        ))
    }

    // ---- Workers ----

    @Test
    fun `pella forger is gated by cindervale and boosted at mine nodes`() {
        var s = engine.newGame("Tester", clock)
        s = act(s, GameIntent.RecruitRetainer("retainer_pella"))
        assertNull("no recruit without region", s.retainer("retainer_pella"))

        s = s.unlock("region:cindervale").addMarks(2000)
        s = act(s, GameIntent.RecruitRetainer("retainer_pella"))
        assertNotNull("pella recruited", s.retainer("retainer_pella"))

        val pella = s.retainer("retainer_pella")!!
        val mineRate = Rates.retainerActionsPerHour(pella, Nodes.get("node_cinder_quarry"))
        val baseline = Rates.retainerActionsPerHour(pella.copy(specialization = RetainerSpecialization.MINER), Nodes.get("node_cinder_quarry"))
        assertTrue("forger gathers faster at mine nodes", mineRate > baseline)
    }

    @Test
    fun `runa prospector is gated by frostreach`() {
        var s = engine.newGame("Tester", clock).addMarks(3000)
        s = act(s, GameIntent.RecruitRetainer("retainer_runa"))
        assertNull("no recruit without frostreach", s.retainer("retainer_runa"))
        s = s.unlock("region:frostreach")
        s = act(s, GameIntent.RecruitRetainer("retainer_runa"))
        assertNotNull("runa recruited", s.retainer("retainer_runa"))
    }

    @Test
    fun `forger and prospector workers produce and gain regional rares offline`() {
        var s = engine.newGame("Tester", clock)
            .unlock("region:cindervale")
            .addMarks(5000)
        s = act(s, GameIntent.RecruitRetainer("retainer_pella"))
        s = act(s, GameIntent.AssignRetainer("retainer_pella", "node_cinder_quarry"))
        s = s.copy(lastSavedEpochMs = clock)
        val result = OfflineSimulator.simulate(s, clock + 4 * 3600 * 1000)
        val out = result.state
        assertTrue("pella produced cinder ore", out.inventoryCount("ore_cinder") > 0)
        assertTrue("pella found emberglass", out.inventoryCount("emberglass") > 0)
    }

    // ---- Enhancement: alternate ADVANCED material ----

    @Test
    fun `advance attempt consumes the chosen alternate material frostvein`() {
        var s = engine.newGame("Tester", clock)
            .addItem("shard_resonance", 100)
            .addItem("frostvein", 10)
            .addItem("crystal_advanced", 10)
            .addEquipmentItem(ItemInstance("b", "sword_cinder", enhancementLevel = 16, durability = 100, maxDurability = 100))
        val frostBefore = s.inventoryCount("frostvein")
        val voidBefore = s.inventoryCount("crystal_advanced")
        s = act(s, GameIntent.Enhance("b", useProtection = false, useAlternateMaterial = true))
        assertEquals("frostvein consumed", frostBefore - 1, s.inventoryCount("frostvein"))
        assertEquals("voidforged untouched", voidBefore, s.inventoryCount("crystal_advanced"))
    }

    @Test
    fun `advance attempt consumes the primary material when chosen`() {
        var s = engine.newGame("Tester", clock)
            .addItem("shard_resonance", 100)
            .addItem("frostvein", 10)
            .addItem("crystal_advanced", 10)
            .addEquipmentItem(ItemInstance("b", "sword_cinder", enhancementLevel = 16, durability = 100, maxDurability = 100))
        val frostBefore = s.inventoryCount("frostvein")
        s = act(s, GameIntent.Enhance("b", useProtection = false))
        assertEquals("frostvein untouched", frostBefore, s.inventoryCount("frostvein"))
        assertEquals("voidforged consumed", 9L, s.inventoryCount("crystal_advanced"))
    }

    @Test
    fun `advance rejects when the chosen material is unavailable`() {
        var s = engine.newGame("Tester", clock)
            .addItem("shard_resonance", 100)
            .addItem("frostvein", 10)
            .addEquipmentItem(ItemInstance("b", "sword_cinder", enhancementLevel = 16, durability = 100, maxDurability = 100))
        val levelBefore = s.itemInstance("b")!!.enhancementLevel
        s = act(s, GameIntent.Enhance("b", useProtection = false)) // no voidforged -> no-op
        assertEquals("no primary material -> rejected", levelBefore, s.itemInstance("b")!!.enhancementLevel)
        assertEquals(10L, s.inventoryCount("frostvein"))
    }

    @Test
    fun `enhancement checkpoint quests resolve at their thresholds`() {
        var s = engine.newGame("Tester", clock)
            .unlock("screen:enhance", "region:cindervale", "region:frostreach")
        s = act(s, GameIntent.Tick(clock))
        // grant accumulated enhancement achievements
        for (lvl in listOf(5, 16, 20, 30)) s = s.copy(stats = s.stats.recordEnhance(lvl))
        s = act(s, GameIntent.Tick(clock))
        assertEquals("+5 checkpoint", QuestStatus.COMPLETED, s.quest("q_advance_begin")?.status)
        assertEquals("+16 checkpoint", QuestStatus.COMPLETED, s.quest("q_frostvein_enhance")?.status)
        assertEquals("+20 checkpoint", QuestStatus.COMPLETED, s.quest("q_advance_mid")?.status)
        assertEquals("+30 checkpoint", QuestStatus.COMPLETED, s.quest("q_advance_near")?.status)
        assertNull("+31 gate untouched (no +31 yet)", s.quest("q_light_beyond")?.let { q -> if (q.status == QuestStatus.COMPLETED) q else null })
    }

    @Test
    fun `the plus 31 gate remains required`() {
        var s = engine.newGame("Tester", clock)
            .unlock("screen:enhance", "region:cindervale", "region:frostreach", "region:stormreach", "recipe:refine_catalyst")
            .addItem("catalyst_storm", 1)
        s = act(s, GameIntent.Tick(clock))
        repeat(10) { s = s.copy(stats = s.stats.addKill("ash_beast")) }
        s = s.copy(stats = s.stats.addKill("storm_herald"))
        s = s.copy(stats = s.stats.recordEnhance(30))
        s = act(s, GameIntent.Tick(clock))
        assertFalse("dawnreach not accessible below +31", s.hasUnlock("region:dawnreach"))
        s = s.copy(stats = s.stats.recordEnhance(31))
        s = act(s, GameIntent.Tick(clock))
        assertTrue("dawnreach gated behind +31", s.hasUnlock("region:dawnreach"))
    }

    // ---- Hazard + regional rare yields ----

    @Test
    fun `frostreach hazard chips health but never below the safe floor`() {
        var s = engine.newGame("Tester", clock).unlock("region:frostreach")
        s = s.copy(character = s.character.copy(
            health = 1000,
            baseStats = s.character.baseStats.copy(maxHp = 1000),
        ))
        s = act(s, GameIntent.Tick(clock)) // auto-discover frostreach nodes
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_frost_quarry"))
        s = advance(s, 3600)
        val floor = (CombatStatsMath.effectiveStats(s).maxHp * SimConfig.COMBAT_RETREAT_HP_FRACTION).toInt().coerceAtLeast(1)
        assertTrue("hazard applied", s.character.health < 1000)
        assertTrue("hazard floored safely", s.character.health >= floor)
        assertTrue("frostvein gathered via regional mechanic", s.inventoryCount("frostvein") > 0)
    }

    @Test
    fun `cindervale rare yield produces emberglass`() {
        var s = engine.newGame("Tester", clock).unlock("region:cindervale")
        s = act(s, GameIntent.Tick(clock)) // auto-discover cindervale nodes
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_cinder_quarry"))
        s = advance(s, 6 * 3600)
        assertTrue("emberglass gathered", s.inventoryCount("emberglass") > 0)
    }

    @Test
    fun `non hazardous regions do not drain health`() {
        var s = engine.newGame("Tester", clock)
        s = s.copy(character = s.character.copy(
            health = 100,
            baseStats = s.character.baseStats.copy(maxHp = 100),
        ))
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_quarry"))
        s = advance(s, 3600)
        assertEquals("no hazard in hollowreach", 100, s.character.health)
    }

    // ---- Offline determinism ----

    @Test
    fun `offline hazard and rare yields are deterministic`() {
        var s = engine.newGame("Tester", clock).unlock("region:frostreach")
        s = s.copy(character = s.character.copy(
            health = 1000,
            baseStats = s.character.baseStats.copy(maxHp = 1000),
        ))
        s = act(s, GameIntent.Tick(clock)) // auto-discover frostreach nodes
        s = act(s, GameIntent.StartActivity(ActivityType.GATHERING, "node_frost_quarry"))
        s = s.copy(lastSavedEpochMs = clock)
        val a = OfflineSimulator.simulate(s, clock + 4 * 3600 * 1000).state
        val b = OfflineSimulator.simulate(s, clock + 4 * 3600 * 1000).state
        assertEquals("deterministic frostvein", a.inventoryCount("frostvein"), b.inventoryCount("frostvein"))
        assertEquals("deterministic ore", a.inventoryCount("ore_frost"), b.inventoryCount("ore_frost"))
        assertEquals("deterministic health", a.character.health, b.character.health)
        val floor = (CombatStatsMath.effectiveStats(a).maxHp * SimConfig.COMBAT_RETREAT_HP_FRACTION).toInt().coerceAtLeast(1)
        assertTrue("offline hazard floored", a.character.health >= floor)
    }
}
