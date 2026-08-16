package com.projecteternal.content

import com.projecteternal.model.RetainerSpecialization

/**
 * Retainer traits. Purely static data. Each retainer accumulates `traitIds`
 * deterministically as they gain levels (see RetainerEngine.milestoneTraits).
 */
data class RetainerTraitDef(
    val id: String,
    val name: String,
    val description: String,
    /** Multiplier applied to gathering rate. */
    val gatheringSpeedMultiplier: Double = 1.0,
    /** Multiplier applied to rarity of yields. */
    val luckMultiplier: Double = 1.0,
    val icon: String = "✦",
)

/** Static retainer trait catalog. */
object RetainerTraits {

    private val all: Map<String, RetainerTraitDef> = listOf(
        RetainerTraitDef(
            id = "eager_strike", name = "Eager",
            description = "+15% gathering speed. Idle hands find no ore.",
            gatheringSpeedMultiplier = 1.15,
            icon = "⚡",
        ),
        RetainerTraitDef(
            id = "lucky_hands", name = "Lucky Hands",
            description = "+20% find chance. Things fall into their pockets.",
            luckMultiplier = 1.2,
            icon = "🍀",
        ),
        RetainerTraitDef(
            id = "deep_focus", name = "Deep Focus",
            description = "+10% gathering speed. They forget the sun.",
            gatheringSpeedMultiplier = 1.1,
            icon = "🧠",
        ),
        RetainerTraitDef(
            id = "sharp_senses", name = "Sharp Senses",
            description = "+10% find chance. They hear the mineral before they see it.",
            luckMultiplier = 1.1,
            icon = "👂",
        ),
        RetainerTraitDef(
            id = "unyielding", name = "Unyielding",
            description = "+25% gathering speed but −10% find chance. Breaks rocks rather than gathers them.",
            gatheringSpeedMultiplier = 1.25,
            luckMultiplier = 0.9,
            icon = "🪨",
        ),
        RetainerTraitDef(
            id = "vein_sense", name = "Vein Sense",
            description = "+20% find chance and +5% gathering speed. They smell the rare veins beneath the rock.",
            gatheringSpeedMultiplier = 1.05,
            luckMultiplier = 1.2,
            icon = "💠",
        ),
    ).associateBy { it.id }

    fun get(id: String): RetainerTraitDef = all[id]
        ?: error("Unknown retainer trait def '$id' (content gap)")

    fun allTraits(): List<RetainerTraitDef> = all.values.sortedBy { it.id }

    /** Whether a specialization may ever receive [traitId]. All traits are generic for now. */
    fun allowedFor(traitId: String, spec: RetainerSpecialization): Boolean = true

    /** Combined speed/luck multipliers from a retainer's trait set. */
    fun speedMultiplier(traitIds: List<String>): Double =
        traitIds.fold(1.0) { acc, id -> acc * get(id).gatheringSpeedMultiplier }

    fun luckMultiplier(traitIds: List<String>): Double =
        traitIds.fold(1.0) { acc, id -> acc * get(id).luckMultiplier }
}