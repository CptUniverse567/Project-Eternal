package com.projecteternal.sim

import com.projecteternal.content.Items
import com.projecteternal.content.Monsters
import com.projecteternal.content.NodeDefinition
import com.projecteternal.content.RecipeDefinition
import com.projecteternal.content.Regions
import com.projecteternal.content.RetainerTraits
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.NodeType
import com.projecteternal.model.Retainer
import kotlin.math.max

/**
 * Shared rate functions. Foreground ticking and the offline simulator use the
 * SAME functions here — there is exactly one implementation of "how fast does
 * mining happen".
 */
object Rates {

    /** Effective player gathering actions per hour at a node. */
    fun gatheringActionsPerHour(state: GameState, node: NodeDefinition): Double {
        var rate = node.actionsPerHour
        val skill = node.skills.firstOrNull() ?: return rate
        val level = state.character.skillLevel(skill)
        rate *= 1.0 + level * 0.02
        rate *= toolMultiplierFor(state, node)
        return rate
    }

    /** Equipped TOOL bonus applied when its gathering skill matches the node. */
    private fun toolMultiplierFor(state: GameState, node: NodeDefinition): Double {
        val tool = state.character.equipped[EquipSlot.TOOL] ?: return 1.0
        val def = Items.get(tool.defId)
        val skill = def.gatheringSkill ?: return 1.0
        if (!node.skills.contains(skill)) return 1.0
        return def.gatheringSpeedMultiplier
    }

    /** Yield multiplier from gathering skill mastery and regional boons. */
    fun gatheringYieldMultiplier(state: GameState, node: NodeDefinition): Double {
        val skill = node.skills.firstOrNull() ?: return 1.0
        val mastery = CombatStatsMath.masteryMultiplier(skill, state.character.skillLevel(skill))
        return mastery * Regions.get(node.regionId).yieldMultiplier
    }

    /** Processing/crafting actions per hour from a recipe. */
    fun recipeActionsPerHour(state: GameState, recipe: RecipeDefinition): Double {
        val level = state.character.skillLevel(recipe.skillId)
        val efficiency = CombatStatsMath.masteryMultiplier(recipe.skillId, level)
        val base = 3600.0 / recipe.timeSeconds
        return base * efficiency
    }

    /** Retainer gathering actions per hour at their assigned node. */
    fun retainerActionsPerHour(retainer: Retainer, node: NodeDefinition): Double {
        var rate = node.actionsPerHour
        rate *= retainer.gatheringSpeed
        rate *= 1.0 + retainer.level * 0.02
        rate *= retainer.productionSpeed
        rate *= RetainerTraits.speedMultiplier(retainer.traitIds)
        // FORGER affinity: gathers faster at SPECIAL and industrial (MINE) nodes.
        if (retainer.specialization == com.projecteternal.model.RetainerSpecialization.FORGER &&
            (node.type == NodeType.SPECIAL || node.type == NodeType.MINE)
        ) {
            rate *= SimConfig.FORGER_NODE_MULTIPLIER
        }
        return rate
    }

    /** Fractional monster kills per hour for the current player vs [monsterId]. */
    fun killsPerHour(state: GameState, monsterId: String): Double {
        val p = CombatStatsMath.effectiveStats(state)
        val m = Monsters.get(monsterId).stats
        val hitP = CombatStatsMath.hitChance(p.accuracy, m.evasion)
        val dmgPerHit = CombatStatsMath.expectedHitDamage(
            p.attack, p.critChance, p.critMultiplier, m.defense
        )
        val effectiveKillsPerHit = max(0.0, dmgPerHit.toDouble() / m.maxHp)
        return effectiveKillsPerHit * (hitP / 100.0) * p.attackSpeed * 3600.0
    }

    /** Expected max-DPS damage taken per second while fighting [monsterId]. */
    fun damageTakenPerSecond(state: GameState, monsterId: String): Double {
        val p = CombatStatsMath.effectiveStats(state)
        val m = Monsters.get(monsterId).stats
        val hitM = CombatStatsMath.hitChance(m.accuracy, p.evasion)
        val dmgPerHit = CombatStatsMath.expectedHitDamage(
            m.attack, m.critChance, m.critMultiplier, p.defense
        ) * (1.0 - p.resistance / 100.0)
        return dmgPerHit * (hitM / 100.0) * m.attackSpeed
    }

    /** Seconds of combat sustainable before health forces a retreat. */
    fun combatSecondsUntilRetreat(state: GameState, monsterId: String): Double {
        val p = CombatStatsMath.effectiveStats(state)
        val damage = damageTakenPerSecond(state, monsterId)
        val regen = CombatStatsMath.regenPerSecond(p.maxHp)
        val netLoss = damage - regen
        if (netLoss <= 0.0) return Double.POSITIVE_INFINITY
        val hpFloor = p.maxHp * SimConfig.COMBAT_RETREAT_HP_FRACTION
        val usable = state.character.health - hpFloor
        if (usable <= 0.0) return 0.0
        return usable / netLoss
    }
}
