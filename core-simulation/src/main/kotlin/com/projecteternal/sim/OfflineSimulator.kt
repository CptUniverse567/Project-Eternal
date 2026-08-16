package com.projecteternal.sim

import com.projecteternal.content.Nodes
import com.projecteternal.content.Quests
import com.projecteternal.content.Recipes
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import com.projecteternal.model.OfflineReport
import kotlin.math.min

/**
 * Offline progression (§7.2). Given (state, elapsed), resolves the whole
 * absence with closed-form math — O(#content) work regardless of whether the
 * absence was 1 hour or 30 days. Below a threshold it steps per-second for
 * fidelity. Deterministic for a fixed state (all randomness flows through a
 * single seeded [RandomSource]).
 */
object OfflineSimulator {

    data class OfflineResult(val state: GameState, val report: OfflineReport)

    fun simulate(state: GameState, now: Long): OfflineResult {
        val lastSaved = state.lastSavedEpochMs
        val rawElapsedSeconds = ((now - lastSaved).coerceAtLeast(0)) / 1000.0
        var elapsed = rawElapsedSeconds.toLong()
        var clamped = false
        if (elapsed > SimConfig.MAX_OFFLINE_SECONDS) {
            elapsed = SimConfig.MAX_OFFLINE_SECONDS
            clamped = true
        }
        if (elapsed <= 0) {
            return OfflineResult(state, OfflineReport())
        }

        val (s0, rng) = RandomSource.next(state)
        val startLevels = levelSnapshot(s0)

        val gained = mutableMapOf<String, Long>()
        val consumed = mutableMapOf<String, Long>()
        val skillXp = mutableMapOf<String, Long>()
        val kills = mutableMapOf<String, Long>()
        val retainerOutput = mutableMapOf<String, Map<String, Long>>()
        val brokenUids = mutableListOf<String>()
        var charXp = 0L

        var s = s0

        fun accumulate(a: ActivityEngine.Delta, r: RetainerEngine.Delta) {
            for ((k, v) in a.resourcesGained) gained[k] = (gained[k] ?: 0) + v
            for ((k, v) in a.resourcesConsumed) consumed[k] = (consumed[k] ?: 0) + v
            for ((k, v) in a.skillXp) skillXp[k] = (skillXp[k] ?: 0) + v
            for ((k, v) in a.kills) kills[k] = (kills[k] ?: 0) + v
            for (id in a.brokenUids) brokenUids.add(id)
            charXp += a.charXp
            for ((id, out) in r.output) retainerOutput[id] = out
        }

        if (elapsed < SimConfig.STEPWISE_THRESHOLD_SECONDS) {
            var remaining = elapsed.toDouble()
            while (remaining > 0) {
                val step = min(1.0, remaining)
                val (sa, da) = ActivityEngine.advance(s, step, rng)
                val (sr, dr) = RetainerEngine.advance(sa, step, rng)
                s = sr
                accumulate(da, dr)
                remaining -= step
            }
        } else {
            val (sa, da) = ActivityEngine.advance(s, elapsed.toDouble(), rng)
            val (sr, dr) = RetainerEngine.advance(sa, elapsed.toDouble(), rng)
            s = sr
            accumulate(da, dr)
        }

        val (se, events) = NotableEvents.roll(s, rng)
        s = se

        val (sq, completed) = QuestEngine.process(s, now)
        s = sq

        val report = OfflineReport(
            elapsedSeconds = elapsed,
            elapsedClamped = clamped,
            activityLabel = activityLabel(s0),
            charXpGained = charXp,
            skillXpGained = skillXp,
            resourcesGained = gained,
            resourcesConsumed = consumed,
            marksGained = (s.character.marks - s0.character.marks).coerceAtLeast(0),
            marksSpent = (s0.character.marks - s.character.marks).coerceAtLeast(0),
            kills = kills,
            retainerOutput = retainerOutput,
            notableEvents = events,
            itemsBroken = brokenUids.mapNotNull { uid ->
                s0.equipmentItems.firstOrNull { it.uid == uid }
                    ?.let { com.projecteternal.content.Items.get(it.defId).name }
                    ?: uid
            },
            newLevels = levelDiff(startLevels, s),
            questsCompleted = completed.map { Quests.get(it).name },
        )

        s = s.copy(
            lastSavedEpochMs = lastSaved + elapsed * 1000,
            totalPlaySeconds = s.totalPlaySeconds + elapsed,
            pendingOfflineReport = if (elapsed >= SimConfig.MIN_ELAPSED_FOR_REPORT_SECONDS) report else null,
        )

        return OfflineResult(s, report)
    }

    private fun activityLabel(state: GameState): String? {
        val a = state.character.currentActivity ?: return null
        return when (a.type) {
            ActivityType.GATHERING -> "Gathering at ${Nodes.get(a.targetId).name}"
            ActivityType.COMBAT -> "Hunting ${com.projecteternal.content.Monsters.get(a.targetId).name}"
            ActivityType.PROCESSING, ActivityType.CRAFTING -> "Working on ${Recipes.get(a.targetId).name}"
        }
    }

    private fun levelSnapshot(state: GameState): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        out["character"] = state.character.level
        for ((skill, _) in state.character.skillLevels) out["skill:$skill"] = state.character.skillLevel(skill)
        return out
    }

    private fun levelDiff(before: Map<String, Int>, after: GameState): Map<String, Int> {
        val out = mutableMapOf<String, Int>()
        val keys = before.keys + after.character.skillLevels.keys.map { "skill:$it" } + "character"
        for (key in keys) {
            val prev = when {
                key == "character" -> before["character"] ?: 1
                key.startsWith("skill:") -> before[key] ?: 0
                else -> 0
            }
            val curr = when {
                key == "character" -> after.character.level
                key.startsWith("skill:") -> after.character.skillLevel(key.removePrefix("skill:"))
                else -> 0
            }
            if (curr > prev) out[key] = curr
        }
        return out
    }
}
