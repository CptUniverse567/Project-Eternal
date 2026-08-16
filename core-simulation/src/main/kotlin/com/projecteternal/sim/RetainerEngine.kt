package com.projecteternal.sim

import com.projecteternal.content.LevelCurves
import com.projecteternal.content.Nodes
import com.projecteternal.content.RetainerTraits
import com.projecteternal.model.GameState
import kotlin.math.min

/** Retainer (worker) production engine — shares rate math with the player. */
object RetainerEngine {

    /** Levels at which a retainer gains a new trait from the [RetainerTraits] pool. */
    val TRAIT_MILESTONE_LEVELS = listOf(3, 7, 12, 18, 25)

    data class Delta(
        val output: Map<String, Map<String, Long>> = emptyMap(), // retainerId -> resources
        val xpGained: Map<String, Long> = emptyMap(),            // retainerId -> xp
        val leveledUp: List<String> = emptyList(),
        val newTraits: Map<String, List<String>> = emptyMap(),    // retainerId -> gained trait ids
    )

    /**
     * Deterministic trait for a retainer at a milestone: picks from the pool in
     * a stable, seedless order (id hash) excluding traits already owned, and
     * excluding any that the retainer's specialization forbids.
     */
    fun milestoneTraitFor(retainerId: String, spec: String, owned: List<String>): String {
        val specEnum = com.projecteternal.model.RetainerSpecialization.valueOf(spec)
        val candidates = RetainerTraits.allTraits()
            .filter { !owned.contains(it.id) && RetainerTraits.allowedFor(it.id, specEnum) }
            .sortedBy { it.id }
        if (candidates.isEmpty()) return ""
        val idx = (retainerId.hashCode() + owned.size) and 0x7fffffff
        return candidates[idx % candidates.size].id
    }

    fun advance(state: GameState, elapsedSeconds: Double, rng: RandomSource): Pair<GameState, Delta> {
        var s = state
        val output = mutableMapOf<String, Map<String, Long>>()
        val xp = mutableMapOf<String, Long>()
        val leveled = mutableListOf<String>()
        val newTraits = mutableMapOf<String, List<String>>()

        for (retainer in s.retainers) {
            val nodeId = retainer.assignedNodeId ?: continue
            val node = Nodes.get(nodeId)
            if (s.node(nodeId)?.unlocked != true) continue
            if (node.yields.isEmpty()) continue

            val ratePerHour = Rates.retainerActionsPerHour(retainer, node)
            val raw = elapsedSeconds * ratePerHour / 3600.0
            val effectiveStamina = retainer.stamina + elapsedSeconds * SimConfig.STAMINA_REGEN_PER_SECOND
            val staminaPossible = effectiveStamina / SimConfig.STAMINA_PER_RETAINER_ACTION
            val actions = min(raw, staminaPossible)
            if (actions > 0) {
                val gained = mutableMapOf<String, Long>()
                val luck = RetainerTraits.luckMultiplier(retainer.traitIds) *
                    com.projecteternal.content.Regions.get(node.regionId).yieldMultiplier
                for (y in node.yields) {
                    val count = rng.poisson(actions * y.chancePercent / 100.0 * luck)
                    if (count > 0) {
                        gained[y.defId] = (gained[y.defId] ?: 0) + count
                        s = s.addItem(y.defId, count)
                        s = s.copy(stats = s.stats.addGather(y.defId, count))
                    }
                }
                if (gained.isNotEmpty()) output[retainer.id] = gained
            }

            val xpGain = (actions * node.xpPerAction).toLong()
            xp[retainer.id] = xpGain
            var nextRetainer = { r: com.projecteternal.model.Retainer ->
                val newXp = r.xp + xpGain
                var lvl = r.level
                var remaining = newXp
                while (remaining >= LevelCurves.skillXpToNext(lvl)) {
                    remaining -= LevelCurves.skillXpToNext(lvl)
                    lvl++
                }
                if (lvl > r.level) leveled.add(r.id)
                r.copy(xp = newXp, level = lvl)
            }

            val updated = nextRetainer(retainer)
            s = s.updateRetainer(retainer.id) { updated }

            // grant milestone traits for each milestone crossed this tick
            val milestonesCrossed = TRAIT_MILESTONE_LEVELS.filter { milestone ->
                milestone > retainer.level && milestone <= updated.level
            }
            if (milestonesCrossed.isNotEmpty()) {
                var owned = updated.traitIds
                val granted = mutableListOf<String>()
                for (milestone in milestonesCrossed) {
                    val trait = milestoneTraitFor(retainer.id, retainer.specialization.name, owned)
                    if (trait.isNotEmpty() && !granted.contains(trait)) {
                        granted.add(trait)
                        owned = owned + trait
                    }
                }
                if (granted.isNotEmpty()) {
                    s = s.updateRetainer(retainer.id) { r -> r.copy(traitIds = granted.fold(r.traitIds) { acc, t -> acc + t }.distinct()) }
                    newTraits[retainer.id] = granted
                }
            }

            val staminaUsed = actions * SimConfig.STAMINA_PER_RETAINER_ACTION
            s = s.updateRetainer(retainer.id) { r ->
                r.copy(
                    stamina = (r.stamina - staminaUsed + elapsedSeconds * SimConfig.STAMINA_REGEN_PER_SECOND)
                        .coerceIn(0.0, r.maxStamina.toDouble()).toInt(),
                )
            }
        }

        return s to Delta(output = output, xpGained = xp, leveledUp = leveled, newTraits = newTraits)
    }
}
