package com.projecteternal.content

import com.projecteternal.model.SkillId

/**
 * Pure level scaling curves. Deterministic functions of (level); character and
 * skill curves intentionally diverge so progression pacing differs.
 */
object LevelCurves {

    /** Cumulative XP required to reach the given character level. */
    fun characterXpToLevel(level: Int): Long =
        (1 until level).sumOf { characterXpToNext(it) }

    /** XP needed to advance FROM `level` to level+1. */
    fun characterXpToNext(level: Int): Long =
        (25L * level * (level + 2)).coerceAtLeast(10L)

    /** Current character level for a raw XP total. */
    fun characterLevelFromXp(totalXp: Long): Int {
        var level = 1
        var remaining = totalXp
        while (remaining >= characterXpToNext(level)) {
            remaining -= characterXpToNext(level)
            level++
        }
        return level
    }

    /** XP needed to advance a skill FROM `level` to level+1. */
    fun skillXpToNext(level: Int): Long =
        (12L * level * (level + 1)).coerceAtLeast(5L)

    /** Current skill level for a raw skill XP total. */
    fun skillLevelFromXp(totalXp: Long): Int {
        var level = 1
        var remaining = totalXp
        while (remaining >= skillXpToNext(level)) {
            remaining -= skillXpToNext(level)
            level++
        }
        return level
    }
}

/**
 * Aggregation point for content catalogs, so the rest of the app depends on
 * one object rather than five.
 */
object Catalog {
    val items = Items
    val monsters = Monsters
    val nodes = Nodes
    val regions = Regions
    val recipes = Recipes
    val skills = Skills
    val quests = Quests
    val enhancement = EnhancementTables
    val curves = LevelCurves
}