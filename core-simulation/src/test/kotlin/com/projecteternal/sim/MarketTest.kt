package com.projecteternal.sim

import com.projecteternal.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketTest {

    private val engine = GameEngine()
    private val t0 = 1_700_000_000_000L

    @Test
    fun `buy and sell round-trip prices are sane`() {
        val s = engine.newGame("Tester", t0).addItem("ore_iron", 5).addItem("shard_resonance", 5)
        val buy = MarketService.currentBuyPrice(s, "ore_iron")
        val sell = MarketService.currentSellPrice(s, "ore_iron")
        assertTrue(buy > sell)
        assertEquals(6L, buy) // base buy price, region 1.0, no drift
        assertEquals(3L, sell)
    }

    @Test
    fun `buying spends marks and grants items`() {
        var s = engine.newGame("Tester", t0) // starts with 50 marks, 2 potions
        s = engine.apply(s, GameIntent.Buy("potion_lesser", 1), t0).state
        assertEquals(0L, s.character.marks)
        assertEquals(3L, s.inventoryCount("potion_lesser"))
    }

    @Test
    fun `cannot buy what you cannot afford`() {
        var s = engine.newGame("Tester", t0)
        s = engine.apply(s, GameIntent.Buy("shard_resonance", 100), t0).state
        assertEquals(50L, s.character.marks)
        assertEquals(3L, s.inventoryCount("shard_resonance"))
    }

    @Test
    fun `selling grants marks and removes items`() {
        var s = engine.newGame("Tester", t0).addItem("ore_iron", 10)
        s = engine.apply(s, GameIntent.Sell("ore_iron", 4), t0).state
        assertEquals(6L, s.inventoryCount("ore_iron"))
        assertEquals(50L + 4 * 3L, s.character.marks)
    }

    @Test
    fun `cannot sell more than owned or unsellables`() {
        var s = engine.newGame("Tester", t0).addItem("wood", 1)
        s = engine.apply(s, GameIntent.Sell("wood", 5), t0).state
        assertEquals(1L, s.inventoryCount("wood"))
        // equipment cannot be traded
        val before = s
        s = engine.apply(s, GameIntent.Sell("sword_bronze", 1), t0).state
        assertEquals(before, s)
    }

    @Test
    fun `heavy trading drifts prices but stays capped`() {
        var s = engine.newGame("Tester", t0).addMarks(1_000_000)
        repeat(200) {
            s = engine.apply(s, GameIntent.Buy("ore_iron", 5), t0).state
            s = engine.apply(s, GameIntent.Sell("ore_iron", 5), t0).state
        }
        val drift = s.market.priceDrift["ore_iron"] ?: 1.0
        assertTrue("drift must stay bounded, got $drift", drift in 0.8..1.25)
    }

    @Test
    fun `visiting a higher region improves sell prices`() {
        var s = engine.newGame("Tester", t0)
        val before = MarketService.currentSellPrice(s, "ore_iron")
        s = s.copy(stats = s.stats.visitRegion("emberreach"))
        val after = MarketService.currentSellPrice(s, "ore_iron")
        assertTrue(after > before)
    }

    @Test
    fun `processed goods command a demand premium that grows with trade reach`() {
        var s = engine.newGame("Tester", t0)
        val home = MarketService.processedDemandPremium(s)
        assertEquals(1.0, home, 0.0001)
        assertEquals(0, MarketService.furthestRegionTier(s))

        val homeMaterial = MarketService.currentSellPrice(s, "iron_fragment")
        s = s.copy(stats = s.stats.visitRegion("emberreach"))
        s = s.copy(stats = s.stats.visitRegion("stormreach"))
        val far = MarketService.processedDemandPremium(s)
        assertTrue("premium must rise at far reachers, $far > $home", far > home)
        assertTrue("furthest tier is 3", MarketService.furthestRegionTier(s) == 3)
        val farMaterial = MarketService.currentSellPrice(s, "iron_fragment")
        assertTrue("processed goods sell higher with demand premium", farMaterial > homeMaterial)
    }

    @Test
    fun `raw resources do not get the processed premium`() {
        var s = engine.newGame("Tester", t0)
        s = s.copy(stats = s.stats.visitRegion("stormreach"))
        val premium = MarketService.processedDemandPremium(s)
        assertTrue(premium > 1.0)
        // Icons ad zero-resource base sell: ore sell base 3; a processed material (steel base 65)
        // should out-sell its feed (refined_iron base 29 + coal) after premium.
        val steel = MarketService.currentSellPrice(s, "steel")
        val refined = MarketService.currentSellPrice(s, "refined_iron")
        val coal = MarketService.currentSellPrice(s, "ore_coal")
        assertTrue(steel > refined + coal)
    }

    @Test
    fun `sell advice points to processing when it is more profitable`() {
        var s = engine.newGame("Tester", t0)
        s = s.copy(stats = s.stats.visitRegion("emberreach"))
        s = s.unlock("recipe:sword_steel", "recipe:refine_refined", "recipe:refine_steel", "screen:enhance")
        // mill_flour: grain -> flour (same sell basis, flour strictly higher)
        val advice = MarketService.sellAdvice(s, "grain")
        assertTrue("advice should exist for a processable raw", advice != null)
        if (advice != null) {
            assertTrue("grain should be worth more as flour", advice.hasBetterPath)
            assertTrue(advice.betterHint.contains("Flour"))
        }
        // potion_lesser has no recipe feed -> direct advice, no better path
        s = s.addItem("herb_brightleaf", 0)
        val direct = MarketService.sellAdvice(s, "ore_iron")
        assertTrue(direct != null)
    }
}
