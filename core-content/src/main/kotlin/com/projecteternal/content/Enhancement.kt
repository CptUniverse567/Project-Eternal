package com.projecteternal.content

import com.projecteternal.model.EnhancementBand
import com.projecteternal.model.ItemStack

/**
 * Enhancement tier-band tables. Bands are DATA keyed by current level;
 * the architecture supports appending further bands without schema changes.
 *
 * BASE (0..15) and ADVANCED (16..30) are fully balanced and reachable.
 * ADVANCED requires Voidforged Crystals (Emberreach) and has a harsher
 * failure model. TRANSCENDENT (31..45) is gated behind the Stormbound
 * Catalyst chain and, on an unprotected failure, SHATTERS back to +31 —
 * a strategic decision that risks existing value. ASCENDANT (46..60) is
 * architecture-only: it carries an unlock token nothing in the game grants,
 * so it cannot be enhanced into until later content provides it.
 */
object EnhancementTables {

    val BASE = EnhancementTable(
        band = EnhancementBand.BASE,
        minLevel = 0,
        maxLevel = 15,
        baseSuccessPercent = mapOf(
            0 to 100, 1 to 95, 2 to 90, 3 to 85, 4 to 80,
            5 to 70, 6 to 60, 7 to 50, 8 to 42, 9 to 35,
            10 to 30, 11 to 24, 12 to 19, 13 to 15, 14 to 12, 15 to 10,
        ),
        shardsPerAttempt = (0..15).associateWith { (it + 1).toLong() },
        failure = FailureConsequence.DOWNGRADE_ONE,
        downgradeThreshold = 11,
        durabilityLossOnFail = 10,
        resolvePerFail = 1,
        maxResolveBonusPercent = 25,
        protectionItem = "oil_preservation",
        fullNegationItem = "ward_of_stability",
        note = "Fully balanced. +11..+15 downgrades on failure.",
    )

    private val advancedSuccess = buildMap {
        for (l in 16..30) {
            // smooth fall from 40% down to 5%
            val p = 40 - ((l - 16) * 35) / 14
            put(l, p)
        }
    }

    val ADVANCED = EnhancementTable(
        band = EnhancementBand.ADVANCED,
        minLevel = 16,
        maxLevel = 30,
        baseSuccessPercent = advancedSuccess,
        shardsPerAttempt = (16..30).associateWith { ((it - 15) * 3L + 8L) },
        materialPerAttempt = ItemStack("crystal_advanced", 1),
        failure = FailureConsequence.DOWNGRADE_TWO,
        downgradeThreshold = 16,
        durabilityLossOnFail = 20,
        resolvePerFail = 2,
        maxResolveBonusPercent = 30,
        protectionItem = "oil_preservation",
        fullNegationItem = "ward_of_stability",
        note = "Requires Voidforged Crystals; failure drops the item 2 levels.",
    )

    private val transcendentSuccess = buildMap {
        for (l in 31..45) put(l, 30 - ((l - 31) * 29) / 14)
    }

    val TRANSCENDENT = EnhancementTable(
        band = EnhancementBand.TRANSCENDENT,
        minLevel = 31,
        maxLevel = 45,
        baseSuccessPercent = transcendentSuccess,
        shardsPerAttempt = (31..45).associateWith { ((it - 15) * 5L + 20L) },
        materialPerAttempt = ItemStack("catalyst_storm", 1),
        failure = FailureConsequence.SHATTER_TO_BAND_FLOOR,
        downgradeThreshold = 31,
        durabilityLossOnFail = 30,
        resolvePerFail = 3,
        maxResolveBonusPercent = 35,
        protectionItem = "oil_preservation",
        fullNegationItem = "ward_of_stability",
        unlockToken = "recipe:refine_catalyst",
        statMultiplierPerLevel = 0.20,
        note = "Gated behind the Stormbound Catalyst. An unprotected failure shatters the item back to +31.",
    )

    val ASCENDANT = EnhancementTable(
        band = EnhancementBand.ASCENDANT,
        minLevel = 46,
        maxLevel = 60,
        baseSuccessPercent = (46..60).associateWith { 20 - (it - 46) },
        shardsPerAttempt = (46..60).associateWith { ((it - 15) * 8L + 40L) },
        failure = FailureConsequence.DOWNGRADE_TWO,
        downgradeThreshold = 46,
        durabilityLossOnFail = 40,
        resolvePerFail = 5,
        maxResolveBonusPercent = 40,
        unlockToken = "tier:ascendant",
        statMultiplierPerLevel = 0.25,
        note = "Ascendancy layer for later content — unreachable until a tier token exists.",
    )

    private val all = listOf(BASE, ADVANCED, TRANSCENDENT, ASCENDANT)

    /** The table governing the attempt from [level] to level+1, or null if none. */
    fun tableFor(level: Int): EnhancementTable? =
        if (level >= maxEnhancementLevel) null
        else all.firstOrNull { level in it.minLevel..it.maxLevel }

    val maxEnhancementLevel: Int get() = ASCENDANT.maxLevel

    /** True if these are level ups of the same band's early numeric scale. */
    fun bandOf(level: Int): EnhancementBand? = tableFor(level)?.band
}