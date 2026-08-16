package com.projecteternal.model

import kotlinx.serialization.Serializable

/** What the player character was doing when the app left the foreground. */
@Serializable
data class ActivityState(
    val type: ActivityType,
    val targetId: String,
    val startedAtEpochMs: Long,
    /** Fractional completed actions carried across ticks for determinism. */
    val carry: Double = 0.0,
) {
    companion object {
        fun combat(monsterId: String, now: Long) =
            ActivityState(ActivityType.COMBAT, monsterId, now)

        fun gathering(nodeId: String, now: Long) =
            ActivityState(ActivityType.GATHERING, nodeId, now)

        fun processing(recipeId: String, now: Long) =
            ActivityState(ActivityType.PROCESSING, recipeId, now)

        fun crafting(recipeId: String, now: Long) =
            ActivityState(ActivityType.CRAFTING, recipeId, now)
    }
}

/** The player character. Level and per-skill levels are authoritative. */
@Serializable
data class Character(
    val name: String,
    val level: Int = 1,
    val xp: Long = 0,
    val skillLevels: Map<SkillId, Int> = emptyMap(),
    val skillXp: Map<SkillId, Long> = emptyMap(),
    val baseStats: CombatStats = CombatStats(),
    val equipped: Map<EquipSlot, ItemInstance> = emptyMap(),
    val marks: Long = 0,
    val health: Int = 100,
    val resolve: Long = 0,
    val currentActivity: ActivityState? = null,
) {
    val maxHp: Int get() = baseStats.maxHp

    fun skillLevel(skill: SkillId): Int = skillLevels[skill] ?: 0
}
