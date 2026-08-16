package com.projecteternal.content

import com.projecteternal.model.CombatStats
import com.projecteternal.model.EnhancementBand
import com.projecteternal.model.EquipSlot
import com.projecteternal.model.ItemKind
import com.projecteternal.model.ItemStack
import com.projecteternal.model.ItemId
import com.projecteternal.model.MonsterId
import com.projecteternal.model.NodeId
import com.projecteternal.model.NodeType
import com.projecteternal.model.Objective
import com.projecteternal.model.QuestId
import com.projecteternal.model.RecipeId
import com.projecteternal.model.RegionId
import com.projecteternal.model.SkillId

/**
 * Content definition classes. These are STATIC data, never per-save state.
 * Everything is plain Kotlin (no @Serializable) — content ships compiled in.
 */

enum class SkillCategory { GATHERING, PROCESSING, CRAFTING }

enum class PerkType { EFFICIENCY_PERCENT, YIELD_PERCENT, LUCK_PERCENT }

data class SkillDefinition(
    val id: SkillId,
    val name: String,
    val category: SkillCategory,
    val description: String,
    val maxLevel: Int = 99,
    val perkPerLevel: PerkType = PerkType.EFFICIENCY_PERCENT,
    val perkValuePerLevel: Double = 0.0, // e.g. 1.0 = +1% per level
)

data class ItemDefinition(
    val id: ItemId,
    val name: String,
    val kind: ItemKind,
    val slot: EquipSlot? = null,
    val tier: Int = 1,
    val baseStats: CombatStats? = null,
    val maxDurability: Int = 0,
    val stackable: Boolean = true,
    val buyPrice: Long = 0,
    val sellPrice: Long = 0,
    val icon: String = "•",
    val description: String = "",
    val enhanceable: Boolean = false,
    /** Restored health when used as a consumable (0 = not a healing consumable). */
    val healAmount: Int = 0,
    /** If set, this TOOL boosts gathering speed for nodes using this skill. */
    val gatheringSkill: SkillId? = null,
    /** Gathering speed multiplier granted by this tool on matching nodes. */
    val gatheringSpeedMultiplier: Double = 1.0,
)

/** One entry in a monster's loot table. Guaranteed entries sum to 100%. */
data class LootEntry(
    val defId: ItemId,
    val chancePercent: Int = 100,
    val minCount: Long = 1,
    val maxCount: Long = 1,
    val guaranteed: Boolean = true,
)

data class MonsterDefinition(
    val id: MonsterId,
    val name: String,
    val tier: Int,
    val regionId: RegionId,
    val stats: CombatStats,
    val xpReward: Long,
    val lootTable: List<LootEntry>,
    val boss: Boolean = false,
    val description: String = "",
)

data class YieldEntry(
    val defId: ItemId,
    val chancePercent: Int = 100,
    val minCount: Long = 1,
    val maxCount: Long = 1,
)

data class NodeDefinition(
    val id: NodeId,
    val regionId: RegionId,
    val type: NodeType,
    val tier: Int,
    val name: String,
    val unlockToken: String = "",
    val skills: List<SkillId>,
    val xpPerAction: Long,
    val actionsPerHour: Double, // base, before character/retainer multipliers
    val yields: List<YieldEntry>,
    val description: String = "",
)

data class RecipeDefinition(
    val id: RecipeId,
    val name: String,
    val skillId: SkillId,
    val skillLevelRequired: Int = 0,
    val timeSeconds: Int,
    val inputs: List<ItemStack>,
    val outputs: List<ItemStack>,
    val xpPerCraft: Long,
    val unlockToken: String = "",
    val description: String = "",
)

/**
 * Regional rare-resource rule: every gathering action in the region has a
 * per-action chance to also yield [defId]. One small, legible player-facing rule.
 */
data class RareYieldConfig(
    val defId: ItemId,
    val chancePercent: Int = 8,
)

data class RegionDefinition(
    val id: RegionId,
    val name: String,
    val tier: Int,
    val description: String,
    val unlockToken: String = "",
    val priceModifier: Double = 1.0,
    /** Multiplier applied to gathering yields earned inside this region, e.g. a regional boon. */
    val yieldMultiplier: Double = 1.0,
    /**
     * Hazard rule: each gathering action in this region chips this much health.
     * Damage is floored so the character can never be driven below a safe HP
     * floor; deterministic and offline-compatible. 0 = no hazard.
     */
    val hazardPerAction: Double = 0.0,
    /** Regional rare-resource rule (e.g. Smoldering Veins, Crystal Snow). Null = none. */
    val rareYield: RareYieldConfig? = null,
)

data class QuestReward(
    val marks: Long = 0,
    val items: List<ItemStack> = emptyList(),
    val charXp: Long = 0,
    val skillXp: Map<SkillId, Long> = emptyMap(),
    val unlocks: List<String> = emptyList(),
)

enum class QuestCategory { MAIN, REGIONAL, COMBAT, LIFESKILL, SIDE, DISCOVERY }

data class QuestDefinition(
    val id: QuestId,
    val name: String,
    val description: String,
    val category: QuestCategory = QuestCategory.SIDE,
    /** quest ids (completed) or unlock tokens that must be satisfied */
    val prerequisites: List<String> = emptyList(),
    val objectives: List<Objective>,
    val reward: QuestReward = QuestReward(),
    /** when true, becomes active automatically once prerequisites are met */
    val autoAccept: Boolean = false,
)

/** Failure consequence of an enhancement attempt, per tier band. */
enum class FailureConsequence {
    DURABILITY_ONLY,
    DOWNGRADE_ONE,
    DOWNGRADE_TWO,
    /** Harsh transcendental failure: the item resets to the band's floor level. */
    SHATTER_TO_BAND_FLOOR,
}

data class EnhancementTable(
    val band: EnhancementBand,
    val minLevel: Int,
    val maxLevel: Int,
    /** key = current level, value = base success percent for attempt to +1. */
    val baseSuccessPercent: Map<Int, Int>,
    /** key = current level, value = resonance shards consumed per attempt. */
    val shardsPerAttempt: Map<Int, Long>,
    val materialPerAttempt: ItemStack? = null,
    /** Second, equivalent material accepted per attempt (player chooses one). */
    val alternateMaterialPerAttempt: ItemStack? = null,
    val failure: FailureConsequence = FailureConsequence.DURABILITY_ONLY,
    /** Levels at or above this threshold suffer [failure]; below it failures only drain durability. */
    val downgradeThreshold: Int = Int.MAX_VALUE,
    val durabilityLossOnFail: Int = 10,
    val resolvePerFail: Long = 1,
    val maxResolveBonusPercent: Int = 25,
    val protectionItem: ItemId? = null,
    /** Item that fully negates a failed attempt: no downgrade AND no durability loss. Consumed on use. */
    val fullNegationItem: ItemId? = null,
    /**
     * Unlock token the player must hold before ANY attempt in this band is
     * allowed (e.g. "recipe:refine_catalyst"). Empty = no gate.
     */
    val unlockToken: String = "",
    /** Stat growth per enhancement level for this band (see CombatStatsMath). */
    val statMultiplierPerLevel: Double = 0.15,
    val note: String = "",
)

/** Guaranteed "first item per roll" entry — see loot resolution. */
data class MarketRegion(
    val regionId: RegionId,
    val buyModifier: Double = 1.0,
    val sellModifier: Double = 1.0,
)
