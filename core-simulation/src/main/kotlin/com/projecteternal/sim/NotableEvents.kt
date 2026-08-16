package com.projecteternal.sim

import com.projecteternal.model.GameState

/**
 * Bounded narrative events rolled on offline resume. The number of rolls is
 * capped regardless of absence length, so a 30-day absence cannot produce
 * hundreds of events.
 */
object NotableEvents {

    data class EventResult(val state: GameState, val events: List<String>)

    fun roll(state: GameState, rng: RandomSource): EventResult {
        val rollCount = rng.nextInt(SimConfig.MAX_NOTABLE_EVENTS) + 1
        var s = state
        val events = mutableListOf<String>()
        repeat(rollCount) {
            if (!rng.chance(70.0)) return@repeat
            when (rng.nextInt(3)) {
                0 -> {
                    val marks = rng.longInRange(5, 25)
                    s = s.addMarks(marks)
                    events.add("While you were away a passing merchant paid you $marks Marks for gossip.")
                }
                1 -> {
                    s = s.addItem("shard_resonance", 1)
                    events.add("A Resonance Shard tumbled out of a crack you never noticed before.")
                }
                else -> {
                    val herbs = rng.longInRange(2, 4)
                    s = s.addItem("herb_brightleaf", herbs)
                    events.add("A cluster of Brightleaf had grown right where you camped (+$herbs).")
                }
            }
        }
        return EventResult(s, events)
    }
}
