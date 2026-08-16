package com.projecteternal.sim

import com.projecteternal.model.GameState
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Single seeded RNG through which ALL random rolls flow (loot, enhancement,
 * rare events). Offline simulation is deterministic given the same seed, so
 * tests can reproduce a full session exactly.
 */
class RandomSource(
    seed: Long,
    private val rng: Random = Random(seed),
) {

    fun nextDouble(): Double = rng.nextDouble()

    fun nextInt(bound: Int): Int = rng.nextInt(bound)

    fun nextLong(): Long = rng.nextLong()

    fun chance(percent: Double): Boolean = rng.nextDouble() * 100.0 < percent

    fun intInRange(min: Int, max: Int): Int =
        if (max <= min) min else min + rng.nextInt(max - min + 1)

    fun longInRange(min: Long, max: Long): Long {
        if (max <= min) return min
        return min + rng.nextLong(max - min + 1)
    }

    /** Poisson-distributed count with expected value [lambda]. */
    fun poisson(lambda: Double): Long {
        if (lambda <= 0.0) return 0
        if (lambda > 100.0) {
            val z = normalSample()
            return max(0.0, lambda + sqrt(lambda) * z).roundToLong()
        }
        val limit = exp(-lambda)
        var p = 1.0
        var k = 0
        while (p > limit) {
            p *= rng.nextDouble()
            k++
            if (k > 100000) break
        }
        return (k - 1).toLong()
    }

    private fun normalSample(): Double {
        val u1 = rng.nextDouble().coerceIn(1e-12, 1.0)
        val u2 = rng.nextDouble()
        return sqrt(-2.0 * ln(u1)) * kotlin.math.cos(2.0 * kotlin.math.PI * u2)
    }

    companion object {
        /** Advance the state's deterministic seed and hand out a new RNG. */
        fun next(state: GameState): Pair<GameState, RandomSource> {
            val (next, seed) = state.nextSeed()
            return next to RandomSource(seed)
        }
    }
}
