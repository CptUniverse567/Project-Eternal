package com.projecteternal.sim

/** Tuning constants for the simulation loop. All values are data, not magic. */
object SimConfig {
    /** Longest absence we will simulate. Longer absences are clamped and logged. */
    const val MAX_OFFLINE_SECONDS: Long = 12 * 3600L

    /** Absences shorter than this resolve silently (no report, no clamp flag). */
    const val MIN_ELAPSED_FOR_REPORT_SECONDS: Long = 15

    /** Below this, offline resolution steps per-second for accuracy. */
    const val STEPWISE_THRESHOLD_SECONDS: Long = 90

    /** Bounded number of narrative/rare events rolled regardless of absence length. */
    const val MAX_NOTABLE_EVENTS: Int = 3

    /** Passive health regeneration, fraction of max HP per second. */
    const val REGEN_FRACTION_PER_SECOND: Double = 0.02

    /** HP fraction at which combat auto-retreats. */
    const val COMBAT_RETREAT_HP_FRACTION: Double = 0.20

    /** Durability consumed per gathering action. */
    const val DURABILITY_PER_GATHER_ACTION: Double = 0.4

    /** Durability consumed per kill on the weapon. */
    const val DURABILITY_PER_KILL_WEAPON: Double = 0.5

    /** Durability consumed per kill on armor. */
    const val DURABILITY_PER_KILL_ARMOR: Double = 0.2

    /** Stamina consumed per retainer action. */
    const val STAMINA_PER_RETAINER_ACTION: Double = 1.0

    /** Retainer stamina regeneration per second. */
    const val STAMINA_REGEN_PER_SECOND: Double = 0.02

    /** Gatherer tool bonus multiplier (Journeyman's Pickaxe). */
    const val PICKAXE_SPEED_MULTIPLIER: Double = 1.25
}
