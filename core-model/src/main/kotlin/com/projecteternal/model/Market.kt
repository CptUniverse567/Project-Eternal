package com.projecteternal.model

import kotlinx.serialization.Serializable

/**
 * Local economy state. Base prices live in content; this captures drift from
 * the baseline (demand elasticity) per resource and per region.
 */
@Serializable
data class MarketState(
    val priceDrift: Map<String, Double> = emptyMap(), // itemId -> multiplier (1.0 = baseline)
    val demandPressure: Map<String, Double> = emptyMap(), // itemId -> current local demand factor
    val lastUpdatedEpochMs: Long = 0,
    val totalTrades: Long = 0,
)

/**
 * Cumulative cross-session statistics. Powers generic quest objectives
 * (kills, crafts, discoveries) and the offline report.
 */
@Serializable
data class StatsTracker(
    val killCounts: Map<MonsterId, Long> = emptyMap(),
    val craftCounts: Map<ItemId, Long> = emptyMap(),
    val gatherCounts: Map<ItemId, Long> = emptyMap(),
    val totalMarksEarned: Long = 0,
    val totalMarksSpent: Long = 0,
    val maxEnhanceAchieved: Int = 0,
    val visitedRegions: Set<RegionId> = emptySet(),
    val completedQuestCount: Int = 0,
) {
    fun addKill(monster: MonsterId): StatsTracker =
        copy(killCounts = killCounts.plus(monster to (killCounts[monster] ?: 0) + 1))

    fun addCraft(item: ItemId, count: Long = 1): StatsTracker =
        copy(craftCounts = craftCounts.plus(item to (craftCounts[item] ?: 0) + count))

    fun addGather(item: ItemId, count: Long): StatsTracker =
        copy(gatherCounts = gatherCounts.plus(item to (gatherCounts[item] ?: 0) + count))

    fun recordEnhance(level: Int): StatsTracker =
        if (level > maxEnhanceAchieved) copy(maxEnhanceAchieved = level) else this

    fun visitRegion(region: RegionId): StatsTracker =
        copy(visitedRegions = visitedRegions + region)
}
