package com.projecteternal.model

import kotlinx.serialization.Serializable

/** Generic quest objective: [type] + parameters, evaluated against game state. */
@Serializable
data class Objective(
    val id: String,
    val type: ObjectiveType,
    val targetId: String = "",
    val targetCount: Long = 1,
    val description: String = "",
)

/** Runtime state of one quest in the player's journal. */
@Serializable
data class QuestProgress(
    val questId: QuestId,
    val status: QuestStatus = QuestStatus.ACTIVE,
    val objectiveProgress: Map<String, Long> = emptyMap(),
    val acceptedAtEpochMs: Long = 0,
    val completedAtEpochMs: Long? = null,
) {
    fun withObjectiveProgress(id: String, value: Long): QuestProgress {
        val next = objectiveProgress.toMutableMap()
        next[id] = value
        return copy(objectiveProgress = next)
    }
}
