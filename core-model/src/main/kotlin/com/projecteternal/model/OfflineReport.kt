package com.projecteternal.model

import kotlinx.serialization.Serializable

/**
 * Legible summary of everything that happened while the app was closed.
 * Produced by the offline simulator; shown once on resume as the
 * "While you were away" screen.
 */
@Serializable
data class OfflineReport(
    val elapsedSeconds: Long = 0,
    val elapsedClamped: Boolean = false,
    val activityLabel: String? = null,
    val charXpGained: Long = 0,
    val skillXpGained: Map<SkillId, Long> = emptyMap(),
    val resourcesGained: Map<ItemId, Long> = emptyMap(),
    val resourcesConsumed: Map<ItemId, Long> = emptyMap(),
    val marksGained: Long = 0,
    val marksSpent: Long = 0,
    val kills: Map<MonsterId, Long> = emptyMap(),
    val retainerOutput: Map<RetainerId, Map<ItemId, Long>> = emptyMap(),
    val notableEvents: List<String> = emptyList(),
    val itemsBroken: List<String> = emptyList(),
    val newLevels: Map<String, Int> = emptyMap(),
    val questsCompleted: List<String> = emptyList(),
) {
    fun isEmpty(): Boolean =
        charXpGained == 0L && skillXpGained.isEmpty() && resourcesGained.isEmpty() &&
            marksGained == 0L && kills.isEmpty() && retainerOutput.isEmpty()

    /** Merge two reports (used when multiple sources contribute). */
    fun merge(other: OfflineReport): OfflineReport = OfflineReport(
        elapsedSeconds = elapsedSeconds + other.elapsedSeconds,
        elapsedClamped = elapsedClamped || other.elapsedClamped,
        activityLabel = activityLabel ?: other.activityLabel,
        charXpGained = charXpGained + other.charXpGained,
        skillXpGained = mergeMaps(skillXpGained, other.skillXpGained),
        resourcesGained = mergeMaps(resourcesGained, other.resourcesGained),
        resourcesConsumed = mergeMaps(resourcesConsumed, other.resourcesConsumed),
        marksGained = marksGained + other.marksGained,
        marksSpent = marksSpent + other.marksSpent,
        kills = mergeMaps(kills, other.kills),
        retainerOutput = other.retainerOutput + retainerOutput.mapValues { (id, m) ->
            mergeMaps(m, other.retainerOutput[id] ?: emptyMap())
        },
        notableEvents = notableEvents + other.notableEvents,
        itemsBroken = itemsBroken + other.itemsBroken,
        newLevels = mergeIntMaps(newLevels, other.newLevels),
        questsCompleted = questsCompleted + other.questsCompleted,
    )

    companion object {
        fun mergeMaps(a: Map<String, Long>, b: Map<String, Long>): Map<String, Long> {
            val out = a.toMutableMap()
            for ((k, v) in b) out[k] = (out[k] ?: 0) + v
            return out
        }

        fun mergeIntMaps(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> {
            val out = a.toMutableMap()
            for ((k, v) in b) out[k] = (out[k] ?: 0) + v
            return out
        }
    }
}
