package com.projecteternal.content

import com.projecteternal.model.RetainerId
import com.projecteternal.model.RetainerSpecialization

/**
 * Recruitable retainers. Hiring is gated by Marks and an unlock token (see
 * GameIntent.RecruitRetainer). A hired recruitable becomes a fresh [Retainer]
 * at level 1 with these stats and an empty trait pool.
 */
data class RetainerRecruitDef(
    val id: RetainerId,
    val name: String,
    val specialization: RetainerSpecialization,
    val costMarks: Long,
    /** Held unlock token required before this retainer can be hired. */
    val unlockToken: String,
    val gatheringSpeed: Double = 1.0,
    val productionSpeed: Double = 1.0,
    val luck: Double = 1.0,
    val icon: String = "🧑‍🌾",
    val description: String = "",
)

/** Static recruitable retainer catalog. */
object RetainerRecruits {

    private val all: Map<RetainerId, RetainerRecruitDef> = listOf(
        RetainerRecruitDef(
            id = "retainer_mara",
            name = "Mara",
            specialization = RetainerSpecialization.FARMER,
            costMarks = 400,
            unlockToken = "screen:workers",
            gatheringSpeed = 1.0,
            luck = 1.1,
            icon = "🌾",
            description = "A steady hand with the scythe. Farm labour for wherever the grain runs.",
        ),
        RetainerRecruitDef(
            id = "retainer_ode",
            name = "Ode",
            specialization = RetainerSpecialization.LUMBERJACK,
            costMarks = 350,
            unlockToken = "screen:workers",
            gatheringSpeed = 1.1,
            luck = 1.0,
            icon = "🪓",
            description = "Splits old timber before breakfast. Forest labour.",
        ),
        RetainerRecruitDef(
            id = "retainer_selkie",
            name = "Selkie",
            specialization = RetainerSpecialization.FISHER,
            costMarks = 450,
            unlockToken = "recipe:craft_fishing_rod",
            gatheringSpeed = 1.0,
            luck = 1.2,
            icon = "🎣",
            description = "Knows the runs by moon phase. Fishery labour.",
        ),
        RetainerRecruitDef(
            id = "retainer_helga",
            name = "Helga",
            specialization = RetainerSpecialization.MINER,
            costMarks = 800,
            unlockToken = "region:emberreach",
            gatheringSpeed = 1.15,
            luck = 1.1,
            icon = "⛏️",
            description = "Worked the Forge-Seam before it burned. Mine labour.",
        ),
        RetainerRecruitDef(
            id = "retainer_sable",
            name = "Sable",
            specialization = RetainerSpecialization.CRAFTER,
            costMarks = 700,
            unlockToken = "region:emberreach",
            productionSpeed = 1.15,
            luck = 1.05,
            icon = "🔨",
            description = "Apprenticed to the Emberhold smiths. Mine and forest labour.",
        ),
        RetainerRecruitDef(
            id = "retainer_voss",
            name = "Voss",
            specialization = RetainerSpecialization.FORAGER,
            costMarks = 1200,
            unlockToken = "region:stormreach",
            gatheringSpeed = 1.2,
            luck = 1.25,
            icon = "🧺",
            description = "Grew up weathering the coast. Forest and special scavenging.",
        ),
        RetainerRecruitDef(
            id = "retainer_pella",
            name = "Pella",
            specialization = RetainerSpecialization.FORGER,
            costMarks = 950,
            unlockToken = "region:cindervale",
            gatheringSpeed = 1.15,
            luck = 1.05,
            icon = "🔨",
            description = "Works the SPECIAL and industrial seams of the ash country. A gathering specialist, not yet a craftsman.",
        ),
        RetainerRecruitDef(
            id = "retainer_runa",
            name = "Runa",
            specialization = RetainerSpecialization.PROSPECTOR,
            costMarks = 1400,
            unlockToken = "region:frostreach",
            gatheringSpeed = 1.1,
            luck = 1.3,
            icon = "💎",
            description = "Chases rare veins from Cindervale to the high fells. Luck, and patience.",
        ),
    ).associateBy { it.id }

    fun get(id: RetainerId): RetainerRecruitDef = all[id]
        ?: error("Unknown retainer recruit def '$id' (content gap)")

    fun allRecruits(): List<RetainerRecruitDef> = all.values.sortedBy { it.costMarks }
}