package com.projecteternal.sim

import com.projecteternal.content.LevelCurves
import com.projecteternal.model.GameState
import com.projecteternal.model.SkillId

/** XP application. Levels are derived from cumulative XP, never stored drift. */
object Progress {

    fun charXp(state: GameState, amount: Long): GameState {
        if (amount <= 0) return state
        val c = state.character
        val newXp = c.xp + amount
        return state.copy(character = c.copy(xp = newXp, level = LevelCurves.characterLevelFromXp(newXp)))
    }

    fun skillXp(state: GameState, skill: SkillId, amount: Long): GameState {
        if (amount <= 0) return state
        val c = state.character
        val newXp = (c.skillXp[skill] ?: 0) + amount
        return state.copy(character = c.copy(
            skillXp = c.skillXp + (skill to newXp),
            skillLevels = c.skillLevels + (skill to LevelCurves.skillLevelFromXp(newXp)),
        ))
    }
}
