package com.projecteternal.sim

import com.projecteternal.content.LevelCurves
import com.projecteternal.content.Quests
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.GameState
import com.projecteternal.model.Objective
import com.projecteternal.model.ObjectiveType
import com.projecteternal.model.QuestStatus

/**
 * Generic quest engine. Objective values are computed purely from game state,
 * so content designers add quests as data without writing code. Completion
 * applies rewards (marks/items/xp/unlocks), which may auto-accept the next
 * quest in the chain — the engine loops to a fixpoint.
 */
object QuestEngine {

    data class Result(val state: GameState, val completed: List<String>)

    fun process(state: GameState, now: Long): Result {
        var s = state
        val completed = mutableListOf<String>()
        var changed = true
        while (changed) {
            changed = false
            val active = s.quests.filter { it.status == QuestStatus.ACTIVE }.map { it.questId }.toSet()
            val done = s.quests.filter { it.status == QuestStatus.COMPLETED }.map { it.questId }.toSet()

            val toAccept = Quests.available(done, s.unlocks)
                .filter { it.autoAccept && !active.contains(it.id) && !done.contains(it.id) }
            if (toAccept.isNotEmpty()) {
                s = s.acceptQuests(toAccept.map { it.id }, now)
                changed = true
            }

            for (quest in s.quests.filter { it.status == QuestStatus.ACTIVE }) {
                val def = Quests.get(quest.questId)
                val progress = def.objectives.associate { obj ->
                    val live = currentValue(s, obj)
                    val prev = quest.objectiveProgress[obj.id] ?: 0
                    // Equip objectives are one-time actions: once achieved they stay
                    // achieved even if the player later unequips or the item breaks,
                    // so an overnight break cannot stall the whole quest chain.
                    val value = if (obj.type == ObjectiveType.EQUIP_SLOT) maxOf(prev, live) else live
                    obj.id to value
                }
                s = s.updateQuest(quest.questId) { it.copy(objectiveProgress = progress) }
                val allDone = def.objectives.all { obj ->
                    if (obj.type == ObjectiveType.EQUIP_SLOT) {
                        (progress[obj.id] ?: 0) >= obj.targetCount
                    } else {
                        currentValue(s, obj) >= obj.targetCount
                    }
                }
                if (allDone) {
                    s = applyRewards(s.completeQuest(quest.questId, now), def)
                    completed.add(quest.questId)
                    changed = true
                }
            }
        }
        return Result(s, completed)
    }

    /** Current objective value derived from live state. */
    fun currentValue(state: GameState, obj: Objective): Long = when (obj.type) {
        ObjectiveType.HAVE_ITEMS -> state.inventoryCount(obj.targetId)
        ObjectiveType.GATHER -> state.stats.gatherCounts[obj.targetId] ?: 0
        ObjectiveType.CRAFT -> state.stats.craftCounts[obj.targetId] ?: 0
        ObjectiveType.KILL -> state.stats.killCounts[obj.targetId] ?: 0
        ObjectiveType.DISCOVER_NODE -> if (state.node(obj.targetId)?.unlocked == true) 1 else 0
        ObjectiveType.REACH_CHAR_LEVEL -> state.character.level.toLong()
        ObjectiveType.REACH_SKILL_LEVEL -> state.character.skillLevel(obj.targetId).toLong()
        ObjectiveType.HAVE_MARKS -> state.character.marks
        ObjectiveType.ENHANCE_ANY_TO -> state.stats.maxEnhanceAchieved.toLong()
        ObjectiveType.EQUIP_SLOT -> {
            val slot = EquipSlot.entries.firstOrNull { it.name == obj.targetId }
            if (slot != null && state.character.equipped.containsKey(slot)) 1 else 0
        }
        ObjectiveType.VISIT_REGION -> if (state.stats.visitedRegions.contains(obj.targetId)) 1 else 0
    }

    private fun applyRewards(state: GameState, def: com.projecteternal.content.QuestDefinition): GameState {
        var s = state
        val r = def.reward
        if (r.marks > 0) s = s.addMarks(r.marks)
        for (item in r.items) s = s.addItem(item.defId, item.count)
        if (r.charXp > 0) s = Progress.charXp(s, r.charXp)
        for ((skill, xp) in r.skillXp) s = Progress.skillXp(s, skill, xp)
        if (r.unlocks.isNotEmpty()) s = s.unlock(*r.unlocks.toTypedArray())
        return s
    }
}
