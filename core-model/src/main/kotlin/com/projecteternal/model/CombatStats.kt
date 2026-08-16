package com.projecteternal.model

import kotlinx.serialization.Serializable

/**
 * Combat-relevant stats shared by the player character and monsters.
 * All numeric fields are non-negative. Percent fields (accuracy, evasion,
 * critChance) are integer percents.
 */
@Serializable
data class CombatStats(
    val attack: Int = 1,
    val defense: Int = 0,
    val accuracy: Int = 50,
    val evasion: Int = 5,
    val critChance: Int = 5,
    val critMultiplier: Double = 1.5,
    val attackSpeed: Double = 1.0,
    val maxHp: Int = 100,
    val resistance: Int = 0,
) {
    fun plus(other: CombatStats): CombatStats = CombatStats(
        attack = attack + other.attack,
        defense = defense + other.defense,
        accuracy = accuracy + other.accuracy,
        evasion = evasion + other.evasion,
        critChance = critChance + other.critChance,
        critMultiplier = critMultiplier + other.critMultiplier - 1.0,
        attackSpeed = attackSpeed + other.attackSpeed,
        maxHp = maxHp + other.maxHp,
        resistance = resistance + other.resistance,
    )
}
