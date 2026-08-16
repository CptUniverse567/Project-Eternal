package com.projecteternal.sim

import com.projecteternal.content.EnhancementTables
import com.projecteternal.content.Items
import com.projecteternal.content.Nodes
import com.projecteternal.content.Recipes
import com.projecteternal.model.ActivityType
import com.projecteternal.model.GameState
import kotlin.math.floor
import kotlin.math.min

/**
 * Foreground + offline activity advancement. A single code path drives
 * gathering, processing, crafting and combat for BOTH live ticks and the
 * offline simulator (see §7.1).
 */
object ActivityEngine {

    data class Delta(
        val resourcesGained: Map<String, Long> = emptyMap(),
        val resourcesConsumed: Map<String, Long> = emptyMap(),
        val charXp: Long = 0,
        val skillXp: Map<String, Long> = emptyMap(),
        val kills: Map<String, Long> = emptyMap(),
        val brokenUids: List<String> = emptyList(),
        val activityEnded: Boolean = false,
    )

    fun advance(state: GameState, elapsedSeconds: Double, rng: RandomSource): Pair<GameState, Delta> {
        val activity = state.character.currentActivity ?: return idleRegen(state, elapsedSeconds)
        return when (activity.type) {
            ActivityType.GATHERING -> advanceGathering(state, activity.targetId, elapsedSeconds, rng)
            ActivityType.PROCESSING, ActivityType.CRAFTING ->
                advanceRecipe(state, activity.targetId, activity.type, elapsedSeconds, rng)
            ActivityType.COMBAT -> advanceCombat(state, activity.targetId, elapsedSeconds, rng)
        }
    }

    private fun idleRegen(state: GameState, elapsedSeconds: Double): Pair<GameState, Delta> {
        val maxHp = CombatStatsMath.effectiveStats(state).maxHp
        val healed = CombatStatsMath.regenPerSecond(maxHp) * elapsedSeconds
        if (healed < 1.0) return state to Delta()
        val hp = min(maxHp, state.character.health + healed.toInt())
        return state.copy(character = state.character.copy(health = hp)) to Delta()
    }

    private fun advanceGathering(
        state: GameState,
        nodeId: String,
        elapsedSeconds: Double,
        rng: RandomSource,
    ): Pair<GameState, Delta> {
        val node = Nodes.get(nodeId)
        val activity = state.character.currentActivity!!
        val ratePerHour = Rates.gatheringActionsPerHour(state, node)
        val total = activity.carry + elapsedSeconds * ratePerHour / 3600.0
        val actions = floor(total).toLong()
        val carry = total - actions
        val yields = node.yields

        var s = state.copy(character = state.character.copy(currentActivity = activity.copy(carry = carry)))
        val gained = mutableMapOf<String, Long>()
        var skillXp = 0L
        val skill = node.skills.firstOrNull()

        if (actions > 0) {
            val mult = Rates.gatheringYieldMultiplier(state, node)
            for (y in yields) {
                val count = rng.poisson(actions.toDouble() * y.chancePercent / 100.0 * mult)
                if (count > 0) {
                    gained[y.defId] = (gained[y.defId] ?: 0) + count
                    s = s.addItem(y.defId, count)
                    s = s.copy(stats = s.stats.addGather(y.defId, count))
                }
            }
            skillXp = actions * node.xpPerAction
            if (skill != null) s = Progress.skillXp(s, skill, skillXp)
            s = Progress.charXp(s, actions * node.xpPerAction / 4)
            s = EquipmentHelper.damageTool(s, actions * SimConfig.DURABILITY_PER_GATHER_ACTION)
        }

        return s to Delta(
            resourcesGained = gained,
            charXp = actions * node.xpPerAction / 4,
            skillXp = if (skill != null) mapOf(skill to skillXp) else emptyMap(),
            brokenUids = EquipmentHelper.brokenUids(state.equipmentItems.associateBy { it.uid }, s),
        )
    }

    private fun advanceRecipe(
        state: GameState,
        recipeId: String,
        type: ActivityType,
        elapsedSeconds: Double,
        rng: RandomSource,
    ): Pair<GameState, Delta> {
        val recipe = Recipes.get(recipeId)
        val unlockOk = recipe.unlockToken.isEmpty() || state.hasUnlock(recipe.unlockToken)
        val skillOk = state.character.skillLevel(recipe.skillId) >= recipe.skillLevelRequired

        if (!unlockOk || !skillOk) {
            val cleared = state.withActivity(null)
            return cleared to Delta(activityEnded = true)
        }

        val activity = state.character.currentActivity!!
        val ratePerHour = Rates.recipeActionsPerHour(state, recipe)
        val total = activity.carry + elapsedSeconds * ratePerHour / 3600.0

        // cap by available inputs
        var maxByInputs = Double.POSITIVE_INFINITY
        for (input in recipe.inputs) {
            if (input.count <= 0) continue
            maxByInputs = min(maxByInputs, state.inventoryCount(input.defId).toDouble() / input.count)
        }
        val actions = floor(min(total, maxByInputs)).toLong()
        // End the job only when inputs can't fund another full action.
        // `total > actions` is almost always true on a foreground tick (fractional
        // carry accumulates toward the next action), so it must not gate the end.
        val carry = total - actions

        var s = state.copy(character = state.character.copy(currentActivity = activity.copy(carry = carry)))
        val consumed = mutableMapOf<String, Long>()
        val gained = mutableMapOf<String, Long>()

        if (actions > 0) {
            for (input in recipe.inputs) {
                val amount = actions * input.count
                consumed[input.defId] = (consumed[input.defId] ?: 0) + amount
                s = s.removeItem(input.defId, amount)
            }
            for (output in recipe.outputs) {
                val def = Items.get(output.defId)
                if (def.slot != null) {
                    val total = actions * output.count
                    repeat(total.toInt()) {
                        val uid = "craft_${output.defId}_${rng.nextLong().toString(16)}"
                        s = s.addEquipmentItem(
                            com.projecteternal.model.ItemInstance(
                                uid = uid,
                                defId = output.defId,
                                durability = def.maxDurability,
                                maxDurability = def.maxDurability,
                            )
                        )
                    }
                    gained[output.defId] = (gained[output.defId] ?: 0) + total
                    s = s.copy(stats = s.stats.addCraft(output.defId, total))
                } else {
                    val amount = actions * output.count
                    gained[output.defId] = (gained[output.defId] ?: 0) + amount
                    s = s.addItem(output.defId, amount)
                    s = s.copy(stats = s.stats.addCraft(output.defId, amount))
                }
            }
            s = Progress.skillXp(s, recipe.skillId, actions * recipe.xpPerCraft)
            s = Progress.charXp(s, actions * recipe.xpPerCraft / 2)
        }

        val ended = recipe.inputs.isNotEmpty() && recipe.inputs.any { s.inventoryCount(it.defId) < it.count }
        val final = if (ended) s.withActivity(null) else s

        return final to Delta(
            resourcesGained = gained,
            resourcesConsumed = consumed,
            charXp = actions * recipe.xpPerCraft / 2,
            skillXp = mapOf(recipe.skillId to actions * recipe.xpPerCraft),
            activityEnded = ended,
        )
    }

    private fun advanceCombat(
        state: GameState,
        monsterId: String,
        elapsedSeconds: Double,
        rng: RandomSource,
    ): Pair<GameState, Delta> {
        val activity = state.character.currentActivity!!
        val beforeEquipment = state.equipmentItems.associateBy { it.uid }
        val result = CombatResolver.resolve(state, monsterId, elapsedSeconds, activity.carry)

        val loot = LootResolver.roll(result.kills, com.projecteternal.content.Monsters.get(monsterId).lootTable, rng)
        var s = state.copy(character = state.character.copy(
            currentActivity = if (result.retreated) null else activity.copy(carry = result.carry),
            health = result.healthFinal,
        ))
        for ((item, count) in loot.items) s = s.addItem(item, count)
        s = Progress.charXp(s, result.xpGained)
        for (i in 0 until result.kills) s = s.copy(stats = s.stats.addKill(monsterId))
        s = EquipmentHelper.damageWeaponForKills(s, result.kills)
        s = EquipmentHelper.damageArmorForKills(s, result.kills)

        return s to Delta(
            resourcesGained = loot.items,
            charXp = result.xpGained,
            kills = mapOf(monsterId to result.kills),
            brokenUids = EquipmentHelper.brokenUids(beforeEquipment, s),
            activityEnded = result.retreated,
        )
    }
}
