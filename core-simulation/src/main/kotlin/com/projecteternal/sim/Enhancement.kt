package com.projecteternal.sim

import com.projecteternal.content.EnhancementTable
import com.projecteternal.model.GameState
import com.projecteternal.model.ItemInstance
import kotlin.math.min

/**
 * Enhancement resolver. `successProbability` is a pure function of
 * (currentLevel, resolve, table) so probability curves are fully testable.
 * Each failed attempt adds Resolve, which raises the next attempt's chance;
 * a success consumes all accumulated Resolve.
 */
object EnhancementResolver {

    fun successProbability(level: Int, resolve: Long, table: EnhancementTable): Double {
        val base = table.baseSuccessPercent[level] ?: return 0.0
        val bonus = min(resolve.toDouble(), table.maxResolveBonusPercent.toDouble())
        return min(base + bonus, 95.0)
    }

    data class AttemptResult(
        val success: Boolean,
        val newLevel: Int,
        val newResolve: Long,
        val durabilityLoss: Int,
        val consumedShards: Long,
        val consumedMaterial: Boolean,
        val consumedProtection: Boolean,
        val consumedFullNegation: Boolean = false,
    )

    data class Precondition(val ok: Boolean, val reason: String = "")

    /** Verify shards, material, durability and level ceiling before an attempt. */
    fun checkPreconditions(state: GameState, item: ItemInstance): Precondition {
        val level = item.enhancementLevel
        val table = com.projecteternal.content.EnhancementTables.tableFor(level)
            ?: return Precondition(false, "Already at the maximum enhancement.")
        if (item.durability <= 0) return Precondition(false, "Item is broken — repair it first.")
        val shards = table.shardsPerAttempt[level] ?: return Precondition(false, "No table data.")
        if (state.inventoryCount("shard_resonance") < shards) {
            return Precondition(false, "Need $shards Resonance Shards.")
        }
        val mat = table.materialPerAttempt
        if (mat != null && state.inventoryCount(mat.defId) < mat.count) {
            return Precondition(false, "Need ${mat.count}x ${mat.defId.replace('_', ' ')}.")
        }
        if (table.unlockToken.isNotEmpty() && !state.hasUnlock(table.unlockToken)) {
            return Precondition(false, "This enhancement tier is locked.")
        }
        return Precondition(true)
    }

    /**
     * Roll one attempt. [useProtection] consumes a protection item to block a
     * downgrade on failure; [useFullNegation] (when a full-negation item is
     * owned) blocks BOTH the downgrade and the durability loss. The state
     * mutation itself is done by the caller so this stays a pure function.
     */
    fun attempt(
        item: ItemInstance,
        resolve: Long,
        useProtection: Boolean,
        hasProtection: Boolean,
        rng: RandomSource,
        useFullNegation: Boolean = false,
        hasFullNegation: Boolean = false,
    ): AttemptResult {
        val level = item.enhancementLevel
        val table = com.projecteternal.content.EnhancementTables.tableFor(level)
            ?: return AttemptResult(false, level, resolve, 0, 0, false, false)

        val shards = table.shardsPerAttempt[level] ?: 0L
        val p = successProbability(level, resolve, table)
        val success = rng.chance(p)

        val consumeFullNegation = useFullNegation && !success && hasFullNegation
        val consumeProtection = useProtection && !success && hasProtection && !consumeFullNegation
        val failProtected = consumeFullNegation || consumeProtection

        var durabilityLoss = 0
        var newLevel = level
        var newResolve = resolve

        if (success) {
            newLevel = min(level + 1, table.maxLevel + 1)
            newResolve = 0
        } else {
            durabilityLoss = if (consumeFullNegation) 0 else table.durabilityLossOnFail
            newResolve = resolve + table.resolvePerFail
            val consequence = if (level >= table.downgradeThreshold) table.failure
            else com.projecteternal.content.FailureConsequence.DURABILITY_ONLY
            if (!failProtected) {
                when (consequence) {
                    com.projecteternal.content.FailureConsequence.DURABILITY_ONLY -> {}
                    com.projecteternal.content.FailureConsequence.DOWNGRADE_ONE ->
                        newLevel = maxOf(level - 1, 0)
                    com.projecteternal.content.FailureConsequence.DOWNGRADE_TWO ->
                        newLevel = maxOf(level - 2, 0)
                    com.projecteternal.content.FailureConsequence.SHATTER_TO_BAND_FLOOR ->
                        newLevel = table.minLevel
                }
            }
        }

        return AttemptResult(
            success = success,
            newLevel = newLevel,
            newResolve = newResolve,
            durabilityLoss = durabilityLoss,
            consumedShards = shards,
            consumedMaterial = table.materialPerAttempt != null,
            consumedProtection = consumeProtection,
            consumedFullNegation = consumeFullNegation,
        )
    }
}
