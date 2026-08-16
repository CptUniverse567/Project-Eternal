package com.projecteternal.sim

import com.projecteternal.content.Monsters
import com.projecteternal.model.GameState
import kotlin.math.max
import kotlin.math.min

/**
 * Combat session resolution. Given elapsed seconds and the player's effective
 * stats, returns kills, damage taken, and the resulting health. Deterministic
 * given (state, elapsed) — randomness is only used for loot, which callers
 * resolve separately.
 */
object CombatResolver {

    data class CombatResult(
        val kills: Long,
        val carry: Double,
        val secondsFought: Double,
        val healthFinal: Int,
        val retreated: Boolean,
        val xpGained: Long,
    )

    fun resolve(state: GameState, monsterId: String, elapsedSeconds: Double, carry: Double): CombatResult {
        val p = CombatStatsMath.effectiveStats(state)
        val monster = Monsters.get(monsterId)
        val m = monster.stats

        val killsPerSecond = Rates.killsPerHour(state, monsterId) / 3600.0
        val secondsUntilRetreat = Rates.combatSecondsUntilRetreat(state, monsterId)

        val fought = min(elapsedSeconds, secondsUntilRetreat)
        val grossKills = fought * killsPerSecond + carry
        val kills = kotlin.math.floor(grossKills).toLong()
        val newCarry = grossKills - kills

        val damageTaken = fought * max(0.0, Rates.damageTakenPerSecond(state, monsterId))
        val regen = CombatStatsMath.regenPerSecond(p.maxHp) * elapsedSeconds
        val hpFloor = (p.maxHp * SimConfig.COMBAT_RETREAT_HP_FRACTION).toInt().coerceAtLeast(1)
        val health = (state.character.health - damageTaken + regen).toInt()
            .coerceIn(hpFloor, p.maxHp)

        return CombatResult(
            kills = kills,
            carry = newCarry,
            secondsFought = fought,
            healthFinal = health,
            retreated = fought < elapsedSeconds,
            xpGained = kills * monster.xpReward,
        )
    }
}
