package com.projecteternal.sim

import com.projecteternal.content.Items
import com.projecteternal.content.LevelCurves
import com.projecteternal.content.Skills
import com.projecteternal.model.CombatStats
import com.projecteternal.model.GameState
import kotlin.math.roundToInt

/** Combat math helpers. Formulas are transparent and unit-tested. */
object CombatStatsMath {

    /** Level-derived stat bonus. */
    fun levelBonus(level: Int): CombatStats = CombatStats(
        attack = (level - 1) * 2,
        defense = level - 1,
        accuracy = level - 1,
        evasion = level - 1,
        maxHp = (level - 1) * 10,
    )

    /** Equipment + level + base stats, fully resolved. */
    fun effectiveStats(state: GameState): CombatStats {
        val c = state.character
        var s = c.baseStats.plus(levelBonus(c.level))
        for (inst in c.equipped.values) {
            val def = Items.get(inst.defId)
            val base = def.baseStats ?: continue
            val growth = com.projecteternal.content.EnhancementTables
                .tableFor(inst.enhancementLevel)?.statMultiplierPerLevel ?: 0.15
            val mult = 1.0 + inst.enhancementLevel * growth
            s = s.plus(scale(base, mult))
        }
        return s
    }

    fun scale(stats: CombatStats, mult: Double): CombatStats = CombatStats(
        attack = (stats.attack * mult).roundToInt(),
        defense = (stats.defense * mult).roundToInt(),
        accuracy = (stats.accuracy * mult).roundToInt(),
        evasion = (stats.evasion * mult).roundToInt(),
        critChance = stats.critChance,
        critMultiplier = stats.critMultiplier,
        attackSpeed = stats.attackSpeed * mult,
        maxHp = (stats.maxHp * mult).roundToInt(),
        resistance = (stats.resistance * mult).roundToInt(),
    )

    fun hitChance(attackerAccuracy: Int, defenderEvasion: Int): Int =
        (attackerAccuracy - defenderEvasion).coerceIn(5, 95)

    /** Expected damage of one hit, averaging crits. */
    fun expectedHitDamage(attack: Int, critChance: Int, critMultiplier: Double, defense: Int): Int {
        val avgCritMult = 1.0 + (critChance / 100.0) * (critMultiplier - 1.0)
        val raw = attack * avgCritMult - defense * 0.5
        return kotlin.math.max(1, raw.roundToInt())
    }

    fun regenPerSecond(maxHp: Int): Double =
        maxHp * SimConfig.REGEN_FRACTION_PER_SECOND

    fun maxSkillLevel(skillId: String, skillXp: Long): Int =
        LevelCurves.skillLevelFromXp(skillXp)

    /** Skill mastery: the multiplier a level grants for [PerkType]. */
    fun masteryMultiplier(skillId: String, level: Int): Double {
        val def = Skills.get(skillId)
        val value = def.perkValuePerLevel * (level - 1)
        return 1.0 + value / 100.0
    }
}
