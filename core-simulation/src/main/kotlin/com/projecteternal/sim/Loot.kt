package com.projecteternal.sim

import com.projecteternal.content.LootEntry

/**
 * Aggregate loot rolling. For K kills:
 *  - guaranteed entries (whose chances sum to 100) are distributed via
 *    largest-remainder so EXACTLY K items drop,
 *  - optional (rare) entries are sampled once each via Poisson.
 * Cost is O(#loot-entries), independent of K — no per-action replay.
 */
object LootResolver {

    data class LootResult(val items: Map<String, Long>)

    fun roll(kills: Long, lootTable: List<LootEntry>, rng: RandomSource): LootResult {
        val out = mutableMapOf<String, Long>()
        if (kills <= 0) return LootResult(out)

        val guaranteed = lootTable.filter { it.guaranteed }
        val optional = lootTable.filterNot { it.guaranteed }
        val k = kills.toDouble()

        if (guaranteed.isNotEmpty()) {
            val totalChance = guaranteed.sumOf { it.chancePercent }.toDouble()
            val exact = mutableListOf<Pair<LootEntry, Double>>()
            var assigned = 0.0
            for (e in guaranteed) {
                val expected = k * e.chancePercent / totalChance
                exact.add(e to expected)
                assigned += expected
            }
            // largest-remainder distribution to sum exactly to K
            val floors = exact.associate { (e, v) -> e to kotlin.math.floor(v) }
            val used = floors.values.sum()
            val remainder = kills - used.toLong()
            val ordered = exact.sortedByDescending { it.second - kotlin.math.floor(it.second) }
            var extra = remainder
            for ((e, _) in ordered) {
                if (extra <= 0) break
                val base = floors[e]!!.toLong()
                out[e.defId] = (out[e.defId] ?: 0) + base + 1
                extra--
            }
            for ((e, f) in floors) {
                if (!out.containsKey(e.defId)) out[e.defId] = f.toLong()
            }
        }

        for (e in optional) {
            val count = rng.poisson(k * e.chancePercent / 100.0)
            if (count > 0) out[e.defId] = (out[e.defId] ?: 0) + count
        }

        return LootResult(out)
    }
}
