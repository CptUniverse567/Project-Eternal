package com.projecteternal.sim

import com.projecteternal.content.Items
import com.projecteternal.content.Regions
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemKind
import kotlin.math.max
import kotlin.math.min

/**
 * Local market. Prices derive from content base prices and the best region the
 * player has visited (Reach price modifiers). Each trade shifts local demand
 * pressure, which drifts prices — capped so the market never spirals.
 *
 * Depth (§23): market demand is *not* linear — processed goods (materials,
 * consumables, equipment) carry a demand premium that grows with the furthest
 * Reach visited, so "process then sell" beats dumping raw ore.
 */
object MarketService {
    /** Kind categories treated as "processed" for demand purposes. */
    private val PROCESSED_KINDS = setOf(ItemKind.MATERIAL, ItemKind.CONSUMABLE, ItemKind.EQUIPMENT)

    /** The most lucrative region modifier visited so far. */
    fun bestRegionModifier(state: GameState): Double {
        var best = 1.0
        for (region in state.stats.visitedRegions) {
            best = max(best, Regions.get(region).priceModifier)
        }
        return best
    }

    /** Furthest region tier visited. Used by the processed-goods demand curve. */
    fun furthestRegionTier(state: GameState): Int {
        var best = 0
        for (region in state.stats.visitedRegions) {
            best = max(best, Regions.get(region).tier)
        }
        return best
    }

    /**
     * Demand multiplier for processed goods. Rises from 1.0 at home up to 1.25
     * at the furthest Reach — far off towns pay more for finished goods.
     */
    fun processedDemandPremium(state: GameState): Double =
        1.0 + 0.05 * min(furthestRegionTier(state), 5)

    fun currentBuyPrice(state: GameState, defId: String): Long {
        val def = Items.get(defId)
        val drift = state.market.priceDrift[defId] ?: 1.0
        return max(1, (def.buyPrice * bestRegionModifier(state) * drift).toLong())
    }

    fun currentSellPrice(state: GameState, defId: String): Long {
        val def = Items.get(defId)
        val drift = state.market.priceDrift[defId] ?: 1.0
        val regionFactor = bestRegionModifier(state)
        val sell = if (def.kind in PROCESSED_KINDS) {
            def.sellPrice * regionFactor * processedDemandPremium(state) / drift
        } else {
            def.sellPrice * regionFactor / drift
        }
        return max(1, sell.toLong())
    }

    /**
     * Decision helper: total Marks from selling [count] of [defId] versus the
     * best available sale of the same raw materials. Returns sell-vs-use advice
     * strings for the UI, and the integer projected values.
     */
    data class SellAdvice(
        val item: String,
        val directSellMarks: Long,
        val hasBetterPath: Boolean,
        val betterHint: String,
    )

    /**
     * Crude but honest "process or sell?" hint: if any recipe turns [defId]
     * into something whose sale outvalues selling the raw feed, say so.
     */
    fun sellAdvice(state: GameState, defId: String): SellAdvice? {
        val def = Items.get(defId)
        if (def.sellPrice <= 0) return null
        val direct = max(1, currentSellPrice(state, defId))

        // best crafted alternative: for recipes that consume this item, compare
        // (output sell price per unit output) * gcdCount vs (feed sell per unit)
        val recipes = com.projecteternal.content.Recipes.availableRecipes(state.unlocks)
        var bestOutputPrice = 0L
        var bestRecipe: com.projecteternal.content.RecipeDefinition? = null
        for (r in recipes) {
            val feed = r.inputs.firstOrNull { it.defId == defId } ?: continue
            if (feed.count <= 0) continue
            for (o in r.outputs) {
                if (!Items.get(o.defId).stackable) continue
                val price = currentSellPrice(state, o.defId) * o.count
                val perFeed = price / feed.count
                if (perFeed > bestOutputPrice) {
                    bestOutputPrice = perFeed
                    bestRecipe = r
                }
            }
        }
        if (bestRecipe == null || bestOutputPrice <= direct) {
            return SellAdvice(def.name, direct, false, "")
        }
        return SellAdvice(
            item = def.name,
            directSellMarks = direct,
            hasBetterPath = true,
            betterHint = "Process into ${Items.get(bestRecipe!!.outputs.first().defId).name} for ${bestOutputPrice}◎/unit",
        )
    }

    enum class TradeKind { BUY, SELL }

    fun recordTrade(state: GameState, defId: String, count: Long, kind: TradeKind): GameState {
        if (count <= 0) return state
        val delta = if (kind == TradeKind.BUY) count.toDouble() * 0.004 else -count.toDouble() * 0.004
        val current = state.market.priceDrift[defId] ?: 1.0
        val next = (current + delta).coerceIn(0.8, 1.25)
        return state.copy(
            market = state.market.copy(
                priceDrift = state.market.priceDrift + (defId to next),
                totalTrades = state.market.totalTrades + count,
            ),
        )
    }

    fun canBuy(state: GameState, defId: String, count: Long): Boolean =
        state.character.marks >= currentBuyPrice(state, defId) * count

    fun canSell(state: GameState, defId: String, count: Long): Boolean =
        state.inventoryCount(defId) >= count && Items.get(defId).sellPrice > 0
}